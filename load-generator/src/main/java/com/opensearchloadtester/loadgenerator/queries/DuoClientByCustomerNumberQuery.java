package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoClientByCustomerNumberQuery extends AbstractQuery {

    private DuoClientByCustomerNumberQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoClientByCustomerNumberQuery random() {
        String customerNumber = String.valueOf(faker().random().nextDouble() < 0.7
                ? faker().number().numberBetween(1000, 1000000)
                : null);

        Map<String, String> queryParams = Map.of(
                "customer_number", customerNumber
        );

        return new DuoClientByCustomerNumberQuery(queryParams,
                "query-templates/q7_duo_client_by_customer_number.json");
    }
}
