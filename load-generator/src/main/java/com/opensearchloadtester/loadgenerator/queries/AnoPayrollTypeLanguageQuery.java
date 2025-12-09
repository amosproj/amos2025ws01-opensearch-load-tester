package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class AnoPayrollTypeLanguageQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/q9_ano_payroll_type_language.json";

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        List<String> type = List.of("Monthly", "Yearly", "Quarterly");
        String payrollType = type.get(RANDOM.nextInt(type.size()));

        List<String> lang = List.of("German", "English", "Spanish", "French");
        String language = lang.get(RANDOM.nextInt(lang.size()));

        int startYear = FAKER.number().numberBetween(2015, 2025);
        String minYear = String.valueOf(startYear);
        String maxYear = String.valueOf(FAKER.number().numberBetween(startYear, 2025));

        Map<String, String> queryParams = Map.of(
                "payroll_type", payrollType,
                "language", language,
                "min_year", minYear,
                "max_year", maxYear
        );
        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}
