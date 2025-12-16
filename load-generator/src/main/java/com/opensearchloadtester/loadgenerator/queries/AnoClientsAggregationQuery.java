package com.opensearchloadtester.loadgenerator.queries;

import java.util.HashMap;
import java.util.Map;

public class AnoClientsAggregationQuery extends AbstractQuery {

    private AnoClientsAggregationQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoClientsAggregationQuery random() {
        return new AnoClientsAggregationQuery(new HashMap<>(), "queries/q5_ano_clients_aggregation.json");
    }
}
