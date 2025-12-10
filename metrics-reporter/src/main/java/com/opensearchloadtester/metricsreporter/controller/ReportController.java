package com.opensearchloadtester.metricsreporter.controller;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.dto.LoadTestSummaryDto;
import com.opensearchloadtester.metricsreporter.service.ReportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestController
@RequestMapping("/api")
public class ReportController {

    // Track unique reporting instances without keeping full metrics in memory
    private final Set<String> reportedInstances = ConcurrentHashMap.newKeySet();

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
    @PostMapping("/metrics")
    public synchronized ResponseEntity<String> submitMetrics(@RequestBody List<MetricsDto> metricsList) {
        Set<String> loadGeneratorIds = new HashSet<>();

        // Validate payload (empty payload is invalid)
        if (metricsList == null || metricsList.isEmpty()) {
            log.error("Received empty metrics payload");
            return ResponseEntity.badRequest().body("Invalid metrics payload\n");
        }

        // Validate metrics entries
        String payloadLoadGeneratorId = null;
        for (int i = 0; i < metricsList.size(); i++) {
            MetricsDto metrics = metricsList.get(i);
            String validationError = validateMetrics(metrics);
            if (validationError != null) {
                log.error("Invalid metrics entry at index {}: {}", i, validationError);
                return ResponseEntity.badRequest().body("Invalid metrics payload\n");
            }
            // Validate that all metrics entries have the same loadGeneratorId
            if (payloadLoadGeneratorId == null) {
                payloadLoadGeneratorId = metrics.getLoadGeneratorId();
            } else if (!payloadLoadGeneratorId.equals(metrics.getLoadGeneratorId())) {
                log.error("Mixed loadGeneratorId values in one payload (first: {}, current: {}, index: {})",
                        payloadLoadGeneratorId, metrics.getLoadGeneratorId(), i);
                return ResponseEntity.badRequest().body("Invalid metrics payload\n");
            }
        }

        loadGeneratorIds.add(payloadLoadGeneratorId);
        log.info("Received {} metrics entries from load generators: {}", metricsList.size(), loadGeneratorIds);

        // Immediately process and persist metrics to avoid unbounded in-memory growth
        try {
            reportService.processMetrics(metricsList);
        } catch (IOException e) {
            log.error("Failed to persist metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to persist metrics: " + e.getMessage() + "\n");
        }

        // Count unique reporting instances
        reportedInstances.addAll(loadGeneratorIds);
        int currentCount = reportedInstances.size();

        log.info("Stored metrics from {}. Received {}/{} replicas. Query count: {}",
                loadGeneratorIds,
                currentCount,
                expectedReplicas,
                metricsList.size());

        // Check if all replicas have reported
        if (currentCount >= expectedReplicas) {
            log.info("All {} replicas have reported. Generating reports...", expectedReplicas);

            try {
                LoadTestSummaryDto summary = reportService.finalizeReports(reportedInstances);

                StringBuilder message = new StringBuilder(String.format(
                        "All metrics received (%d/%d replicas). Reports generated successfully!\n" +
                                "Total load generators: %d\n" +
                                "Total queries: %d\n",
                        currentCount, expectedReplicas,
                        summary.getLoadGeneratorInstances().size(),
                        summary.getTotalQueries()
                ));

                if (jsonExportEnabled) {
                    message.append("Full JSON report: ").append(reportService.getFullJsonReportPath()).append("\n");
                    message.append("Statistics JSON: ").append(reportService.getStatisticsReportPath()).append("\n");
                }
                if (csvExportEnabled) {
                    message.append("CSV report: ").append(reportService.getCsvReportPath()).append("\n");
                }

                return ResponseEntity.ok(message.toString());

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
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Report Controller is running!\n");
    }

    // Validate a single metrics entry
    // Returns a string with the validation error, or null if the metrics entry is valid
    private String validateMetrics(MetricsDto metrics) {
        if (metrics == null) {
            return "metrics entry is null";
        }
        if (metrics.getLoadGeneratorId() == null || metrics.getLoadGeneratorId().isBlank()) {
            return "loadGeneratorId is missing";
        }
        if (metrics.getQueryType() == null || metrics.getQueryType().isBlank()) {
            return "queryType is missing";
        }
        if (metrics.getRequestDurationMillis() != null && metrics.getRequestDurationMillis() < 0) {
            return "requestDurationMillis is negative";
        }
        if (metrics.getQueryDurationMillis() != null && metrics.getQueryDurationMillis() < 0) {
            return "queryDurationMillis is negative";
        }
        if (metrics.getHttpStatusCode() < 100 || metrics.getHttpStatusCode() > 599) {
            return "httpStatusCode is out of range";
        }
        return null;
    }

}
