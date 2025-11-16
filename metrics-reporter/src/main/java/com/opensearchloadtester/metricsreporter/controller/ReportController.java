package com.opensearchloadtester.metricsreporter.controller;

import com.opensearchloadtester.metricsreporter.dto.Metrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@Slf4j
@RestController
@RequestMapping("/api/metrics-reporter-test")
public class ReportController {

    private int size;
    private ArrayList<Metrics> metrics = new ArrayList<>();

    /**
     * This Post request saves the recieved parameter to local vars
     *
     * @param metrics DTO for metrics data
     */
    @PostMapping("/addMetrics")
    public ResponseEntity<String> addMetrics(@RequestBody Metrics metrics) {
        log.info("Received request to receive metrics");

        // Check params
        if (metrics.getRequestType().isBlank() || metrics.getRoundtripMilSec() == 0 || metrics.getJsonResponse().isBlank()) {
            log.error("Invalid request parameters requestType:{}, roundtripMilSec:{}, jsonResponse:{} \n",
                    metrics.getRequestType(), metrics.getRoundtripMilSec(), metrics.getJsonResponse());
            return ResponseEntity.badRequest().build();
        }

        this.metrics.add(metrics);

        log.debug("Received data from {}: requestType:{}, roundtripMilSec:{}, jsonResponse:{} \n",
                metrics.getLoadGeneratorInstance(), metrics.getRequestType(), metrics.getRoundtripMilSec(), metrics.getJsonResponse());

        log.info("Metrics stored successfully!\n");
        return ResponseEntity.ok("Metrics stored successfully!\n");
    }

    // Simple getter
    public ArrayList<Metrics> getMetrics() {
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
