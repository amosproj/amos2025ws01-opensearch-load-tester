package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class AnoClientByYearQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/q6_ano_client_by_year.json";

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        String clientName = FAKER.name().fullName();
        String year = String.valueOf(FAKER.number().numberBetween(2015, 2025));

        Map<String, String> queryParams = Map.of(
                "client_name", clientName,
                "year", year
        );
        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}
