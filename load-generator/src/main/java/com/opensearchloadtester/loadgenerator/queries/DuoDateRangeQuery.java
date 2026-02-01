package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoDateRangeQuery extends AbstractQuery {

    private DuoDateRangeQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoDateRangeQuery random() {

        int year = faker().random().nextInt(100) < 70 ? 2025 : 2026;

        String from = year + "-01-01T00:00:00Z";
        String to   = year   + "-12-31T23:59:59Z";

        Map<String, String> queryParams = Map.of(
                "gte", from,
                "lte", to
        );

        return new DuoDateRangeQuery(
                queryParams,
                "query-templates/complex/q5_duo_range.json"
        );
    }
}
