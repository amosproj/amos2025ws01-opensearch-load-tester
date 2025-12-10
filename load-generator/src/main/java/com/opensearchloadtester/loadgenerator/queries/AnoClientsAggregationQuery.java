package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class AnoClientsAggregationQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/q5_ano_clients_aggregation.json";

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        int startYear = FAKER.number().numberBetween(2015, 2025);
        String minYear = String.valueOf(startYear);
        String maxYear = String.valueOf(FAKER.number().numberBetween(startYear, 2025));

        Map<String, String> queryParams = Map.of(
                "min_year", minYear,
                "max_year", maxYear
        );
        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}
