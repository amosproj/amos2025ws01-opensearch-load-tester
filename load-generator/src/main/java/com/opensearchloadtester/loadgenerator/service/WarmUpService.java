package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * WarmUpService performs a simple warm-up phase before load testing.
 *
 * - Sends a fixed number of lightweight requests to OpenSearch
 * - Uses the same query as the real scenario
 * - Does NOT report metrics
 * - Ensures OpenSearch caches and connections are warmed
 */
@Slf4j
@Service
public class WarmUpService {

    private final MetricsCollectorService metricsCollectorService;

    @Value("${opensearch.url}")
    private String openSearchBaseUrl;

    public WarmUpService(MetricsCollectorService metricsCollectorService) {
        this.metricsCollectorService = metricsCollectorService;
    }

    /**
     * Executes a simple warm-up routine using the scenario's query.
     */
    public void runWarmUp(ScenarioConfig scenarioConfig) {

        int warmupRequests = 40;      // Hardcoded request count
        long minWarmupMs = 10000L;     // Minimum warm-up duration: 2 seconds

        log.info("Warm-up: running {} requests", warmupRequests);

        long warmupStart = System.currentTimeMillis();   // TRACK START TIME

        try {
            // Build the SAME query as the real load test
            String templateFile = scenarioConfig.getQuery().getType().getTemplatePath();

            OpenSearchQueryExecution warmupQuery = new OpenSearchQueryExecution(
                    scenarioConfig.getName(),
                    scenarioConfig.getDocumentType().getIndex(),
                    templateFile,
                    scenarioConfig.getQuery().getParameters(),
                    openSearchBaseUrl,
                    metricsCollectorService
            );

            int success = 0;

            for (int i = 0; i < warmupRequests; i++) {
                try {
                    warmupQuery.run();
                    success++;
                } catch (Exception ignored) {
                    // Warm-up errors are ignored intentionally
                }
            }

            long spent = System.currentTimeMillis() - warmupStart;

            // Ensure the warm-up lasts at least 10 seconds
            if (spent < minWarmupMs) {
                Thread.sleep(minWarmupMs - spent);
            }

            long warmupEnd = System.currentTimeMillis();
            long warmupDurationMs = warmupEnd - warmupStart;

            log.info("Warm-up finished: {} successful requests", success);
            log.info("Warm-up duration: {} ms", warmupDurationMs);

        } catch (Exception e) {
            log.warn("Warm-up encountered an issue but continuing: {}", e.getMessage());
        }
    }
}
