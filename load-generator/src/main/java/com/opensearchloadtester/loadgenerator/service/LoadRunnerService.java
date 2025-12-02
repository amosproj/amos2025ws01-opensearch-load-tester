package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

/**
 * Service responsible for executing multiple queries simultaneously using thread pools.
 * Manages thread lifecycle and ensures proper shutdown when all queries are completed.
 */
@Slf4j
@Service
public class LoadRunnerService {

    private final MetricsReporterClient metricsReporterClient;
    private final MetricsCollectorService metricsCollectorService;

    @Value("${opensearch.url}")
    private String openSearchBaseUrl;

    public LoadRunnerService(MetricsReporterClient metricsReporterClient,
                             MetricsCollectorService metricsCollectorService) {
        this.metricsReporterClient = metricsReporterClient;
        this.metricsCollectorService = metricsCollectorService;
    }

    /**
     * Executes queries according to the ScenarioConfig
     *
     * @param scenarioConfig scenario configuration
     */
    public void execute(ScenarioConfig scenarioConfig) {

        // Parameter check
        if (scenarioConfig == null) {
            log.error("executeQueries: No configuration provided");
            return;
        }

        log.info("Started execution of scenario: {}", scenarioConfig.getName());
        log.info("Scenario duration: {} ({} seconds)",
                scenarioConfig.getDuration(),
                scenarioConfig.getDuration().getSeconds());

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

        // Track overall test start time
        long testStartTime = System.currentTimeMillis();

        ScheduledExecutorService executorService = Executors
                .newSingleThreadScheduledExecutor();

        try {
            long durationNs = scenarioConfig.getDuration().toNanos();
            int qpsTotal = scenarioConfig.getQueriesPerSecond();
            int qpsPerLoadGen = qpsTotal / Integer.parseInt(System.getenv("LOAD_GENERATOR_REPLICAS"));
            long durationPerQuery = 1000_000_000L / qpsPerLoadGen;
            log.debug("Schedule delay:  {} ms  ", durationPerQuery);

            // Start scheduled query execution
            ScheduledFuture<?> future = executorService.scheduleAtFixedRate(query, durationPerQuery / 2, durationPerQuery, TimeUnit.NANOSECONDS);
            executorService.schedule(() -> future.cancel(false), durationNs, TimeUnit.NANOSECONDS);

            // TODO: Wait for all threads to complete
            log.info("Waiting for all threads to complete");
            boolean completed = true;
            executorService.awaitTermination(durationNs + 2000_000_000L, TimeUnit.NANOSECONDS);
            // TODO: Set a timeout per queryExecution
            // boolean completed = latch.await(10, TimeUnit.MINUTES);

            long testEndTime = System.currentTimeMillis();
            long actualDurationMs = testEndTime - testStartTime;
            double actualDurationSeconds = actualDurationMs / 1000.0;


            if (completed) {
                log.info("Calling MetricsReporterClient");
                metricsReporterClient.reportMetrics(metricsCollectorService.getMetrics());
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
