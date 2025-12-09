package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import com.opensearchloadtester.loadgenerator.service.LoadRunner;
import com.opensearchloadtester.loadgenerator.service.MetricsCollector;
import com.opensearchloadtester.loadgenerator.service.QueryExecutionTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestScenarioInitializer implements CommandLineRunner {

    private static final int WARMUP_REQUEST_COUNT = 40;
    private static final long MIN_WARMUP_DURATION_MS = 10_000L;

    private final ScenarioConfig scenarioConfig;
    private final LoadRunner loadRunner;
    private final OpenSearchGenericClient openSearchClient;

    @Override
    public void run(String... args) {
        log.info("Initializing test scenario: {}", scenarioConfig.getName());
        try {
            // 1) Optional warm-up phase
            if (scenarioConfig.isWarmUpEnabled()) {
                log.info("Warm-up is enabled for this scenario.");
                runWarmUp();
                log.info("Warm-up completed. Starting main load test...");
            } else {
                log.info("Warm-up is disabled. Starting main load test directly...");
            }

            // 2) Real load test execution
            loadRunner.executeScenario(scenarioConfig);
            log.info("Finished load test successfully");
        } catch (Exception e) {
            // Only one log line with stacktrace – no duplicate
            log.error("Unexpected error while executing load test", e);
            throw new RuntimeException("Failed to execute load test", e);
        }
    }

    private void runWarmUp() {
        log.info("Warm-up: running until at least {} requests AND at least {} ms",
                WARMUP_REQUEST_COUNT, MIN_WARMUP_DURATION_MS);

        long warmupStart = System.currentTimeMillis();

        String queryTemplatePath = scenarioConfig.getQuery().getType().getTemplatePath();

        QueryExecutionTask warmupTask = new QueryExecutionTask(
                scenarioConfig.getName() + "-warmup",
                scenarioConfig.getDocumentType().getIndex(),
                queryTemplatePath,
                scenarioConfig.getQuery().getParameters(),
                openSearchClient,
                new NoOpMetricsCollector() // warm-up metrics are ignored
        );

        int successCount = 0;
        int failureCount = 0;
        int totalRequests = 0;

        // Continue sending warm-up requests while:
        //  - The minimum warm-up time has not been reached, OR
        //  - The minimum number of warm-up requests has not yet been executed
        while ((System.currentTimeMillis() - warmupStart) < MIN_WARMUP_DURATION_MS
                || totalRequests < WARMUP_REQUEST_COUNT) {

            try {
                warmupTask.run();
                successCount++;
            } catch (RuntimeException ex) {
                // Count failed warm-up executions, but do not stop the warm-up.
                // Failures are logged only at debug level.
                failureCount++;
                log.debug("Warm-up request {} failed: {}", totalRequests, ex.getMessage());
            }

            totalRequests++;
        }

        long elapsedMs = System.currentTimeMillis() - warmupStart;

        // Define "warm-up successful": at least one successful OpenSearch query.
        if (successCount == 0) {
            log.error("Warm-up failed: 0 successful requests out of {} ({} ms total)",
                    totalRequests, elapsedMs);
            throw new IllegalStateException(
                    "Warm-up failed: no successful OpenSearch queries during warm-up");
        }

        log.info("Warm-up completed in {} ms: {} successful, {} failed ({} total)",
                elapsedMs, successCount, failureCount, totalRequests);
    }

    /**
     * MetricsCollector implementation that intentionally ignores all metrics.
     * This ensures that warm-up traffic does not appear in the final reports.
     */
    static class NoOpMetricsCollector extends MetricsCollector {
        @Override
        public void appendMetrics(String requestType, long roundtripMilSec, String jsonResponse) {
            // Intentionally left blank: no metrics are collected during warm-up.
        }
    }
}
