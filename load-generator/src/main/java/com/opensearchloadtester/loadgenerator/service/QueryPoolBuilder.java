package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;

import java.util.ArrayList;
import java.util.List;

public final class QueryPoolBuilder {

    private QueryPoolBuilder() {}

    public static List<QueryType> build(ScenarioConfig config) {
        var mix = config.getQueryMix();
        if (mix == null || mix.isEmpty()) {
            var allowed = config.getQueryTypes();
            if (allowed == null || allowed.isEmpty()) {
                throw new IllegalArgumentException("queryTypes must not be empty.");
            }
            return allowed;
        }

        var pool = new ArrayList<QueryType>();
        for (var e : mix) {
            for (int i = 0; i < e.getPercent(); i++) {
                pool.add(e.getType());
            }
        }

        if (pool.isEmpty()) {
            throw new IllegalArgumentException("queryMix must contain at least one positive weight.");
        }

        return pool;
    }
}
