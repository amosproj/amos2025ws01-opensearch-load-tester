package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.loadgenerator.client.LoadTestStartSyncClient;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import com.opensearchloadtester.loadgenerator.service.LoadRunner;
import com.opensearchloadtester.loadgenerator.service.MetricsCollector;
import com.opensearchloadtester.loadgenerator.service.QueryExecutionTask;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;

@Slf4j
@Component
public class TestScenarioInitializer implements CommandLineRunner {

    private static final int WARMUP_REQUEST_COUNT = 40;
    private static final long MIN_WARMUP_DURATION_MS = 10_000L;

    private final String loadGeneratorId;
    private final int numberLoadGenerators;
    private final ScenarioConfig scenarioConfig;
    private final LoadRunner loadRunner;
    private final OpenSearchGenericClient openSearchClient;
    private final LoadTestStartSyncClient loadTestStartSyncClient;
    private final MetricsReporterClient metricsReporterClient;

    public TestScenarioInitializer(
            @Value("${HOSTNAME}") String loadGeneratorId,
            @Value("${load.generator.replicas}") int numberLoadGenerators,
            ScenarioConfig scenarioConfig,
            LoadRunner loadRunner,
            OpenSearchGenericClient openSearchClient,
            LoadTestStartSyncClient loadTestStartSyncClient,
            MetricsReporterClient metricsReporterClient
    ) {
        this.loadGeneratorId = loadGeneratorId;
        this.numberLoadGenerators = numberLoadGenerators;
        this.scenarioConfig = scenarioConfig;
        this.loadRunner = loadRunner;
        this.openSearchClient = openSearchClient;
        this.loadTestStartSyncClient = loadTestStartSyncClient;
        this.metricsReporterClient = metricsReporterClient;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing load test with scenario {}", scenarioConfig.getName());

        // 1) Optional warm-up
        if (scenarioConfig.isWarmUpEnabled()) {
            runWarmUp();
        }

        // 2) Sync load test start with other Load Generators
        if (numberLoadGenerators > 1) {
            synchronizeStart();
        }

        // 3) Execute load test
        log.info("Starting load test");
        loadRunner.executeScenario(scenarioConfig);
        log.info("Finished load test successfully");
    }

    private void runWarmUp() {
        log.info("Running warm-up until at least {} requests executed AND at least {} ms elapsed",
                WARMUP_REQUEST_COUNT, MIN_WARMUP_DURATION_MS);

        long warmupStart = System.currentTimeMillis();

        MetricsCollector warmupCollector = new MetricsCollector(metricsReporterClient, 1, false);

        QueryExecutionTask warmupTask = new QueryExecutionTask(
                loadGeneratorId,
                scenarioConfig.getDocumentType().getIndex(),
                scenarioConfig.getQueryTypes(),
                openSearchClient,
                warmupCollector
                // warm-up metrics are ignored
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
                failureCount++;
                log.debug("Warm-up request {} failed: {}", totalRequests, ex.getMessage());
            }

            totalRequests++;
        }

        long elapsedMs = System.currentTimeMillis() - warmupStart;

        // Define "warm-up successful": at least one successful OpenSearch query.
        if (successCount == 0) {
            throw new IllegalStateException(
                    String.format("Warm-up failed: 0 successful requests out of %s (%s ms total)",
                            totalRequests, elapsedMs)
            );
        }

        log.info("Warm-up completed in {} ms: {} successful, {} failed ({} total)",
                elapsedMs, successCount, failureCount, totalRequests);
    }

    private void synchronizeStart() {
        log.info("Synchronizing global start with other Load Generators");

        loadTestStartSyncClient.registerReady(loadGeneratorId);
        loadTestStartSyncClient.awaitStartPermission();
    }

}
