package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoMultiRegexQuery extends AbstractQuery {

    private DuoMultiRegexQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoMultiRegexQuery random() {
        Map<String, String> queryParams = Map.of();
        return new DuoMultiRegexQuery(queryParams, "queries/leaf/q3_duo_multi_regex.json");
    }
}
