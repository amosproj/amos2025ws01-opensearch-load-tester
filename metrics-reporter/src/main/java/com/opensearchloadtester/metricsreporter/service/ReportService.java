package com.opensearchloadtester.metricsreporter.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.dto.StatisticsDto;
import lombok.extern.slf4j.Slf4j;
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
 * Supports JSON export formats for raw query data.
 */
@Slf4j
@Service
public class ReportService {

    private final ObjectMapper objectMapper;
    private final ObjectWriter ndjsonWriter;

    @Value("${report.output.directory}")
    private String outputDirectory;

    @Value("${report.stats.filename:statistics.json}")
    private String statsFilename;
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
        this.objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        this.ndjsonWriter = this.objectMapper.writer().without(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Processes incoming metrics in a streaming fashion:
     * - flattens the MetricsDto list into entries
     * - appends to NDJSON
     * - updates aggregated statistics in memory
     */
    public synchronized void processMetrics(List<MetricsDto> metricsList) throws IOException {
        // metricsList is already validated in the controller, so we can skip the validation here

        initializeReportFiles();
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

        Path ndjsonPath = Paths.get(outputDirectory, ndjsonFilename);
        Path statsPath = Paths.get(outputDirectory, statsFilename);
        Path fullJsonPath = Paths.get(outputDirectory, fullJsonFilename);

        // Start a fresh run: remove leftover report files from a previous run (e.g., when reports are volume-mounted).
        deleteReportFileIfExists(ndjsonPath);
        deleteReportFileIfExists(statsPath);
        deleteReportFileIfExists(fullJsonPath);

        // Create NDJSON file placeholder
        Files.createFile(ndjsonPath);
        log.info("Created NDJSON report file: {}", ndjsonPath.toAbsolutePath());

        filesInitialized = true;
    }

    private void deleteReportFileIfExists(Path path) {
        try {
            if (Files.deleteIfExists(path)) {
                log.info("Deleted previous report file {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("Failed to delete previous report file {}: {}", path.toAbsolutePath(), e.getMessage());
        }
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
        return resolveReportPath(statsFilename);
    }

    /**
     * Returns the absolute path to the NDJSON report file.
     */
    public Path getNdjsonReportPath() {
        return resolveReportPath(ndjsonFilename);
    }

    /**
     * Returns the absolute path to the full JSON report file containing all query results.
     */
    public Path getFullJsonReportPath() {
        return resolveReportPath(fullJsonFilename);
    }

    private Path resolveReportPath(String fileName) {
        return Paths.get(outputDirectory, fileName).toAbsolutePath();
    }

    /**
     * Finalizes reports by writing the aggregated statistics JSON and building the full query results JSON
     * without loading all query results into memory.
     */
    public synchronized StatisticsDto finalizeReports(Set<String> loadGeneratorInstances) throws IOException {
        initializeReportFiles();

        StatisticsDto statistics = stats.toStatistics(LocalDateTime.now(), loadGeneratorInstances);

        Path statsPath = Paths.get(outputDirectory, statsFilename);
        objectMapper.writeValue(statsPath.toFile(), statistics);
        Path ndjsonPath = Paths.get(outputDirectory, ndjsonFilename);
        Path fullJsonPath = Paths.get(outputDirectory, fullJsonFilename);
        writeFullJsonReport(ndjsonPath, fullJsonPath);
        //deleteNdjsonFile(ndjsonPath);

        log.info("Statistics written: queries={}, errors={}, instances={}", statistics.getTotalQueries(), statistics.getTotalErrors(), statistics.getLoadGeneratorInstances().size());
        log.info("Request duration stats: avg={}ms min={}ms max={}ms | Query duration stats: avg={}ms min={}ms max={}ms",
                String.format("%.2f", statistics.getRequestDurationMs().getAverage()),
                statistics.getRequestDurationMs().getMin(),
                statistics.getRequestDurationMs().getMax(),
                String.format("%.2f", statistics.getQueryDurationMs().getAverage()),
                statistics.getQueryDurationMs().getMin(),
                statistics.getQueryDurationMs().getMax());

        return statistics;
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

    private void deleteNdjsonFile(Path ndjsonPath) {
        try {
            if (Files.deleteIfExists(ndjsonPath)) {
                log.info("Deleted temporary NDJSON file {}", ndjsonPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("Failed to delete temporary NDJSON file {}: {}", ndjsonPath.toAbsolutePath(), e.getMessage());
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

                if (result.getHttpStatusCode() >= 400) {
                    totalErrors++;
                }

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

        StatisticsDto toStatistics(LocalDateTime generatedAt, Set<String> loadGeneratorInstances) {
            StatisticsDto.DurationStats requestDuration = new StatisticsDto.DurationStats();
            if (requestDurationCount > 0) {
                requestDuration.setAverage(requestDurationSum / (double) requestDurationCount);
                requestDuration.setMin(requestDurationMin);
                requestDuration.setMax(requestDurationMax);
            } else {
                requestDuration.setAverage(0.0);
                requestDuration.setMin(0L);
                requestDuration.setMax(0L);
            }

            StatisticsDto.DurationStats queryDuration = new StatisticsDto.DurationStats();
            if (queryDurationCount > 0) {
                queryDuration.setAverage(queryDurationSum / (double) queryDurationCount);
                queryDuration.setMin(queryDurationMin);
                queryDuration.setMax(queryDurationMax);
            } else {
                queryDuration.setAverage(0.0);
                queryDuration.setMin(0L);
                queryDuration.setMax(0L);
            }

            return new StatisticsDto(
                    generatedAt,
                    requestDuration,
                    queryDuration,
                    totalQueries,
                    totalErrors,
                    new ArrayList<>(loadGeneratorInstances)
            );
        }
    }
}
