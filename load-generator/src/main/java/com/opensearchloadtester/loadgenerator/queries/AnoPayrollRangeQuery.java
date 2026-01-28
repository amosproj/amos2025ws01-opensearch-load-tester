package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class AnoPayrollRangeQuery extends AbstractQuery {

    private AnoPayrollRangeQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoPayrollRangeQuery random() {
        String fromYear = getRandomYear();
        String toYear = getRandomYearAfter(fromYear);

        Map<String, String> queryParams = Map.of(
                "from_year", fromYear,
                "to_year", toYear
        );

        return new AnoPayrollRangeQuery(queryParams, "query-templates/q1_ano_payroll_range.json");
    }
}
