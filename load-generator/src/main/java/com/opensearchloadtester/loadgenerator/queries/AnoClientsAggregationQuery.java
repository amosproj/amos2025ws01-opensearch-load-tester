package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class AnoClientsAggregationQuery extends AbstractQuery {

    private AnoClientsAggregationQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoClientsAggregationQuery random() {
        int startYear = faker().number().numberBetween(2015, 2025);
        String minYear = String.valueOf(startYear);
        String maxYear = String.valueOf(faker().number().numberBetween(startYear, 2025));

        Map<String, String> queryParams = Map.of(
                "min_year", minYear,
                "max_year", maxYear
        );

        return new AnoClientsAggregationQuery(queryParams, "queries/q5_ano_clients_aggregation.json");
    }
}
