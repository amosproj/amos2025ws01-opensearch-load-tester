package com.opensearchloadtester.loadgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;

import java.util.ArrayList;
import java.util.List;

public final class QueryPoolBuilder {

    private static final int MAX_POOL_SIZE = 10_000;

    private QueryPoolBuilder() {}

    public static List<QueryType> build(ScenarioConfig config) {
        JsonNode mix = config.getQueryMix();

        if (mix == null || mix.isNull() || (mix.isArray() && mix.isEmpty())) {
            throw new IllegalArgumentException(
                    "query_mix must be defined and contain at least one query type. " +
                            "Use short form (e.g. query_mix: [TYPE_A, TYPE_B]) for uniform distribution."
            );
        }

        if (!mix.isArray()) {
            throw new IllegalArgumentException("query_mix must be a YAML list");
        }

        int totalWeight = 0;
        for (JsonNode entry : mix) {
            int weight;

            if (entry.isTextual()) {
                weight = 1;
            } else if (entry.isObject()) {
                JsonNode percentNode = entry.get("percent");
                if (percentNode == null || !percentNode.canConvertToInt()) {
                    throw new IllegalArgumentException("query_mix entry missing integer field 'percent'");
                }
                weight = percentNode.asInt();
                if (weight <= 0) {
                    throw new IllegalArgumentException("query_mix weights must be > 0");
                }
            } else {
                throw new IllegalArgumentException("Invalid query_mix entry: " + entry);
            }

            totalWeight += weight;
            if (totalWeight > MAX_POOL_SIZE) {
                throw new IllegalArgumentException(
                        "query_mix weights sum too large (" + totalWeight + "). " +
                                "Please use smaller ratios (e.g. 2:5:3) or percentages that sum to ~100."
                );
            }
        }

        var pool = new ArrayList<QueryType>(totalWeight);

        for (JsonNode entry : mix) {
            if (entry.isTextual()) {
                QueryType type = QueryType.valueOf(entry.asText());
                pool.add(type);
                continue;
            }

            JsonNode typeNode = entry.get("type");
            JsonNode percentNode = entry.get("percent");

            if (typeNode == null || !typeNode.isTextual()) {
                throw new IllegalArgumentException("query_mix entry missing string field 'type'");
            }
            if (percentNode == null || !percentNode.canConvertToInt()) {
                throw new IllegalArgumentException("query_mix entry missing integer field 'percent'");
            }

            QueryType type = QueryType.valueOf(typeNode.asText());
            int weight = percentNode.asInt();

            for (int i = 0; i < weight; i++) {
                pool.add(type);
            }
        }

        return pool;
    }
}
