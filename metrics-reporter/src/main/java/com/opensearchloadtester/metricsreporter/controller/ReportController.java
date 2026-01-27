package com.opensearchloadtester.metricsreporter.controller;

import com.opensearchloadtester.common.dto.FinishLoadTestDto;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.config.ShutdownAfterResponseInterceptor;
import com.opensearchloadtester.metricsreporter.dto.StatisticsDto;
import com.opensearchloadtester.metricsreporter.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@RequiredArgsConstructor
public class ReportController {

    // Load Generators that have submitted at least one metrics batch
    private final Set<String> reportedLoadGenerators = ConcurrentHashMap.newKeySet();

    // Load Generators that have finished their run, either successfully or with an error
    private final ConcurrentHashMap<String, FinishLoadTestDto> finishedLoadGenerators = new ConcurrentHashMap<>();

    @Value("${load.generator.replicas}")
    private int expectedLoadGenerators;

    @Value("${report.export.json.enabled}")
    private boolean jsonExportEnabled;

    private final ReportService reportService;
    private boolean loadTestFinished = false;

    /**
     * This Post request saves the received metrics to thread-safe storage.
     * Stores incoming metrics batches. Does not finalize the run.
     * Finalization happens only after all replicas call /finish.
     *
     */
    @PostMapping("/metrics")
    public synchronized ResponseEntity<String> submitMetrics(@RequestBody List<MetricsDto> metricsList) {
        Set<String> loadGeneratorIds = new HashSet<>();

        // Reject late batches after finalization
        if (loadTestFinished) {
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
        reportedLoadGenerators.addAll(loadGeneratorIds);
        int reportedCount = reportedLoadGenerators.size();

        log.info("Stored metrics from {}. Reported {}/{} replicas. Batch size: {}",
                loadGeneratorIds,
                reportedCount,
                expectedLoadGenerators,
                metricsList.size());

        // Finalization happens only after all replicas call /finish/{id}.
        return ResponseEntity.ok(
                String.format("Metrics stored successfully. Reported replicas (%d/%d). Waiting for finish signals.\n",
                        reportedCount, expectedLoadGenerators)
        );
    }

    /**
     * Called by each Load Generator after finishing its run, either successfully or with an error.
     * Generates reports once all expected Load Generators have finished.
     */
    @PostMapping("/finish")
    public synchronized ResponseEntity<String> finish(@Valid @RequestBody FinishLoadTestDto finishLoadTestDto,
                                                      HttpServletRequest request) {

        // Idempotency guard: prevents generating the reports more than once
        if (loadTestFinished) {
            return ResponseEntity.ok().build();
        }

        if (finishedLoadGenerators.containsKey(finishLoadTestDto.getLoadGeneratorId())) {
            log.debug("Load Generator with id '{}' was already marked as FINISHED",
                    finishLoadTestDto.getLoadGeneratorId());
            return ResponseEntity.ok().build();
        }

        finishedLoadGenerators.put(finishLoadTestDto.getLoadGeneratorId(), finishLoadTestDto);

        log.info("Load Generator with id '{}' marked as FINISHED with {} ({}/{})",
                finishLoadTestDto.getLoadGeneratorId(),
                finishLoadTestDto.isSuccess() ? "SUCCESS" : "ERROR",
                finishedLoadGenerators.size(),
                expectedLoadGenerators);

        // Generate reports only after all load generators have finished their run
        if (finishedLoadGenerators.size() == expectedLoadGenerators) {
            loadTestFinished = true;

            log.info("All {} Load Generators finished their run. Generating reports...",
                    finishedLoadGenerators.size());

            List<FinishLoadTestDto> failedLoadGenerators = finishedLoadGenerators.values().stream()
                    .filter(dto -> !dto.isSuccess())
                    .toList();

            if (!failedLoadGenerators.isEmpty()) {
                logFailedLoadGenerators(failedLoadGenerators);
            }

            try {
                StatisticsDto summary = reportService.finalizeReports(reportedLoadGenerators);

                StringBuilder message = new StringBuilder(String.format(
                        "Reports generated successfully!\n" +
                                "Total Load Generators: %d/%d\n" +
                                "Total Queries executed: %d\n",
                        summary.getLoadGeneratorInstances().size(), expectedLoadGenerators,
                        summary.getTotalQueries()
                ));

                if (jsonExportEnabled) {
                    message.append("Results JSON report: ").append(reportService.getResultsJsonPath()).append("\n");
                    message.append("Statistics JSON: ").append(reportService.getStatisticsReportPath());
                }

                log.info(message.toString());

                // Mark request for application shutdown AFTER response completed
                request.setAttribute(ShutdownAfterResponseInterceptor.SHUTDOWN_AFTER_RESPONSE, true);
                int exitCode = failedLoadGenerators.isEmpty()
                        ? ShutdownAfterResponseInterceptor.EXIT_OK
                        : ShutdownAfterResponseInterceptor.EXIT_LOAD_GENERATOR_FAILED;
                request.setAttribute(ShutdownAfterResponseInterceptor.EXIT_CODE, exitCode);

                return ResponseEntity.ok().build();
            } catch (IOException e) {
                log.error("Failed to generate reports", e);
                loadTestFinished = false;
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Finish received but failed to generate reports: " + e.getMessage() + "\n");
            }
        }

        return ResponseEntity.ok().build();
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

    private void logFailedLoadGenerators(List<FinishLoadTestDto> failedLoadGenerators) {
        StringBuilder warning = new StringBuilder();
        warning.append("The following Load Generators finished with an error:\n");

        for (FinishLoadTestDto dto : failedLoadGenerators) {
            warning.append("- Load Generator '")
                    .append(dto.getLoadGeneratorId())
                    .append("': ")
                    .append(dto.getErrorMessage() != null ? dto.getErrorMessage() : "No error message provided")
                    .append("\n");
        }

        warning.append(
                "The load test may not have been executed at the fullest configured load, " +
                        "and the generated reports may not contain all expected metrics."
        );

        log.warn(warning.toString());
    }

}
