package com.opensearchloadtester.metricsreporter.controller;

import com.opensearchloadtester.metricsreporter.dto.Metrics;
import com.opensearchloadtester.metricsreporter.dto.TestRunReport;
import com.opensearchloadtester.metricsreporter.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/api")
public class ReportController {

    // Thread-safe storage for metrics from different load generator instances
    private final ConcurrentHashMap<String, Metrics> metricsMap = new ConcurrentHashMap<>();
    
    // Counter for received reports
    private final AtomicInteger receivedReports = new AtomicInteger(0);
    
    @Value("${load.generator.replicas:1}")
    private int expectedReplicas;
    
    @Value("${report.export.json.enabled:true}")
    private boolean jsonExportEnabled;
    
    @Value("${report.export.csv.enabled:true}")
    private boolean csvExportEnabled;
    
    @Autowired
    private ReportService reportService;

    /**
     * This Post request saves the received metrics to thread-safe storage.
     * When all expected load generator replicas have reported, it triggers report generation.
     *
     * @param metrics DTO for metrics data
     * @return ResponseEntity with status message
     */
    @PostMapping("/addMetrics")
    public synchronized ResponseEntity<String> addMetrics(@RequestBody Metrics metrics) {
        log.info("Received metrics from load generator: {}", metrics.getLoadGeneratorInstance());

        // Validate params
        if (metrics.getRequestType() == null || metrics.getRoundtripMilSec() == null || 
            metrics.getJsonResponse() == null || metrics.getLoadGeneratorInstance() == null) {
            log.error("Invalid request parameters - requestType:{}, roundtripMilSec:{}, jsonResponse:{}, instance:{}", 
                    metrics.getRequestType(), metrics.getRoundtripMilSec(), 
                    metrics.getJsonResponse(), metrics.getLoadGeneratorInstance());
            return ResponseEntity.badRequest().body("Invalid metrics data\n");
        }

        // Store metrics in thread-safe map
        metricsMap.put(metrics.getLoadGeneratorInstance(), metrics);
        int currentCount = receivedReports.incrementAndGet();
        
        log.info("Stored metrics from {}. Received {}/{} replicas. Query count: {}", 
                metrics.getLoadGeneratorInstance(), 
                currentCount, 
                expectedReplicas,
                metrics.getRequestType().size());

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
