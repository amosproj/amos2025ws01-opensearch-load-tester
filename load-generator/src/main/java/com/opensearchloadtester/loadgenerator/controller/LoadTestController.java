package com.opensearchloadtester.loadgenerator.controller;

import com.opensearchloadtester.loadgenerator.service.NoOpQueryExecution;
import com.opensearchloadtester.loadgenerator.service.QueryExecutionFactory;
import com.opensearchloadtester.loadgenerator.service.LoadRunnerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for triggering load tests.
 */
@Slf4j
@RestController
@RequestMapping("/api/load-test")
public class LoadTestController {

    private final LoadRunnerService loadRunnerService;

    public LoadTestController(LoadRunnerService loadRunnerService) {
        this.loadRunnerService = loadRunnerService;
    }

    /**
     * Starts a load test with n parallel query execution threads.
     * 
     * @param threadCount Number of parallel threads to spawn
     * @return Response indicating success or failure
     */
    @PostMapping("/start")
    public ResponseEntity<String> startLoadTest(@RequestParam(defaultValue = "5") int threadCount) {
        log.info("Received request to start load test with {} threads", threadCount);

        try {
            // Create query executions using factory pattern
            QueryExecutionFactory factory = threadId -> 
                new NoOpQueryExecution("query-" + threadId, 1000);

            loadRunnerService.executeQueries(threadCount, factory);

            return ResponseEntity.ok(
                String.format("Load test completed successfully with %d threads", threadCount)
            );
        } catch (InterruptedException e) {
            log.error("Load test was interrupted", e);
            Thread.currentThread().interrupt();
            return ResponseEntity.status(500)
                .body("Load test was interrupted: " + e.getMessage());
        } catch (Exception e) {
            log.error("Error executing load test", e);
            return ResponseEntity.status(500)
                .body("Error executing load test: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Load Generator is running!");
    }
}

