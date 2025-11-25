package com.opensearchloadtester.metricsreporter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
    "load_generator_instances",
    "query_results"
    
})
public class TestRunReport {
    
    /**
     * Aggregated statistics for all query results
     */
    @JsonProperty("statistics")
    private Statistics statistics;
    
    /**
     * Timestamp when the report was generated
     */
    @JsonProperty("report_generated_at")
    private LocalDateTime reportGeneratedAt;
    
    /**
     * Total number of queries executed in this test run
     */
    @JsonProperty("total_queries")
    private Integer totalQueries;
    
    /**
     * Number of queries that resulted in errors
     */
    @JsonProperty("total_errors")
    private Integer totalErrors;
    
    /**
     * List of all query results from all load generator instances
     */
    @JsonProperty("query_results")
    private List<QueryResult> queryResults;
    
    /**
     * List of unique load generator instances that contributed to this report
     */
    @JsonProperty("load_generator_instances")
    private List<String> loadGeneratorInstances;
    
    /**
     * Statistics class containing aggregated metrics
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Statistics {
        @JsonProperty("roundtrip_ms")
        private RoundtripStats roundtripMs;
        
        @JsonProperty("opensearch_took_ms")
        private OpenSearchTookStats opensearchTookMs;
    }
    
    /**
     * Statistics for roundtrip times
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoundtripStats {
        @JsonProperty("average")
        private Double average;
        
        @JsonProperty("min")
        private Long min;
        
        @JsonProperty("max")
        private Long max;
    }
    
    /**
     * Statistics for OpenSearch took times
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpenSearchTookStats {
        @JsonProperty("average")
        private Double average;
        
        @JsonProperty("min")
        private Long min;
        
        @JsonProperty("max")
        private Long max;
    }
}

