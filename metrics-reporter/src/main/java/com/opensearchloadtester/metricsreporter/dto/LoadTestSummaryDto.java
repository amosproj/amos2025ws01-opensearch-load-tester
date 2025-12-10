package com.opensearchloadtester.metricsreporter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.opensearchloadtester.common.dto.MetricsDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a complete test run report containing all query results.
 * This is the main report structure that can be exported to JSON or CSV.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({
        "report_generated_at",
        "statistics",
        "total_queries",
        "total_errors",
        "query_results",
        "load_generator_instances"
})
public class LoadTestSummaryDto {

    @JsonProperty("statistics")
    private Statistics statistics;

    @JsonProperty("report_generated_at")
    private LocalDateTime reportGeneratedAt;

    @JsonProperty("total_queries")
    private Integer totalQueries;

    @JsonProperty("total_errors")
    private Integer totalErrors;

    @JsonProperty("query_results")
    private List<MetricsDto> queryResults;

    @JsonProperty("load_generator_instances")
    private List<String> loadGeneratorInstances;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        @JsonProperty("request_duration_ms")
        private DurationStats requestDurationMs;

        @JsonProperty("query_duration_ms")
        private DurationStats queryDurationMs;
    }

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
