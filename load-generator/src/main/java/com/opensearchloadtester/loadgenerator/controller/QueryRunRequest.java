package com.opensearchloadtester.loadgenerator.controller;

import com.opensearchloadtester.loadgenerator.model.QueryType;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO for the /api/load-test/run request body.
 * <p>
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
    private QueryType queryType;
    private int threads;
    private int iterations;
    private String indexName;

    private Map<String, String> params;

    @Override
    public String toString() {
        return "QueryRunRequest{" +
                "queryId='" + queryId + '\'' +
                ", queryType='" + queryType.name() + '\'' +
                ", threads=" + threads +
                ", iterations=" + iterations +
                ", indexName='" + indexName + '\'' +
                ", params=" + params +
                '}';
    }
}

