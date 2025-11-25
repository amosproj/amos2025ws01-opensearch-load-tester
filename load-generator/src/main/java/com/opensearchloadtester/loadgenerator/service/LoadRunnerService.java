package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.model.DocumentType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for executing multiple queries simultaneously using thread pools.
 * Manages thread lifecycle and ensures proper shutdown when all queries are completed.
 */
@Slf4j
@Service
public class LoadRunnerService {

    private final MetricsReporterClient metricsReporterClient;
    private final MetricsCollectorService metricsCollectorService;
    private final QueryRegistry queryRegistry;

    @Value("${opensearch.url}")
    private String openSearchBaseUrl;

    public LoadRunnerService(MetricsReporterClient metricsReporterClient,
                             MetricsCollectorService metricsCollectorService, QueryRegistry queryRegistry) {
        this.metricsReporterClient = metricsReporterClient;
        this.metricsCollectorService = metricsCollectorService;
        this.queryRegistry = queryRegistry;
    }

    /**
     * Executes n query executions simultaneously, each on a single thread
     * and waits until all threads are completed.
     *
     * @param queryExecutions List of query executions to run in parallel
     * @throws InterruptedException if the execution is interrupted while waiting
     */
    public void executeQueries(List<QueryExecution> queryExecutions) throws InterruptedException {
        if (queryExecutions == null || queryExecutions.isEmpty()) {
            log.warn("No query executions provided, nothing to execute");
            return;
        }

        int threadCount = queryExecutions.size();

        // TODO: resources can be a problem here, use a thread pool with a max size
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            // Submit all query executions to the thread pool
            for (QueryExecution queryExecution : queryExecutions) {
                executorService.submit(() -> {
                    try {
                        log.debug("Starting query execution: {}", queryExecution.getId());
                        queryExecution.run();
                        log.debug("Completed query execution: {}", queryExecution.getId());

                    } catch (Exception e) {
                        log.error("Error executing query: {}", queryExecution.getId(), e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            log.info("Waiting for all {} query execution threads to complete", threadCount);
            boolean completed = latch.await(Long.MAX_VALUE, TimeUnit.SECONDS);
            // TODO: Use this for production to set a timeout
            // boolean completed = latch.await(10, TimeUnit.MINUTES);

            if (completed) {
                log.info("Calling MetricsReporterClient");
                metricsReporterClient.reportMetrics(metricsCollectorService.getMetrics());

                log.info("All {} query execution threads completed successfully", threadCount);
            } else {
                log.warn("Timeout waiting for query execution threads to complete");
            }

        } finally {
            // Shutdown executor service
            shutdownExecutorService(executorService);
        }
    }

    /**
     * Executes queries according to the ScenarioConfig
     *
     * @param scenarioConfig scenario configuration
     */
    public void execute(ScenarioConfig scenarioConfig) {
        log.info("Started execution of scenario {} ...", scenarioConfig.getName());

        // Parameter check
        if (scenarioConfig == null) {
            log.error("executeQueries: No configuration provided");
            return;
        }

        // Load query template
        final String templateFile;
        try {
            templateFile = scenarioConfig.getQuery().getType().getTemplatePath();
        } catch (IllegalArgumentException ex) {
            log.error("executeQueries: Unknown queryPath in config: {}", scenarioConfig.getQuery().getType().getTemplatePath());
            return;
        }

        // Create QueryExecution
        OpenSearchQueryExecution query = new OpenSearchQueryExecution(
                scenarioConfig.getName(),
                scenarioConfig.getDocumentType().getIndex(),
                templateFile,
                scenarioConfig.getQuery().getParameters(),
                openSearchBaseUrl,
                metricsCollectorService
        );

        int MAX_THREADS = 50;

        // Number of parallel threads
        int threadPoolSize = scenarioConfig.getConcurrency().getThreadPoolSize();
        if (threadPoolSize > MAX_THREADS) {
            threadPoolSize = MAX_THREADS;
            log.warn("executeQueries: Too many concurrency threads specified." +
                    "Maximal threadPoolSize is {}", MAX_THREADS);
        }

        // Setup threadPool and countDownLatch
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(threadPoolSize);

        int clientSize = scenarioConfig.getConcurrency().getClientSize();

        try {
            // Submit all query executions to the thread pool
            for (int i = 0; i < threadPoolSize; i++) {
                executorService.submit(() -> {
                    int queryCounter = 0;
                    long start = System.nanoTime();
                    try {

                        log.debug("Thread with ID {}: Starting query executions ", Thread.currentThread().threadId());

                        long durationNs = scenarioConfig.getDuration().toNanos();
                        int queriesPerSecondTotal = scenarioConfig.getQueriesPerSecond();
                        int queriesPerSecondPerThread = queriesPerSecondTotal /
                                scenarioConfig.getConcurrency().getThreadPoolSize();
                        int batchesPerSecond = Math.max(1, queriesPerSecondPerThread / clientSize);
                        long sleepBetweenBatchesMs = 1000L / batchesPerSecond;

                        while (System.nanoTime() - start < durationNs) {
                            // execute batch
                            for (int j = 0; j < clientSize; j++) {
                                query.run();
                                queryCounter++;
                            }
                            // rate limit
                            Thread.sleep(sleepBetweenBatchesMs);
                        }

                        log.debug("Thread with ID {}: Finished query executions ", Thread.currentThread().threadId());

                    } catch (Exception e) {
                        log.error("Error executing queries in thread {}", Thread.currentThread().threadId(), e);
                    } finally {
                        log.info("Thread with ID {} finished after {}s and executed {} queries.", Thread.currentThread().threadId(), (System.nanoTime() - start) / 1_000_000_000.0, queryCounter);
                        latch.countDown();
                    }
                });
            }

            // Wait for all threads to complete
            log.info("Waiting for all {} threads to complete", threadPoolSize);
            boolean completed = latch.await(Long.MAX_VALUE, TimeUnit.SECONDS);
            // TODO: Set a timeout per queryExecution
            // boolean completed = latch.await(10, TimeUnit.MINUTES);

            if (completed) {
                log.info("Calling MetricsReporterClient");
                metricsReporterClient.reportMetrics(metricsCollectorService.getMetrics());
                log.info("All {} threads completed successfully", threadPoolSize);
            } else {
                log.warn("Timeout waiting for execution threads to complete");
            }

        } catch (InterruptedException e) {
            log.error("Error when awaiting all threads", e);
        } finally {
            // Shutdown executor service
            shutdownExecutorService(executorService);
        }


    }

    /**
     * Executes n query executions simultaneously using a factory to create them.
     *
     * @param threadCount           Number of query execution threads to spawn
     * @param queryExecutionFactory Factory to create query execution instances
     * @throws InterruptedException if the execution is interrupted while waiting
     */
    public void executeQueries(int threadCount, QueryExecutionFactory queryExecutionFactory)
            throws InterruptedException {
        if (threadCount <= 0) {
            log.warn("Invalid thread count: {}, nothing to execute", threadCount);
            return;
        }

        List<QueryExecution> queryExecutions = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            queryExecutions.add(queryExecutionFactory.create(i));
        }

        executeQueries(queryExecutions);
    }

    /**
     * Gracefully shuts down the executor service.
     *
     * @param executorService The executor service to shut down
     */
    private void shutdownExecutorService(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Executor service did not terminate gracefully, forcing shutdown");
                executorService.shutdownNow();
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    log.error("Executor service did not terminate");
                }
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while shutting down executor service", e);
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
