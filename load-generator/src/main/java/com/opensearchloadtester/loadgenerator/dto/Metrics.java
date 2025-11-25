package com.opensearchloadtester.loadgenerator.dto;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
@Data
public class Metrics {


    /**
     * loadGeneratorInstance   Name of reporting loadGenerator instance
     * requestType          Array of types of queries that were executed
     * roundtripMilSec      Array of times from first request to final answer
     * jsonResponse         Array of responses of query executions
     */
    private String loadGeneratorInstance;
    private ArrayList<String> requestType = new ArrayList<>();
    private ArrayList<Long> roundtripMilSec = new ArrayList<>();
    private ArrayList<String> jsonResponse = new ArrayList<>();

    public Metrics(String loadGeneratorInstance) {
        this.loadGeneratorInstance = loadGeneratorInstance;
    }

    /**
     * Adds metrics to the collections. This method is synchronized to ensure thread-safety
     * when multiple threads add metrics concurrently.
     *
     * @param requestType The type/name of the query
     * @param roundtripMilSec Round-trip time in milliseconds (0 is a valid value for very fast queries)
     * @param jsonResponse The JSON response from OpenSearch
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
        
        // Log if arrays get out of sync (should never happen with synchronized method)
        if (this.requestType.size() != this.roundtripMilSec.size() || 
            this.requestType.size() != this.jsonResponse.size()) {
            log.error("CRITICAL: Metrics arrays are out of sync! requestType: {}, roundtripMilSec: {}, jsonResponse: {}",
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
