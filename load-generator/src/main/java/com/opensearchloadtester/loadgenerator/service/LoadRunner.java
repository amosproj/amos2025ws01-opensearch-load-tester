package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
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
                scenarioConfig.getName(), scenarioConfig.getScheduleDuration().getSeconds());

        log.info("Timout {}s", scenarioConfig.getQueryResponseTimeout().toSeconds());

        List<QueryType> queryPool = QueryPoolBuilder.build(scenarioConfig);

        QueryExecutionTask query = new QueryExecutionTask(
                loadGeneratorId,
                scenarioConfig.getDocumentType().getIndex(),
                queryPool,
                openSearchClient,
                metricsCollector,
                scenarioConfig.getQueryResponseTimeout()
        );

        // Track overall test start time
        long testStartTime = System.currentTimeMillis();

        ScheduledExecutorService scheduler = Executors
                .newSingleThreadScheduledExecutor();
        ExecutorService workers = Executors.newCachedThreadPool();

        ScheduledFuture<?> scheduledTaskForCleanup = null;

        try {
            long durationNs = scenarioConfig.getScheduleDuration().toNanos();
            int qpsTotal = scenarioConfig.getQueriesPerSecond();
            int qpsPerLoadGen = qpsTotal / numberLoadGenerators;
            long durationPerQuery = 1_000_000_000L / qpsPerLoadGen;
            AtomicInteger queryCounter = new AtomicInteger();
            log.debug("Schedule delay:  {} ns  ", durationPerQuery);

            // Start scheduled query execution
            final ScheduledFuture<?> scheduledTask = scheduler.scheduleAtFixedRate(() -> {
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
            scheduledTaskForCleanup = scheduledTask;
            // After the scenario duration, stop the periodic scheduling (already submitted tasks may still run).
            scheduler.schedule(() -> {
                scheduledTask.cancel(false);
                scheduler.shutdown();
            }, durationNs, TimeUnit.NANOSECONDS);

            boolean schedulerStopped = false;
            try {
                // Wait for the scheduler to stop because it populates worker threads
                schedulerStopped = scheduler.awaitTermination(
                        durationNs + TimeUnit.SECONDS.toNanos(10),
                        TimeUnit.NANOSECONDS);
                if (!schedulerStopped) {
                    log.warn("Scheduler did not stop in time; proceeding with worker shutdown");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while waiting for scheduler to stop", e);
            }

            // Now stop workers
            workers.shutdown();
            boolean workersStopped = awaitExecutorServiceTermination(workers, "workers");

            // Check if QPS fulfilled
            if (queryCounter.get() < qpsPerLoadGen * scenarioConfig.getScheduleDuration().toSeconds()) {
                log.warn("Load Generator can't keep up with QPS... please increase REPLICA amount!");
            }

            long testEndTime = System.currentTimeMillis();
            long actualDurationMs = testEndTime - testStartTime;
            double actualDurationSeconds = actualDurationMs / 1000.0;

            if (workersStopped) {
                log.info("Calling MetricsReporterClient");

                try {
                    metricsCollector.flush();
                } catch (Exception e) {
                    log.warn("Failed to flush metrics for {}", loadGeneratorId, e);
                }

                log.info("Scenario '{}' completed successfully. All threads finished.", scenarioConfig.getName());
                log.info("Schedule duration: {}s, Total duration: {}s",
                        scenarioConfig.getScheduleDuration().getSeconds(),
                        String.format("%.2f", actualDurationSeconds));
            } else {
                log.warn("Scenario '{}' was interrupted while waiting for worker threads to finish. Actual runtime: {}s",
                        scenarioConfig.getName(), String.format("%.2f", actualDurationSeconds));
            }
        } catch (Exception e) {
            throw new RuntimeException("Error executing queries", e);
        } finally {
            // Fallback cleanup if an exception skipped the normal shutdown path.
            if (scheduledTaskForCleanup != null) scheduledTaskForCleanup.cancel(false);
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

    private boolean awaitExecutorServiceTermination(ExecutorService executorService, String executorName) {
        try {
            while (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                log.info("Still waiting for {} to complete...", executorName);
            }
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while waiting for {} to complete", executorName, e);
            return false;
        }
    }

}
