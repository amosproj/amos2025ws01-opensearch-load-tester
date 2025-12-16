package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class LoadRunner {

    private final String loadGeneratorId;
    private final OpenSearchGenericClient openSearchClient;
    private final MetricsReporterClient metricsReporterClient;
    private final MetricsCollector metricsCollector;

    public LoadRunner(
            @Value("${HOSTNAME}") String loadGeneratorId,
            OpenSearchGenericClient openSearchClient,
            MetricsReporterClient metricsReporterClient,
            MetricsCollector metricsCollector
    ) {
        this.loadGeneratorId = loadGeneratorId;
        this.openSearchClient = openSearchClient;
        this.metricsReporterClient = metricsReporterClient;
        this.metricsCollector = metricsCollector;
    }

    /**
     * Executes queries according to the ScenarioConfig
     *
     * @param scenarioConfig scenario configuration
     */
    public void executeScenario(ScenarioConfig scenarioConfig) {
        log.info("Started '{}' execution (duration: {} sec)",
                scenarioConfig.getName(), scenarioConfig.getDuration().getSeconds());

        QueryExecutionTask query = new QueryExecutionTask(
                loadGeneratorId,
                scenarioConfig.getDocumentType().getIndex(),
                scenarioConfig.getQueryTypes(),
                openSearchClient,
                metricsCollector
        );

        // Track overall test start time
        long testStartTime = System.currentTimeMillis();

        ScheduledExecutorService executorService = Executors
                .newSingleThreadScheduledExecutor();
        ExecutorService workers = Executors.newCachedThreadPool();

        try {
            long durationNs = scenarioConfig.getDuration().toNanos();
            int qpsTotal = scenarioConfig.getQueriesPerSecond();
            int qpsPerLoadGen = qpsTotal / Integer.parseInt(System.getenv("LOAD_GENERATOR_REPLICAS"));
            long durationPerQuery = 1000_000_000L / qpsPerLoadGen;
            AtomicInteger queryCounter = new AtomicInteger();
            log.debug("Schedule delay:  {} ms  ", durationPerQuery);

            // Start scheduled query execution
            ScheduledFuture<?> future = executorService.scheduleAtFixedRate(() -> {
                        try {
                            workers.submit(query);
                            queryCounter.getAndIncrement();
                        } catch (RejectedExecutionException | OutOfMemoryError e) {
                            log.warn("Failed to create a new thread. QPS cannot be reached...", e);
                            log.warn("Please increase REPLICAS amount!");
                        }
                    },
                    durationPerQuery / 2,
                    durationPerQuery,
                    TimeUnit.NANOSECONDS);
            executorService.schedule(() -> future.cancel(false), durationNs, TimeUnit.NANOSECONDS);

            // TODO: Wait for all threads to complete
            log.info("Waiting for all threads to complete");
            boolean completed = true;
            executorService.awaitTermination(durationNs + 2000_000_000L, TimeUnit.NANOSECONDS);
            // TODO: Set a timeout per queryExecution
            // boolean completed = latch.await(10, TimeUnit.MINUTES);

            // Check if QPS fulfilled
            if (queryCounter.get() != qpsPerLoadGen * scenarioConfig.getDuration().toSeconds()) {
                log.warn("Load Generator can't keep up with QPS... please increase REPLICA amount!");
            }

            long testEndTime = System.currentTimeMillis();
            long actualDurationMs = testEndTime - testStartTime;
            double actualDurationSeconds = actualDurationMs / 1000.0;

            if (completed) {
                log.info("Calling MetricsReporterClient");
                metricsReporterClient.reportMetrics(metricsCollector.getMetricsList());
                log.info("Scenario '{}' completed successfully. All threads finished.", scenarioConfig.getName());
                log.info("Test duration - Expected: {} ({}s), Actual: {}s",
                        scenarioConfig.getDuration(),
                        scenarioConfig.getDuration().getSeconds(),
                        String.format("%.2f", actualDurationSeconds));
            } else {
                log.warn("Timeout waiting for execution threads to complete");
                log.warn("Scenario '{}' did not complete within expected duration. Actual runtime: {}s",
                        scenarioConfig.getName(), String.format("%.2f", actualDurationSeconds));
            }

        } catch (Exception e) {
            log.error("Error executing queries:", e);
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
