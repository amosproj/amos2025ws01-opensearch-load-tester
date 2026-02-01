package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class AnoPayrollTypeLanguageQuery extends AbstractQuery {

    private AnoPayrollTypeLanguageQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoPayrollTypeLanguageQuery random() {
        List<String> type = List.of("Monthly", "Yearly", "Quarterly");
        String payrollType = type.get(faker().random().nextInt(type.size()));

        List<String> lang = List.of("German", "English", "Spanish", "French");
        String language = lang.get(faker().random().nextInt(type.size()));

        String year_from = getRandomYear();
        String year_to = getRandomYearAfter(year_from);

        Map<String, String> queryParams = Map.of(
                "payroll_type", payrollType,
                "language", language,
                "year_from", year_from,
                "year_to", year_to
        );

        return new AnoPayrollTypeLanguageQuery(queryParams,
                "query-templates/q9_ano_payroll_type_language.json");
    }
}
