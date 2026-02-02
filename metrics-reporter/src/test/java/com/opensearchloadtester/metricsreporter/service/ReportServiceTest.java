package com.opensearchloadtester.metricsreporter.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.dto.StatisticsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReportServiceTest {

    private static final String LOAD_GENERATOR_ID = "lg-1";

    @TempDir
    Path tempDir;

    private ReportService reportService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reportService = new ReportService();
        ReflectionTestUtils.setField(reportService, "outputDirectory", tempDir.toString());
        ReflectionTestUtils.setField(reportService, "statsFilename", "statistics.json");
        ReflectionTestUtils.setField(reportService, "ndjsonFilename", "tmp_query_results.ndjson");
        ReflectionTestUtils.setField(reportService, "resultsJsonFilename", "query_results.json");

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void processMetrics_and_finalizeReports_writeOutputsAndStats() throws Exception {
        List<MetricsDto> metrics = List.of(
                new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 100L, 50L, 10, 200),
                new MetricsDto(LOAD_GENERATOR_ID, "query_type_test", 300L, 150L, 5, 500)
        );

        reportService.processMetrics(metrics);

        Path ndjsonPath = tempDir.resolve("tmp_query_results.ndjson");

        assertThat(Files.exists(ndjsonPath)).isTrue();

        List<String> ndjsonLines = Files.readAllLines(ndjsonPath);

        assertThat(ndjsonLines).hasSize(2);
        assertThat(ndjsonLines.get(0)).isNotBlank();
        assertThat(ndjsonLines.get(1)).isNotBlank();

        JsonNode ndjsonEntry1 = objectMapper.readTree(ndjsonLines.get(0));
        JsonNode ndjsonEntry2 = objectMapper.readTree(ndjsonLines.get(1));

        assertThat(ndjsonEntry1.get("load_generator_id").asText()).isEqualTo(LOAD_GENERATOR_ID);
        assertThat(ndjsonEntry1.get("query_type").asText()).isEqualTo("query_type_test");
        assertThat(ndjsonEntry1.get("request_duration_millis").asLong()).isEqualTo(100L);
        assertThat(ndjsonEntry1.get("query_duration_millis").asLong()).isEqualTo(50L);
        assertThat(ndjsonEntry1.get("total_hits").asInt()).isEqualTo(10);
        assertThat(ndjsonEntry1.get("http_status_code").asInt()).isEqualTo(200);

        assertThat(ndjsonEntry2.get("load_generator_id").asText()).isEqualTo(LOAD_GENERATOR_ID);
        assertThat(ndjsonEntry2.get("query_type").asText()).isEqualTo("query_type_test");
        assertThat(ndjsonEntry2.get("request_duration_millis").asLong()).isEqualTo(300L);
        assertThat(ndjsonEntry2.get("query_duration_millis").asLong()).isEqualTo(150L);
        assertThat(ndjsonEntry2.get("total_hits").asInt()).isEqualTo(5);
        assertThat(ndjsonEntry2.get("http_status_code").asInt()).isEqualTo(500);

        StatisticsDto statistics = reportService.finalizeReports(Set.of(LOAD_GENERATOR_ID));

        Path statsPath = tempDir.resolve("statistics.json");
        Path resultsJsonPath = tempDir.resolve("query_results.json");

        assertThat(Files.exists(statsPath)).isTrue();
        assertThat(Files.exists(resultsJsonPath)).isTrue();

        StatisticsDto writtenStats = objectMapper.readValue(statsPath.toFile(), StatisticsDto.class);
        assertThat(writtenStats.getTotalQueries()).isEqualTo(2L);
        assertThat(writtenStats.getTotalErrors()).isEqualTo(1L);
        assertThat(writtenStats.getLoadGeneratorInstances()).containsExactly(LOAD_GENERATOR_ID);

        assertThat(writtenStats.getRequestDurationMs().getAverage()).isEqualTo(200.0);
        assertThat(writtenStats.getRequestDurationMs().getMin()).isEqualTo(100L);
        assertThat(writtenStats.getRequestDurationMs().getMax()).isEqualTo(300L);

        assertThat(writtenStats.getQueryDurationMs().getAverage()).isEqualTo(100.0);
        assertThat(writtenStats.getQueryDurationMs().getMin()).isEqualTo(50L);
        assertThat(writtenStats.getQueryDurationMs().getMax()).isEqualTo(150L);

        assertThat(statistics.getTotalQueries()).isEqualTo(2L);
        assertThat(statistics.getTotalErrors()).isEqualTo(1L);

        JsonNode resultsJson = objectMapper.readTree(resultsJsonPath.toFile());
        assertThat(resultsJson.isArray()).isTrue();
        assertThat(resultsJson.size()).isEqualTo(2);
        assertThat(resultsJson.get(0)).isEqualTo(ndjsonEntry1);
        assertThat(resultsJson.get(1)).isEqualTo(ndjsonEntry2);
    }
}
