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


    // Number of warm-up requests to send before the real load test.

    private static final int WARMUP_REQUEST_COUNT = 40;

    //Minimum warm-up duration in milliseconds.

    private static final long MIN_WARMUP_DURATION_MS = 10_000L;

    private final ScenarioConfig scenarioConfig;
    private final LoadRunner loadRunner;
    private final OpenSearchGenericClient openSearchClient;

    @Override
    public void run(String... args) {
        log.info("Initializing test scenario: {}", scenarioConfig.getName());
        try {
            // 1) Warm-up phase
            runWarmUp();

            log.info("Warm-up completed. Starting main load test...");

            // 2) Real load test execution
            loadRunner.executeScenario(scenarioConfig);
            log.info("Finished load test successfully");
        } catch (Exception e) {
            log.error("Unexpected error while executing load test: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute load test", e);
        }
    }

    // Executes a warm-up phase before the actual load test.

    private void runWarmUp() {
        log.info("Warm-up: running at least {} requests (minimum duration: {} ms)",
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

        log.info("Warm-up finished: {} successful, {} failed ({} total) in {} ms",
                successCount, failureCount, totalRequests, elapsedMs);
    }

}
