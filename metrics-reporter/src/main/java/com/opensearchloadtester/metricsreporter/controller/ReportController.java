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

        // Check if all replicas have reported
        if (currentCount >= expectedReplicas) {
            log.info("All {} replicas have reported. Generating reports...", expectedReplicas);
            
            try {
                generateReports();
                
                String message = String.format(
                    "All metrics received (%d/%d replicas). Reports generated successfully!\n" +
                    "Total load generators: %d\n" +
                    "Total queries: %d\n",
                    currentCount, expectedReplicas, 
                    metricsMap.size(),
                    metricsMap.values().stream().mapToInt(m -> m.getRequestType().size()).sum()
                );
                
                if (jsonExportEnabled) {
                    message += "JSON report: " + reportService.getJsonReportPath() + "\n";
                }
                if (csvExportEnabled) {
                    message += "CSV report: " + reportService.getCsvReportPath() + "\n";
                }
                
                return ResponseEntity.ok(message);
                
            } catch (IOException e) {
                log.error("Failed to generate reports", e);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Metrics received but failed to generate reports: " + e.getMessage() + "\n");
            }
        } else {
            // Not all replicas reported yet
            return ResponseEntity.ok(
                String.format("Metrics stored successfully. Waiting for remaining replicas (%d/%d)\n",
                    currentCount, expectedReplicas)
            );
        }
    }
    
    /**
     * Generates reports from all collected metrics.
     * Called when all expected replicas have reported.
     *
     * @throws IOException if report generation fails
     */
    private void generateReports() throws IOException {
        List<Metrics> allMetrics = new ArrayList<>(metricsMap.values());
        
        log.info("Generating reports from {} load generator instances", allMetrics.size());
        
        if (jsonExportEnabled) {
            log.info("Generating JSON report...");
            for (Metrics metrics : allMetrics) {
                reportService.appendToJsonReport(metrics);
            }
            log.info("JSON report generated at: {}", reportService.getJsonReportPath());
        }
        
        if (csvExportEnabled) {
            log.info("Generating CSV report...");
            for (Metrics metrics : allMetrics) {
                reportService.appendToCsvReport(metrics);
            }
            log.info("CSV report generated at: {}", reportService.getCsvReportPath());
        }
        
        // Log aggregated statistics
        TestRunReport report = reportService.createReport(allMetrics);
        log.info("Report Summary - Total Queries: {}, Total Errors: {}, Load Generator Instances: {}", 
                report.getTotalQueries(), 
                report.getTotalErrors(), 
                report.getLoadGeneratorInstances().size());
    }

    /**
     * Returns all collected metrics as a list.
     * Thread-safe getter for metrics.
     *
     * @return List of all metrics
     */
    public List<Metrics> getMetrics() {
        return new ArrayList<>(metricsMap.values());
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Report Controller is running!\n");
    }

}
