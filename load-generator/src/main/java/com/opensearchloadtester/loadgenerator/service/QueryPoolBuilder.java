package com.opensearchloadtester.loadgenerator.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class QueryPoolBuilder {

    public static List<QueryType> build(ScenarioConfig config) {
        Objects.requireNonNull(config, "ScenarioConfig must not be null");
        JsonNode mix = config.getQueryMix();

        ArrayList<QueryType> pool = new ArrayList<>();

        for (JsonNode entry : mix) {
            if (entry.isTextual()) {
                pool.add(QueryType.valueOf(entry.asText()));
            } else {
                JsonNode typeNode = entry.get("type");
                JsonNode percentNode = entry.get("percent");

                QueryType type = QueryType.valueOf(typeNode.asText());
                int weight = percentNode.asInt();
                for (int i = 0; i < weight; i++) {
                    pool.add(type);
                }
            }
        }

        return pool;
    }
}
