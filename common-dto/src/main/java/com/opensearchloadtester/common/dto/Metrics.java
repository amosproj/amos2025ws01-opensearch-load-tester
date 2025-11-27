package com.opensearchloadtester.common.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared metrics DTO used by load-generator (producer) and metrics-reporter (consumer).
 * Stores aligned arrays of query metadata and responses; addMetrics is synchronized for thread safety.
 */
@Slf4j
@Data
@NoArgsConstructor
public class Metrics {

    /**
     * loadGeneratorInstance   Name of reporting loadGenerator instance
     * requestType             Array of types of queries that were executed
     * roundtripMilSec         Array of times from first request to final answer
     * jsonResponse            Array of responses of query executions
     */
    private String loadGeneratorInstance;
    private List<String> requestType = new ArrayList<>();
    private List<Long> roundtripMilSec = new ArrayList<>();
    private List<String> jsonResponse = new ArrayList<>();

    public Metrics(String loadGeneratorInstance) {
        this.loadGeneratorInstance = loadGeneratorInstance;
    }

    /**
     * Thread-safe add that keeps the metric arrays in sync.
     *
     * @param requestType     The type/name of the query
     * @param roundtripMilSec Round-trip time in milliseconds (0 is valid)
     * @param jsonResponse    The JSON response from OpenSearch
     */
    public synchronized void addMetrics(String requestType, long roundtripMilSec, String jsonResponse) {
        if (requestType == null || jsonResponse == null) {
            log.error("Error when adding Metrics - null values detected. requestType: {}, jsonResponse: {}, roundtripMilSec: {}",
                    requestType, jsonResponse, roundtripMilSec);
            return;
        }
        this.requestType.add(requestType);
        this.roundtripMilSec.add(roundtripMilSec);
        this.jsonResponse.add(jsonResponse);

        // Guard against misalignment (should not happen with synchronized block)
        int size = this.requestType.size();
        if (size != this.roundtripMilSec.size() || size != this.jsonResponse.size()) {
            log.error("CRITICAL: Metrics arrays out of sync! requestType: {}, roundtripMilSec: {}, jsonResponse: {}",
                    this.requestType.size(), this.roundtripMilSec.size(), this.jsonResponse.size());
        }
    }

    public String getRequestType(int index) {
        return requestType.get(index);
    }

    public long getRoundtripMilSec(int index) {
        return roundtripMilSec.get(index);
    }

    public String getJsonResponse(int index) {
        return jsonResponse.get(index);
    }
}
