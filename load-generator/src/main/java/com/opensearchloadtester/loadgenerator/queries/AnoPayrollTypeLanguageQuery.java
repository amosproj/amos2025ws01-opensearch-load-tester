package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class AnoPayrollTypeLanguageQuery extends AbstractQuery {

    private AnoPayrollTypeLanguageQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoPayrollTypeLanguageQuery random() {
        List<String> type = List.of("Monthly", "Yearly", "Quarterly");
        String payrollType = type.get(faker().number().numberBetween(0, 3));

        List<String> lang = List.of("German", "English", "Spanish", "French");
        String language = lang.get(faker().number().numberBetween(0, 3));

        int startYear = faker().number().numberBetween(2015, 2025);
        String minYear = String.valueOf(startYear);
        String maxYear = String.valueOf(faker().number().numberBetween(startYear, 2025));

        Map<String, String> queryParams = Map.of(
                "payroll_type", payrollType,
                "language", language,
                "min_year", minYear,
                "max_year", maxYear
        );

        return new AnoPayrollTypeLanguageQuery(queryParams,
                "queries/q9_ano_payroll_type_language.json");
    }
}
