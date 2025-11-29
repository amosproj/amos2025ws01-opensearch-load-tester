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

    private final LoadRunner loadRunner;
    private final MetricsCollectorService metricsCollectorService;
    private final ScenarioConfig scenarioConfig;
    @Value("${opensearch.url}")
    private String openSearchBaseUrl;

    public LoadTestController(LoadRunner loadRunner,
                              MetricsCollectorService metricsCollectorService,
                              ScenarioConfig scenarioConfig) {
        this.loadRunner = loadRunner;
        this.metricsCollectorService = metricsCollectorService;
        this.scenarioConfig = scenarioConfig;
    }

    /**
     * Endpoint to throw a query run against OpenSearch
     *
     */
    @PostMapping("/run")
    public ResponseEntity<String> runQuery(@RequestBody(required = false) QueryRunRequest request) {
        // Basic null check on request body
        if (request == null) {
            return ResponseEntity.badRequest().body("Request body must not be null\n");
        }

        String queryId = request.getQueryId();
        if (queryId == null || queryId.isBlank()) {
            return ResponseEntity.badRequest().body("queryId must not be empty\n");
        }

        int threads = request.getThreads();
        int iterations = request.getIterations();

        // Basic validation of numeric parameters
        if (threads <= 0 || iterations <= 0) {
            return ResponseEntity.badRequest()
                    .body("threads and iterations must be > 0\n");
        }
        // Optional: protect the system from insane values
        int MAX_THREADS = 50;
        int MAX_ITERATIONS = 1000;
        if (threads > MAX_THREADS || iterations > MAX_ITERATIONS) {
            log.warn("Invalid threads/iterations: threads={}, iterations={}", threads, iterations);
            return ResponseEntity.badRequest()
                    .body(String.format("threads must be <= %d and iterations <= %d\n",
                            MAX_THREADS, MAX_ITERATIONS));
        }

        // 1) Look up query template-file path
        final String templateFilePath = request.getQueryType().getTemplatePath();

        // Default index if none provided
        String indexName = request.getIndexName();
        if (indexName == null || indexName.isBlank()) {
            indexName = "ano_test";
        }

        // Ensure params is never null
        Map<String, String> params = request.getParams();
        if (params == null) {
            params = Map.of();
        }

        // 2) Build list of query execution tasks
        List<QueryExecutionTask> executions = new ArrayList<>();

        for (int t = 0; t < threads; t++) {
            for (int i = 0; i < iterations; i++) {
                String id = queryId + "-t" + t + "-it" + i;

                executions.add(
                        new QueryExecutionTask(
                                id,
                                indexName,
                                templateFilePath,
                                params,
                                openSearchBaseUrl,
                                metricsCollectorService
                        )
                );
            }
        }

        try {
            // 3) Execute all queries in parallel
            // This call blocks until all executions have finished
            loadRunner.executeQueries(executions);
        } catch (InterruptedException ie) {
            // Restore interrupt flag and inform the client
            Thread.currentThread().interrupt();
            log.error("Query run {} was interrupted", queryId, ie);
            return ResponseEntity.status(500)
                    .body("Query run was interrupted\n");
        } catch (Exception ex) {
            // Catch-all for unexpected runtime errors
            log.error("Unexpected error while executing query run {}", queryId, ex);
            return ResponseEntity.status(500)
                    .body("Unexpected error while executing query run\n");
        }

        log.debug("Query run {} finished successfully", queryId);
        return ResponseEntity.ok("Query run finished successfully\n");
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
        loadRunner.executeScenario(scenarioConfig);
    }
}


