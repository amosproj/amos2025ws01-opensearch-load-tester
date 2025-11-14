package com.opensearchloadtester.loadgenerator.controller;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
/**
 * DTO for the /api/load-test/run request body.
 *
 * Represents one logical "query run" request coming from the client:
 * - which queryId to execute
 * - how many threads
 * - how many iterations per thread
 * - which OpenSearch index
 * - parameter map to fill into the JSON template
 */
@Setter
@Getter
public class QueryRunRequest {

    private String queryId;
    private int threads;
    private int iterations;
    private String indexName;

    private Map<String, String> params;

    @Override
    public String toString() {
        return "QueryRunRequest{" +
                "queryId='" + queryId + '\'' +
                ", threads=" + threads +
                ", iterations=" + iterations +
                ", indexName='" + indexName + '\'' +
                ", params=" + params +
                '}';
    }
}

