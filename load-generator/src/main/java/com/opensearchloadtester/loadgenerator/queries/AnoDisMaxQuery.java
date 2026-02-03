package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class AnoDisMaxQuery extends AbstractQuery {

    private AnoDisMaxQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoDisMaxQuery random() {
        List<String> payrollTypes = List.of("Monthly", "Yearly", "Quarterly");
        String payrollType = payrollTypes.get(faker().random().nextInt(payrollTypes.size()));

        List<String> languages = List.of("German", "English", "Spanish", "French");
        String language = languages.get(faker().random().nextInt(languages.size()));

        List<String> searchTerms = List.of(
                "Brutto-Netto-Abrechnung",
                "Abrechnung",
                "Brutto",
                "Netto",
                "Lohn"
        );
        String searchTerm = searchTerms.get(faker().random().nextInt(searchTerms.size()));

        Map<String, String> queryParams = Map.of(
                "payroll_type", payrollType,
                "language", language,
                "search_terms", searchTerm
        );

        return new AnoDisMaxQuery(queryParams, "queries/compound/q3_ano_dis_max.json");
    }
}
