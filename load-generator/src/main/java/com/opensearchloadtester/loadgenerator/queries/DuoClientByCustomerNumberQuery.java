package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoClientByCustomerNumberQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/q7_duo_client_by_customer_number.json";

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        String customerNumber = String.valueOf(FAKER.random().nextDouble() < 0.7
                ? FAKER.number().numberBetween(1000, 1000000)
                : null);

        Map<String, String> queryParams = Map.of(
                "customer_number", customerNumber
        );
        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}
