package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class AnoMultiRegexQuery extends AbstractQuery {

    private AnoMultiRegexQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoMultiRegexQuery random() {
        String year = getRandomYear();

        Map<String, String> queryParams = Map.of(
                "year", year
        );

        return new AnoMultiRegexQuery(queryParams, "query-templates/leaf/q2_ano_multi_regex.json");
    }
}
