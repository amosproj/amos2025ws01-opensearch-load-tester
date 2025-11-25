package com.opensearchloadtester.metricsreporter.dto;

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


    // TODO: check if roundtripMilSec == 0 is 100% a non valid value.
    public void addMetrics(String requestType, long roundtripMilSec, String jsonResponse) {
        if (requestType == null || roundtripMilSec == 0 || jsonResponse == null) {
            log.error("Error when adding Metrics requestType: " +
                    "{}, roundtripMilSec: " +
                    "{}, jsonResponse: " +
                    "{}", requestType, roundtripMilSec, jsonResponse);
            return;
        }
        this.requestType.add(requestType);
        this.roundtripMilSec.add(roundtripMilSec);
        this.jsonResponse.add(jsonResponse);
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
