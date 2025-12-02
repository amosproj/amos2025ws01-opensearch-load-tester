package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoadRunner {

    private final OpenSearchGenericClient openSearchClient;
    private final MetricsReporterClient metricsReporterClient;
    private final MetricsCollector metricsCollector;

    /**
     * Executes queries according to the ScenarioConfig
     *
     * @param scenarioConfig scenario configuration
     */
    public void executeScenario(ScenarioConfig scenarioConfig) {
        log.info("Started '{}' execution (duration: {} sec)",
                scenarioConfig.getName(), scenarioConfig.getDuration().getSeconds());

        String queryTemplatePath = scenarioConfig.getQuery().getType().getTemplatePath();

        QueryExecutionTask query = new QueryExecutionTask(
                scenarioConfig.getDocumentType().getIndex(),
                queryTemplatePath,
                scenarioConfig.getQuery().getParameters(),
                openSearchClient,
                metricsCollector
        );

        // TODO: Deprecated, remove threadPoolSize
        int threadPoolSize = 5;

        // Setup threadPool and countDownLatch
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        CountDownLatch latch = new CountDownLatch(threadPoolSize);

        // TODO: Deprecated, remove clientSize
        int clientSize = 10;

        // Track overall test duration
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
                        int queriesPerSecondPerThread = queriesPerSecondTotal / threadPoolSize;
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
                        log.info("Thread with ID {} finished after {}s and executed {} queries.",
                                Thread.currentThread().threadId(),
                                (System.nanoTime() - start) / 1_000_000_000.0,
                                queryCounter);
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
                metricsReporterClient.reportMetrics(metricsCollector.getReport());
                log.info("Scenario '{}' completed successfully. All {} threads finished.",
                        scenarioConfig.getName(), threadPoolSize);
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
