package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class AnoClientByYearQuery extends AbstractQuery {

    private AnoClientByYearQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoClientByYearQuery random() {
        String clientName = faker().name().fullName();
        String year = String.valueOf(faker().number().numberBetween(2015, 2025));

        Map<String, String> queryParams = Map.of(
                "client_name", clientName,
                "year", year
        );

        return new AnoClientByYearQuery(queryParams, "queries/q6_ano_client_by_year.json");
    }
}
