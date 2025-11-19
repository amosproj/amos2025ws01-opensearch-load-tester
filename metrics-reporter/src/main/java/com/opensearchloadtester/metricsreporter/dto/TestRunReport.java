package com.opensearchloadtester.metricsreporter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class TestRunReport {
    
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
}

