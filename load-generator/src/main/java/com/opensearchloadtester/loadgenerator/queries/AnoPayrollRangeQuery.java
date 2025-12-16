package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class AnoPayrollRangeQuery extends AbstractQuery {

    private AnoPayrollRangeQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoPayrollRangeQuery random() {
        int startYear = faker().number().numberBetween(2015, 2026);
        String fromYear = String.valueOf(startYear);
        String toYear = String.valueOf(faker().number().numberBetween(startYear, 2026));

        Map<String, String> queryParams = Map.of(
                "from_year", fromYear,
                "to_year", toYear
        );

        return new AnoPayrollRangeQuery(queryParams, "queries/q1_ano_payroll_range.json");
    }
}
