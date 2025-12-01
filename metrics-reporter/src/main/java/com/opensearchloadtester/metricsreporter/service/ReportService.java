package com.opensearchloadtester.metricsreporter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opensearchloadtester.common.dto.Metrics;
import com.opensearchloadtester.metricsreporter.dto.QueryResult;
import com.opensearchloadtester.metricsreporter.dto.TestRunReport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service responsible for creating and exporting test run reports.
 * Supports JSON and CSV export formats for raw query data.
 */
@Slf4j
@Service
public class ReportService {

    private final ObjectMapper objectMapper;

    @Value("${report.output.directory:./reports}")
    private String outputDirectory;

    @Value("${report.stats.filename:statistics.json}")
    private String statsFilename;
    @Value("${report.csv.filename:test_run_report.csv}")
    private String csvFilename;
    @Value("${report.ndjson.filename:test_run_report.ndjson}")
    private String ndjsonFilename;

    private final StatsAccumulator stats = new StatsAccumulator();
    private boolean filesInitialized = false;

    public ReportService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * Processes incoming metrics in a streaming fashion:
     * - converts to QueryResults
     * - appends to CSV and NDJSON
     * - updates aggregated statistics in memory
     */
    public synchronized void processMetrics(Metrics metrics) throws IOException {
        initializeReportFiles();

        List<QueryResult> queryResults = convertMetricsToQueryResults(metrics);

        appendToCsvReport(queryResults);
        appendToNdjsonReport(queryResults);
        stats.update(queryResults);
    }

    /**
     * Initializes the report directory and creates empty report files if they don't exist.
     * Idempotent and guarded to avoid repeated work under concurrent calls.
     */
    private synchronized void initializeReportFiles() throws IOException {
        if (filesInitialized) {
            return;
        }

        Path dirPath = Paths.get(outputDirectory);

        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("Created report output directory: {}", dirPath.toAbsolutePath());
        }

        Path csvPath = Paths.get(outputDirectory, csvFilename);
        Path ndjsonPath = Paths.get(outputDirectory, ndjsonFilename);

        // Create CSV file with headers
        if (!Files.exists(csvPath)) {
            String csvHeaders = "Load Generator Instance,Request Type,Roundtrip (ms),OpenSearch Took (ms),Hits Count,Has Error,JSON Response\n";
            Files.writeString(csvPath, csvHeaders);
            log.info("Created initial CSV report file: {}", csvPath.toAbsolutePath());
        }

        // Create NDJSON file placeholder
        if (!Files.exists(ndjsonPath)) {
            Files.createFile(ndjsonPath);
            log.info("Created NDJSON report file: {}", ndjsonPath.toAbsolutePath());
        }

