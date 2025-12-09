package com.opensearchloadtester.metricsreporter.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opensearchloadtester.common.dto.LoadGeneratorReportDto;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.dto.LoadTestSummary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
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
    private final ObjectWriter ndjsonWriter;

    @Value("${report.output.directory:./reports}")
    private String outputDirectory;

    @Value("${report.stats.filename:statistics.json}")
    private String statsFilename;
    @Value("${report.csv.filename:query_results.csv}")
    private String csvFilename;
    @Value("${report.ndjson.filename:tmp_query_results.ndjson}")
    private String ndjsonFilename;
    @Value("${report.fulljson.filename:query_results.json}")
    private String fullJsonFilename;

    private final StatsAccumulator stats = new StatsAccumulator();
    private boolean filesInitialized = false;

    public ReportService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.ndjsonWriter = this.objectMapper.writer().without(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Processes incoming metrics in a streaming fashion:
     * - flattens the MetricsDto list into entries
     * - appends to CSV and NDJSON
     * - updates aggregated statistics in memory
     */
    public synchronized void processMetrics(List<MetricsDto> metricsList) throws IOException {
        // metricsList is already validated in the controller, so we can skip the validation here

        initializeReportFiles();
        appendToCsvReport(metricsList);
        appendToNdjsonReport(metricsList);
        stats.update(metricsList);
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
            String csvHeaders = "Load Generator ID,Query Type,Request Duration (ms),Query Duration (ms),Total Hits,HTTP Status Code\n";
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


    private void appendToCsvReport(List<MetricsDto> metricsList) throws IOException {
        Path csvPath = Paths.get(outputDirectory, csvFilename);

        if (!Files.exists(csvPath)) {
            initializeReportFiles();
        }

        try (FileWriter fileWriter = new FileWriter(csvPath.toFile(), true);
             CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT)) {

            for (MetricsDto metrics : metricsList) {
                csvPrinter.printRecord(
                        metrics.getLoadGeneratorId(),
                        metrics.getQueryType(),
                        metrics.getRequestDurationMillis(),
                        metrics.getQueryDurationMillis(),
                        metrics.getTotalHits(),
                        metrics.getHttpStatusCode()
                );
            }

            csvPrinter.flush();
        }

        log.info("Appended {} metrics entries to CSV report", metricsList.size());
    }

    private void appendToNdjsonReport(List<MetricsDto> metricsList) throws IOException {
        Path ndjsonPath = Paths.get(outputDirectory, ndjsonFilename);

        if (!Files.exists(ndjsonPath)) {
            initializeReportFiles();
        }

        try (FileWriter writer = new FileWriter(ndjsonPath.toFile(), true)) {
            for (MetricsDto metrics : metricsList) {
                writer.write(ndjsonWriter.writeValueAsString(metrics));
                writer.write("\n");
            }
            writer.flush();
        }

        log.info("Appended {} metrics entries to NDJSON report", metricsList.size());
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
     * Returns the absolute path to the full JSON report file containing all query results.
     */
    public Path getFullJsonReportPath() {
        return Paths.get(outputDirectory, fullJsonFilename).toAbsolutePath();
    }

    /**
     * Finalizes reports by writing a summary JSON without loading all query results into memory.
     */
    public synchronized LoadTestSummary finalizeReports(Set<String> loadGeneratorInstances) throws IOException {
        initializeReportFiles();

        LoadTestSummary.Statistics statistics = stats.toStatistics();

        LoadTestSummary report = new LoadTestSummary(
                statistics,
                LocalDateTime.now(),
                stats.getTotalQueries(),
                stats.getTotalErrors(),
                new ArrayList<>(), // omit query_results to stay lean
                new ArrayList<>(loadGeneratorInstances)
        );

        Path statsPath = Paths.get(outputDirectory, statsFilename);
        objectMapper.writeValue(statsPath.toFile(), report);
        Path ndjsonPath = Paths.get(outputDirectory, ndjsonFilename);
        Path fullJsonPath = Paths.get(outputDirectory, fullJsonFilename);
        writeFullJsonReport(ndjsonPath, fullJsonPath);

        log.info("Summary written: queries={}, errors={}, instances={}", report.getTotalQueries(), report.getTotalErrors(), report.getLoadGeneratorInstances().size());
        log.info("Request duration stats: avg={}ms min={}ms max={}ms | Query duration stats: avg={}ms min={}ms max={}ms",
                String.format("%.2f", statistics.getRequestDurationMs().getAverage()),
                statistics.getRequestDurationMs().getMin(),
                statistics.getRequestDurationMs().getMax(),
                String.format("%.2f", statistics.getQueryDurationMs().getAverage()),
                statistics.getQueryDurationMs().getMin(),
                statistics.getQueryDurationMs().getMax());

        return report;
    }

    /**
     * Builds a valid JSON array file from the NDJSON stream so tools like Grafana can import it.
     */
    private void writeFullJsonReport(Path ndjsonPath, Path fullJsonPath) throws IOException {
        if (!Files.exists(ndjsonPath)) {
            log.warn("NDJSON report file {} not found; skipping full JSON export", ndjsonPath.toAbsolutePath());
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(ndjsonPath);
             FileWriter writer = new FileWriter(fullJsonPath.toFile())) {
            JsonGenerator generator = objectMapper.getFactory().createGenerator(writer);
            generator.useDefaultPrettyPrinter();
            generator.writeStartArray();

            String line;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                generator.writeTree(objectMapper.readTree(line));
                count++;
            }

            generator.writeEndArray();
            generator.flush();
            log.info("Full JSON report written to {} with {} metrics entries", fullJsonPath.toAbsolutePath(), count);
        }
    }

    @lombok.Getter
    private static class StatsAccumulator {
        // total query count must stay below 2.147.483.647 (int max value) else we will have to use long for this field
        private int totalQueries = 0;
        private int totalErrors = 0;

        private long requestDurationCount = 0;
        private long requestDurationSum = 0;
        private long requestDurationMin = Long.MAX_VALUE;
        private long requestDurationMax = Long.MIN_VALUE;

        private long queryDurationCount = 0;
        private long queryDurationSum = 0;
        private long queryDurationMin = Long.MAX_VALUE;
        private long queryDurationMax = Long.MIN_VALUE;

        void update(List<MetricsDto> results) {
            for (MetricsDto result : results) {
                totalQueries++;


                Long requestDurationMs = result.getRequestDurationMillis();
                if (requestDurationMs != null) {
                    requestDurationCount++;
                    requestDurationSum += requestDurationMs;
                    requestDurationMin = Math.min(requestDurationMin, requestDurationMs);
                    requestDurationMax = Math.max(requestDurationMax, requestDurationMs);
                }

                Long queryDurationMs = result.getQueryDurationMillis();
                if (queryDurationMs != null && queryDurationMs >= 0) {
                    queryDurationCount++;
                    queryDurationSum += queryDurationMs;
                    queryDurationMin = Math.min(queryDurationMin, queryDurationMs);
                    queryDurationMax = Math.max(queryDurationMax, queryDurationMs);
                }
            }
        }

        LoadTestSummary.Statistics toStatistics() {
            LoadTestSummary.DurationStats requestDuration = new LoadTestSummary.DurationStats();
            if (requestDurationCount > 0) {
                requestDuration.setAverage(requestDurationSum / (double) requestDurationCount);
                requestDuration.setMin(requestDurationMin);
                requestDuration.setMax(requestDurationMax);
            } else {
                requestDuration.setAverage(0.0);
                requestDuration.setMin(0L);
                requestDuration.setMax(0L);
            }

            LoadTestSummary.DurationStats queryDuration = new LoadTestSummary.DurationStats();
            if (queryDurationCount > 0) {
                queryDuration.setAverage(queryDurationSum / (double) queryDurationCount);
                queryDuration.setMin(queryDurationMin);
                queryDuration.setMax(queryDurationMax);
            } else {
                queryDuration.setAverage(0.0);
                queryDuration.setMin(0L);
                queryDuration.setMax(0L);
            }

            return new LoadTestSummary.Statistics(requestDuration, queryDuration);
        }
    }
        ObjectNode toObjectNode(ObjectMapper mapper) {
            ObjectNode node = mapper.createObjectNode();
            node.put("load_generator_id", loadGeneratorId);
            node.put("scenario", scenario);
            node.put("query_type", queryType);
            if (requestDurationMs != null) {
                node.put("request_duration_ms", requestDurationMs);
            }
            if (queryDurationMs != null) {
                node.put("query_duration_ms", queryDurationMs);
            }
            if (totalHits != null) {
                node.put("total_hits", totalHits);
            }
            node.put("http_status_code", httpStatusCode);
            return node;
        }
}
