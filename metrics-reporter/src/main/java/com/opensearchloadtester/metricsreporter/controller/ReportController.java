package com.opensearchloadtester.metricsreporter.controller;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.config.ShutdownAfterResponseInterceptor;
import com.opensearchloadtester.metricsreporter.dto.StatisticsDto;
import com.opensearchloadtester.metricsreporter.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
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

    // Load generators that have sent at least one metrics batch
    private final Set<String> reportedInstances = ConcurrentHashMap.newKeySet();

    // Load generators that have completed sending all batches
    private final Set<String> finishedInstances = ConcurrentHashMap.newKeySet();
    private boolean finalized = false;

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
     * Stores incoming metrics batches. Does not finalize the run.
     * Finalization happens only after all replicas call /finish/{loadGeneratorId}.
     *
     */
    @PostMapping("/metrics")
    public synchronized ResponseEntity<String> submitMetrics(@RequestBody List<MetricsDto> metricsList) {
        Set<String> loadGeneratorIds = new HashSet<>();

        // Reject late batches after finalization
        if (finalized) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Run already finalized; metrics batch rejected\n");
        }

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
        log.info("Received {} metrics entries from load generator: {}", metricsList.size(), payloadLoadGeneratorId);

        // Immediately process and persist metrics to avoid unbounded in-memory growth
        try {
            reportService.processMetrics(metricsList);
        } catch (IOException e) {
            log.error("Failed to persist metrics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to persist metrics: " + e.getMessage() + "\n");
        }

        // Track that this load generator has reported at least one batch
        reportedInstances.addAll(loadGeneratorIds);
        int reportedCount = reportedInstances.size();

        log.info("Stored metrics from {}. Reported {}/{} replicas. Batch size: {}",
                loadGeneratorIds,
                reportedCount,
                expectedReplicas,
                metricsList.size());

        // Finalization happens only after all replicas call /finish/{id}.
        return ResponseEntity.ok(
                String.format("Metrics stored successfully. Reported replicas (%d/%d). Waiting for finish signals.\n",
                        reportedCount, expectedReplicas)
        );

    }
    /**
     * Called by each load generator when it has finished sending all metrics batches.
     * Generates reports only once all expected replicas have finished.
     */
    @PostMapping("/finish/{loadGeneratorId}")
    public synchronized ResponseEntity<String> finish(@PathVariable String loadGeneratorId,
                                                      HttpServletRequest request) {
        if (loadGeneratorId == null || loadGeneratorId.isBlank()) {
            return ResponseEntity.badRequest().body("Invalid loadGeneratorId\n");
        }

        // If the run is already finalized, finish is idempotent: return 200 (OK)
        if (finalized) {
            log.info("Finish received from {} but run is already finalized - returning 200 OK", loadGeneratorId);
            return ResponseEntity.ok("Run already finalized\n");
        }

        boolean alreadyFinished = finishedInstances.contains(loadGeneratorId);
        if (!alreadyFinished) {
            finishedInstances.add(loadGeneratorId);
        }
        int finishedCount = finishedInstances.size();

        if (alreadyFinished) {
            log.info("Duplicate finish received from {}. Finished {}/{}",
                    loadGeneratorId, finishedCount, expectedReplicas);
        } else {
            log.info("Received finish from {}. Finished {}/{}",
                    loadGeneratorId, finishedCount, expectedReplicas);
        }

        // Only finalize once all replicas have explicitly finished sending batches
        if (finishedCount >= expectedReplicas) {
            log.info("All {} replicas finished. Generating reports...", expectedReplicas);

            finalized = true; // idempotency guard: prevent double-finalize
            try {
                StatisticsDto summary = reportService.finalizeReports(reportedInstances);

                StringBuilder message = new StringBuilder(String.format(
                        "All load generators finished (%d/%d). Reports generated successfully!\n" +
                                "Total load generators: %d\n" +
                                "Total queries: %d\n",
                        finishedCount, expectedReplicas,
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

                // Reset in-memory state for next run
                reportedInstances.clear();
                finishedInstances.clear();
                reportService.resetForNewRun();
                finalized = false;
                // Mark request for application shutdown AFTER response completed
                request.setAttribute(ShutdownAfterResponseInterceptor.SHUTDOWN_AFTER_RESPONSE, true);

                return ResponseEntity.ok(message.toString());

            } catch (IOException e) {
                log.error("Failed to generate reports", e);
                finalized = false;
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Finish received but failed to generate reports: " + e.getMessage() + "\n");
            }
        }

        if (alreadyFinished) {
            return ResponseEntity.ok("Finish already received\n");
        }

        return ResponseEntity.ok(
                String.format("Finish stored. Waiting for remaining load generators (%d/%d)\n",
                        finishedCount, expectedReplicas)
        );
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
