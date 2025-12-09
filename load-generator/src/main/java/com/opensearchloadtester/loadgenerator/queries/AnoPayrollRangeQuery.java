package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class AnoPayrollRangeQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/q1_ano_payroll_range.json";

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        int startYear = FAKER.number().numberBetween(2015, 2025);
        String fromYear = String.valueOf(startYear);
        String toYear = String.valueOf(FAKER.number().numberBetween(startYear, 2025));

        Map<String, String> queryParams = Map.of(
                "from_year", fromYear,
                "to_year", toYear
        );
        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}
