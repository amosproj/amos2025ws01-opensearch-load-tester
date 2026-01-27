package com.opensearchloadtester.metricsreporter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents aggregated statistics for a load test run.
 * Kept separate from the full summary to match the dedicated statistics file.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({
        "report_generated_at",
        "request_duration_ms",
        "query_duration_ms",
        "total_queries",
        "total_errors",
        "load_generator_instances"
})
public class StatisticsDto {

    @JsonProperty("report_generated_at")
    private LocalDateTime reportGeneratedAt;

    @JsonProperty("request_duration_ms")
    private DurationStats requestDurationMs;

    @JsonProperty("query_duration_ms")
    private DurationStats queryDurationMs;

    @JsonProperty("total_queries")
    private Long totalQueries;

    @JsonProperty("total_errors")
    private Long totalErrors;

    @JsonProperty("load_generator_instances")
    private List<String> loadGeneratorInstances;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DurationStats {
        @JsonProperty("average")
        private Double average;

        @JsonProperty("min")
        private Long min;

        @JsonProperty("max")
        private Long max;
    }
}
