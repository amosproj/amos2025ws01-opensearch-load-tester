package com.opensearchloadtester.metricsreporter.dto;

import lombok.Data;

@Data
public class Metrics {

    /**
     * requestType       Type of query that was executed
     * roundtripMilSec   Time from first request to final answer
     * jsonResponse      Response of query execution
     */
    private String requestType;
    private int roundtripMilSec;
    private String jsonResponse;

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public int getRoundtripMilSec() {
        return roundtripMilSec;
    }

    public void setRoundtripMilSec(int roundtripMilSec) {
        this.roundtripMilSec = roundtripMilSec;
    }

    public String getJsonResponse() {
        return jsonResponse;
    }

    public void setJsonResponse(String jsonResponse) {
        this.jsonResponse = jsonResponse;
    }
}
