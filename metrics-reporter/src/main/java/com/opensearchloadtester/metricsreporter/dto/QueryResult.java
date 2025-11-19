package com.opensearchloadtester.metricsreporter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single query execution result from a test run.
 * This DTO captures all relevant information from an OpenSearch query execution
 * including timing, request type, and the complete raw response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QueryResult {
    
    /**
     * The type/name of the query that was executed (e.g., "q1_ano_payroll_range")
     */
    @JsonProperty("request_type")
    private String requestType;
    
    /**
     * Round-trip time in milliseconds from sending the request to receiving the response
     */
    @JsonProperty("roundtrip_ms")
    private Long roundtripMs;
    
    /**
     * The complete raw JSON response from OpenSearch (includes took, hits, score, errors, etc.)
     */
    @JsonProperty("json_response")
    private String jsonResponse;
    
    /**
     * Indicates whether this query resulted in an error (derived from response)
     */
    @JsonProperty("has_error")
    private Boolean hasError;
    
    /**
     * The load generator instance that executed this query
     */
    @JsonProperty("load_generator_instance")
    private String loadGeneratorInstance;
}

