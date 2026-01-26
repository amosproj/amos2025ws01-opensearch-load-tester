package com.opensearchloadtester.loadgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class QueryPoolBuilder {

    private QueryPoolBuilder() {}

    public static List<QueryType> build(ScenarioConfig config) {
        Objects.requireNonNull(config, "ScenarioConfig must not be null");
        JsonNode mix = config.getQueryMix();

        if (mix == null || !mix.isArray() || mix.isEmpty()) {
            // TODO: Better exception text?
            throw new IllegalStateException("ScenarioConfig must be validated before building query pool");
        }

        int totalWeight = 0;
        for (JsonNode entry : mix) {
            if (entry.isTextual()) {
                totalWeight++;
            } else {
                JsonNode percent = entry.get("percent");
                if (percent == null) {
                    throw new IllegalStateException("ScenarioConfig must be validated before building query pool");
                }
                totalWeight += percent.asInt();
            }
        }

        var pool = new ArrayList<QueryType>(totalWeight);

        for (JsonNode entry : mix) {
            if (entry.isTextual()) {
                pool.add(QueryType.valueOf(entry.asText()));
            } else {
                JsonNode typeNode = entry.get("type");
                JsonNode percentNode = entry.get("percent");

                if (typeNode == null || percentNode == null) {
                    throw new IllegalStateException("ScenarioConfig must be validated before building query pool");
                }

                QueryType type = QueryType.valueOf(typeNode.asText());
                int weight = percentNode.asInt();
                for (int i = 0; i < weight; i++) {
                    pool.add(type);
                }
            }
        }

        if (pool.size() != totalWeight) {
            throw new IllegalStateException("Internal error building query pool: expected size=" + totalWeight + ", actual=" + pool.size());
        }

        return pool;
    }
}
