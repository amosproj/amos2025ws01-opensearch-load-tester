package com.opensearchloadtester.loadgenerator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.loadgenerator.client.LoadTestStartSyncClient;
import com.opensearchloadtester.loadgenerator.client.MetricsReporterClient;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import com.opensearchloadtester.loadgenerator.service.LoadRunner;
import com.opensearchloadtester.loadgenerator.service.MetricsCollector;
import com.opensearchloadtester.loadgenerator.service.QueryExecutionTask;
import com.opensearchloadtester.loadgenerator.service.QueryPoolBuilder;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private final ObjectMapper objectMapper;

    public TestScenarioInitializer(
            @Value("${HOSTNAME}") String loadGeneratorId,
            @Value("${load.generator.replicas}") int numberLoadGenerators,
            ScenarioConfig scenarioConfig,
            LoadRunner loadRunner,
            OpenSearchGenericClient openSearchClient,
            LoadTestStartSyncClient loadTestStartSyncClient,
            MetricsReporterClient metricsReporterClient,
            ObjectMapper objectMapper
    ) {
        this.loadGeneratorId = loadGeneratorId;
        this.numberLoadGenerators = numberLoadGenerators;
        this.scenarioConfig = scenarioConfig;
        this.loadRunner = loadRunner;
        this.openSearchClient = openSearchClient;
        this.loadTestStartSyncClient = loadTestStartSyncClient;
        this.metricsReporterClient = metricsReporterClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) {
        log.info("Initializing load test with scenario {}", scenarioConfig.getName());

        if (scenarioConfig.getWarmUpEnabled()) {
            runWarmUp();
        }

        if (numberLoadGenerators > 1) {
            synchronizeStart();
        }

        log.info("Starting load test");
        loadRunner.executeScenario(scenarioConfig);
        log.info("Finished load test successfully");
    }

    private void runWarmUp() {
        log.info("Running warm-up until at least {} requests executed AND at least {} ms elapsed",
                WARMUP_REQUEST_COUNT, MIN_WARMUP_DURATION_MS);

        long warmupStart = System.currentTimeMillis();

        MetricsCollector warmupCollector = new MetricsCollector(metricsReporterClient, false);
        List<QueryType> queryPool = QueryPoolBuilder.build(scenarioConfig);

        QueryExecutionTask warmupTask = new QueryExecutionTask(
                loadGeneratorId,
                scenarioConfig.getDocumentType().getIndex(),
                queryPool,
                openSearchClient,
                warmupCollector,// warm-up metrics are ignored
                objectMapper
        );

        boolean atLeastOneSuccessful = false;
        int totalRequests = 0;

        while ((System.currentTimeMillis() - warmupStart) < MIN_WARMUP_DURATION_MS
                || totalRequests < WARMUP_REQUEST_COUNT) {

            try {
                warmupTask.run();
                atLeastOneSuccessful = true;
            } catch (RuntimeException ex) {
                log.debug("Warm-up request {} failed: {}", totalRequests, ex.getMessage());
            }

            totalRequests++;
        }

        long elapsedMs = System.currentTimeMillis() - warmupStart;

        // Define "warm-up successful": at least one successful OpenSearch query.
        if (!atLeastOneSuccessful) {
            throw new IllegalStateException(
                    String.format("Warm-up failed: 0 successful requests out of %s (%s ms total)",
                            totalRequests, elapsedMs)
            );
        }

        log.info("Warm-up completed in {} ms ({} total requests)",
                elapsedMs, totalRequests);
    }

    private void synchronizeStart() {
        log.info("Synchronizing global start with other Load Generators");

        loadTestStartSyncClient.registerReady(loadGeneratorId);
        loadTestStartSyncClient.awaitStartPermission();
    }

}
