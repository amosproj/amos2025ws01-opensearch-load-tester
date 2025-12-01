package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.springframework.stereotype.Service;

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
@RequiredArgsConstructor
public class LoadRunner {

    private final OpenSearchGenericClient openSearchClient;
    private final MetricsReporterClient metricsReporterClient;
    private final MetricsCollectorService metricsCollectorService;

    /**
     * Executes n query executions simultaneously, each on a single thread
     * and waits until all threads are completed.
     *
     * @param queryExecutionTasks List of query execution tasks to run in parallel
     * @throws InterruptedException if the execution is interrupted while waiting
     */
    public void executeQueries(List<QueryExecutionTask> queryExecutionTasks) throws InterruptedException {
        if (queryExecutionTasks == null || queryExecutionTasks.isEmpty()) {
            log.warn("No query executions provided, nothing to execute");
            return;
        }

        int threadCount = queryExecutionTasks.size();

        // TODO: resources can be a problem here, use a thread pool with a max size
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        try {
            // Submit all query executions to the thread pool
            for (QueryExecutionTask queryExecutionTask : queryExecutionTasks) {
                executorService.submit(() -> {
                    try {
                        log.debug("Starting query execution: {}", queryExecutionTask.getId());
                        queryExecutionTask.run();
                        log.debug("Completed query execution: {}", queryExecutionTask.getId());

                    } catch (Exception e) {
                        log.error("Error executing query: {}", queryExecutionTask.getId(), e);
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
    public void executeScenario(ScenarioConfig scenarioConfig) {
        log.info("Started execution of scenario: {}", scenarioConfig.getName());
        log.info("Scenario duration: {} ({} seconds)",
                scenarioConfig.getDuration(),
                scenarioConfig.getDuration().getSeconds());


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

        // Create QueryExecutionTask
        QueryExecutionTask query = new QueryExecutionTask(
                scenarioConfig.getName(),
                scenarioConfig.getDocumentType().getIndex(),
                templateFile,
                scenarioConfig.getQuery().getParameters(),
                openSearchClient,
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

        // Track overall test start time
        long testStartTime = System.currentTimeMillis();

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

            long testEndTime = System.currentTimeMillis();
            long actualDurationMs = testEndTime - testStartTime;
            double actualDurationSeconds = actualDurationMs / 1000.0;

            if (completed) {
                log.info("Calling MetricsReporterClient");
                metricsReporterClient.reportMetrics(metricsCollectorService.getMetrics());
                log.info("Scenario '{}' completed successfully. All {} threads finished.", scenarioConfig.getName(), threadPoolSize);
                log.info("Test duration - Expected: {} ({}s), Actual: {}s",
                        scenarioConfig.getDuration(),
                        scenarioConfig.getDuration().getSeconds(),
                        String.format("%.2f", actualDurationSeconds));
            } else {
                log.warn("Timeout waiting for execution threads to complete");
                log.warn("Scenario '{}' did not complete within expected duration. Actual runtime: {}s",
                        scenarioConfig.getName(), String.format("%.2f", actualDurationSeconds));
            }

        } catch (InterruptedException e) {
            log.error("Error when awaiting all threads", e);
        } finally {
            // Shutdown executor service
            shutdownExecutorService(executorService);
        }


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