        filesInitialized = true;
    }


    private void appendToCsvReport(List<QueryResult> queryResults) throws IOException {
        Path csvPath = Paths.get(outputDirectory, csvFilename);

        if (!Files.exists(csvPath)) {
            initializeReportFiles();
        }

        try (FileWriter fileWriter = new FileWriter(csvPath.toFile(), true);
             CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT)) {

            for (QueryResult result : queryResults) {
                csvPrinter.printRecord(
                        result.getLoadGeneratorInstance(),
                        result.getRequestType(),
                        result.getRoundtripMs(),
                        result.getOpensearchTookMs(),
                        result.getHitsCount(),
                        result.getHasError(),
                        result.getJsonResponse()
                );
            }

            csvPrinter.flush();
        }

        log.info("Appended {} query results to CSV report", queryResults.size());
    }

    private void appendToNdjsonReport(List<QueryResult> queryResults) throws IOException {
        Path ndjsonPath = Paths.get(outputDirectory, ndjsonFilename);

        if (!Files.exists(ndjsonPath)) {
            initializeReportFiles();
        }

        try (FileWriter writer = new FileWriter(ndjsonPath.toFile(), true)) {
            for (QueryResult result : queryResults) {
                writer.write(objectMapper.writeValueAsString(result));
                writer.write("\n");
            }
            writer.flush();
        }

        log.info("Appended {} query results to NDJSON report", queryResults.size());
    }

    /**
     * Helper method to convert a Metrics object to a list of QueryResult objects.
     *
     * @param metrics Metrics object to convert
     * @return List of QueryResult objects
     */
    private List<QueryResult> convertMetricsToQueryResults(Metrics metrics) {
        List<QueryResult> queryResults = new ArrayList<>();
        String instanceName = metrics.getLoadGeneratorInstance();

        for (int i = 0; i < metrics.getRequestType().size(); i++) {
            String requestType = metrics.getRequestType(i);
            Long roundtripMs = metrics.getRoundtripMilSec(i);
            String jsonResponseStr = metrics.getJsonResponse(i);

            // Parse JSON response
            com.fasterxml.jackson.databind.JsonNode jsonResponse = null;
            Long opensearchTookMs = null;
            Integer hitsCount = null;
            boolean hasError = false;

            try {
                jsonResponse = objectMapper.readTree(jsonResponseStr);

                // Extract took
                if (jsonResponse.has("took")) {
                    opensearchTookMs = jsonResponse.get("took").asLong();
                }

                // Extract hits count
                if (jsonResponse.has("hits")) {
                    com.fasterxml.jackson.databind.JsonNode hitsNode = jsonResponse.get("hits");
                    if (hitsNode.has("total") && hitsNode.get("total").has("value")) {
                        hitsCount = hitsNode.get("total").get("value").asInt();
                    }
                }

                // Check for errors
                hasError = jsonResponse.has("error");

            } catch (Exception e) {
                log.warn("Failed to parse JSON response for {}: {}", requestType, e.getMessage());
                hasError = true;
            }

            QueryResult queryResult = new QueryResult(
                    requestType,
                    roundtripMs,
                    opensearchTookMs,
                    hitsCount,
                    jsonResponse,
                    hasError,
                    instanceName
            );

            queryResults.add(queryResult);
        }

        return queryResults;
    }


    /**
     * Returns the absolute path to the JSON statistics report file.
     *
     * @return Path to JSON statistics report file
     */
    public Path getStatisticsReportPath() {
        return Paths.get(outputDirectory, statsFilename).toAbsolutePath();
    }

    /**
     * Returns the absolute path to the CSV report file.
     *
     * @return Path to CSV report file
     */
    public Path getCsvReportPath() {
        return Paths.get(outputDirectory, csvFilename).toAbsolutePath();
    }

    /**
     * Returns the absolute path to the NDJSON report file.
     */
    public Path getNdjsonReportPath() {
        return Paths.get(outputDirectory, ndjsonFilename).toAbsolutePath();
    }

    /**
     * Finalizes reports by writing a summary JSON without loading all query results into memory.
     */
    public synchronized TestRunReport finalizeReports(Set<String> loadGeneratorInstances) throws IOException {
        initializeReportFiles();

        TestRunReport.Statistics statistics = stats.toStatistics();

        TestRunReport report = new TestRunReport(
                statistics,
                LocalDateTime.now(),
                stats.getTotalQueries(),
                stats.getTotalErrors(),
                new ArrayList<>(), // omit query_results to stay lean
                new ArrayList<>(loadGeneratorInstances)
        );

        Path statsPath = Paths.get(outputDirectory, statsFilename);
        objectMapper.writeValue(statsPath.toFile(), report);

        log.info("Summary written: queries={}, errors={}, instances={}", report.getTotalQueries(), report.getTotalErrors(), report.getLoadGeneratorInstances().size());
        log.info("Roundtrip stats: avg={}ms min={}ms max={}ms | Took stats: avg={}ms min={}ms max={}ms",
                String.format("%.2f", statistics.getRoundtripMs().getAverage()),
                statistics.getRoundtripMs().getMin(),
                statistics.getRoundtripMs().getMax(),
                String.format("%.2f", statistics.getOpensearchTookMs().getAverage()),
                statistics.getOpensearchTookMs().getMin(),
                statistics.getOpensearchTookMs().getMax());

        return report;
    }

    @lombok.Getter
    private static class StatsAccumulator {
        // Keep total
        private int totalQueries = 0;
        private int totalErrors = 0;

        private long roundtripCount = 0;
        private long roundtripSum = 0;
        private long roundtripMin = Long.MAX_VALUE;
        private long roundtripMax = Long.MIN_VALUE;

        private long tookCount = 0;
        private long tookSum = 0;
        private long tookMin = Long.MAX_VALUE;
        private long tookMax = Long.MIN_VALUE;

        void update(List<QueryResult> results) {
            for (QueryResult result : results) {
                totalQueries++;
                if (Boolean.TRUE.equals(result.getHasError())) {
                    totalErrors++;
                }

                Long rt = result.getRoundtripMs();
                if (rt != null) {
                    roundtripCount++;
                    roundtripSum += rt;
                    roundtripMin = Math.min(roundtripMin, rt);
                    roundtripMax = Math.max(roundtripMax, rt);
                }

                Long took = result.getOpensearchTookMs();
                if (took != null) {
                    tookCount++;
                    tookSum += took;
                    tookMin = Math.min(tookMin, took);
                    tookMax = Math.max(tookMax, took);
                }
            }
        }

        TestRunReport.Statistics toStatistics() {
            TestRunReport.RoundtripStats rts = new TestRunReport.RoundtripStats();
            if (roundtripCount > 0) {
                rts.setAverage(roundtripSum / (double) roundtripCount);
                rts.setMin(roundtripMin);
                rts.setMax(roundtripMax);
            } else {
                rts.setAverage(0.0);
                rts.setMin(0L);
                rts.setMax(0L);
            }

            TestRunReport.OpenSearchTookStats ots = new TestRunReport.OpenSearchTookStats();
            if (tookCount > 0) {
                ots.setAverage(tookSum / (double) tookCount);
                ots.setMin(tookMin);
                ots.setMax(tookMax);
            } else {
                ots.setAverage(0.0);
                ots.setMin(0L);
                ots.setMax(0L);
            }

            return new TestRunReport.Statistics(rts, ots);
        }
    }

}
