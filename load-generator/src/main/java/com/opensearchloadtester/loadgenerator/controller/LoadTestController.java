package com.opensearchloadtester.loadgenerator.controller;

import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import com.opensearchloadtester.loadgenerator.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * REST controller for triggering load tests and ad-hoc query runs
 * against OpenSearch.
 * <p>
 * Endpoints:
 * - POST /api/load-test/start : demo endpoint using NoOpQueryExecution
 * - POST /api/load-test/run   : real endpoint executing JSON template-based queries
 * - GET  /api/load-test/queries : list available queryIds from QueryRegistry
 * - GET  /api/load-test/health  : basic health check
 * <p>
 * It delegates the actual parallel execution to LoadRunnerService.
 */
@Slf4j
@RestController
@RequestMapping("/api/load-test")
public class LoadTestController {

    private final LoadRunnerService loadRunnerService;
    private final QueryRegistry queryRegistry;
    private final MetricsCollectorService metricsCollectorService;
    private final ScenarioConfig scenarioConfig;
    @Value("${opensearch.url}")
    private String openSearchBaseUrl;

    public LoadTestController(LoadRunnerService loadRunnerService,
                              QueryRegistry queryRegistry,
                              MetricsCollectorService metricsCollectorService,
                              ScenarioConfig scenarioConfig) {
        this.loadRunnerService = loadRunnerService;
        this.queryRegistry = queryRegistry;
        this.metricsCollectorService = metricsCollectorService;
        this.scenarioConfig = scenarioConfig;
    }

    /**
     * List of ids of the querys that where defined in the query registry
     */
    @GetMapping("/queries")
    public ResponseEntity<?> listQueries() {
        return ResponseEntity.ok(queryRegistry.listQueryIds());
    }


    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Load Generator is running!\n");
    }

    @PostMapping("/test")
    public void testExecution() {
        log.debug("Config file: {}", scenarioConfig.toString());
        loadRunnerService.execute(scenarioConfig);
    }
}


