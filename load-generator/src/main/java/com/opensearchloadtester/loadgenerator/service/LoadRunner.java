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
    private final int numberLoadGenerators;
    private final OpenSearchGenericClient openSearchClient;
    private final MetricsReporterClient metricsReporterClient;
    private final MetricsCollector metricsCollector;

    public LoadRunner(
            @Value("${HOSTNAME}") String loadGeneratorId,
            @Value("${load.generator.replicas}") int numberLoadGenerators,
            OpenSearchGenericClient openSearchClient,
            MetricsReporterClient metricsReporterClient,
            MetricsCollector metricsCollector
    ) {
        this.loadGeneratorId = loadGeneratorId;
        this.numberLoadGenerators = numberLoadGenerators;
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
        log.info("Executing '{}' (expected duration: {} sec)",
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

        ScheduledExecutorService scheduler = Executors
                .newSingleThreadScheduledExecutor();
        ExecutorService workers = Executors.newCachedThreadPool();

        ScheduledFuture<?> future = null;

        try {
            long durationNs = scenarioConfig.getDuration().toNanos();
            int qpsTotal = scenarioConfig.getQueriesPerSecond();
            int qpsPerLoadGen = qpsTotal / numberLoadGenerators;
            long durationPerQuery = 1_000_000_000L / qpsPerLoadGen;
            AtomicInteger queryCounter = new AtomicInteger();
            log.debug("Schedule delay:  {} ns  ", durationPerQuery);

            // Start scheduled query execution
            future = scheduler.scheduleAtFixedRate(() -> {
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
            // After the scenario duration, stop the periodic scheduling (already submitted tasks may still run).
            ScheduledFuture<?> finalFuture = future;
            scheduler.schedule(() -> {
                try {
                    finalFuture.cancel(false);
                } finally {
                    scheduler.shutdown();
                }
            }, durationNs, TimeUnit.NANOSECONDS);

            // shutdown scheduler and wait (bounded)
            boolean schedulerStopped = awaitExecutorServiceTermination(scheduler, "scheduler", durationNs + TimeUnit.SECONDS.toNanos(10), TimeUnit.NANOSECONDS);
            if (!schedulerStopped) {
                log.warn("Scheduler did not stop in time; forcing shutdownNow()");
                scheduler.shutdownNow();
                schedulerStopped = awaitExecutorServiceTermination(scheduler, "scheduler", 10, TimeUnit.SECONDS);
            }

            // Now stop workers and wait for in-flight tasks (bounded)
            workers.shutdown();
            boolean workersStopped = awaitExecutorServiceTermination(workers, "workers", 60, TimeUnit.SECONDS);
            if (!workersStopped) {
                log.warn("Workers did not terminate in time; forcing shutdownNow()");
                workers.shutdownNow();
                workersStopped = awaitExecutorServiceTermination(workers, "workers", 10, TimeUnit.SECONDS);
            }

            // Check if QPS fulfilled
            if (queryCounter.get() < qpsPerLoadGen * scenarioConfig.getDuration().toSeconds()) {
                log.warn("Load Generator can't keep up with QPS... please increase REPLICA amount!");
            }

            long testEndTime = System.currentTimeMillis();
            long actualDurationMs = testEndTime - testStartTime;
            double actualDurationSeconds = actualDurationMs / 1000.0;

            if (schedulerStopped && workersStopped) {
                log.info("Calling MetricsReporterClient");

                try { metricsCollector.flush(); } catch (Exception ignored) {}
                try { metricsReporterClient.finish(loadGeneratorId); } catch (Exception ignored) {}

                log.info("Scenario '{}' completed successfully. All threads finished.", scenarioConfig.getName());
                log.info("Test duration - Expected: {} ({}s), Actual: {}s",
                        scenarioConfig.getDuration(),
                        scenarioConfig.getDuration().getSeconds(),
                        String.format("%.2f", actualDurationSeconds));
            } else {
                log.warn("Scenario '{}' was interrupted while waiting for worker threads to finish. Actual runtime: {}s",
                        scenarioConfig.getName(), String.format("%.2f", actualDurationSeconds));
            }
        } catch (Exception e) {
            log.error("Error executing queries:", e);
        } finally {

            try { if (future != null) future.cancel(false); } catch (Exception ignored) {}

            shutdownExecutorService(scheduler);
            shutdownExecutorService(workers);
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

    private boolean awaitExecutorServiceTermination(ExecutorService executorService, String executorName, long totalTimeout, TimeUnit unit) {
        long deadline = System.nanoTime() + unit.toNanos(totalTimeout);
        try {
            while (System.nanoTime() < deadline) {
                if (executorService.awaitTermination(30, TimeUnit.SECONDS)) return true;
                log.info("Still waiting for {} to complete...", executorName);
            }
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for {} to complete", executorName, e);
            return false;
        }
    }
}
