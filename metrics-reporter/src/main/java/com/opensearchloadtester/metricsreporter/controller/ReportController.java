package com.opensearchloadtester.metricsreporter.controller;

import com.opensearchloadtester.metricsreporter.dto.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/metrics-reporter-test")
public class ReportController {

    private Metrics metrics;

    /**
     * This Post request saves the recieved parameter to local vars
     *
     * @param metrics DTO for metrics data
     */
    @PostMapping("/setMetrics")
    public ResponseEntity<String> setMetrics(@RequestBody Metrics metrics) {
        log.info("Received request to receive metrics");

        // Check params
        if (metrics.getRequestType().isBlank() || metrics.getRoundtripMilSec() == 0 || metrics.getJsonResponse().isBlank()) {
            log.error("Invalid request parameters requestType:{}, roundtripMilSec:{}, jsonResponse:{} \n",
                    metrics.getRequestType(), metrics.getRoundtripMilSec(), metrics.getJsonResponse());
            return ResponseEntity.badRequest().build();
        }

        this.metrics = metrics;

        log.debug("Received data: requestType:{}, roundtripMilSec:{}, jsonResponse:{} \n",
                metrics.getRequestType(), metrics.getRoundtripMilSec(), metrics.getJsonResponse());

        log.info("Metrics stored successfully!\n");
        return ResponseEntity.ok("Metrics stored successfully!\n");
    }

    // Simple getter
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Metrics Reporter is running!\n");
    }

}
