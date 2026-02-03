package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class AnoDisMaxExpensiveQuery extends AbstractQuery {

    private AnoDisMaxExpensiveQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoDisMaxExpensiveQuery random() {
        List<String> payrollTypes = List.of("Monthly", "Yearly", "Quarterly");
        String payrollType = payrollTypes.get(faker().random().nextInt(payrollTypes.size()));

        List<String> languages = List.of("German", "English", "Spanish", "French");
        String language = languages.get(faker().random().nextInt(languages.size()));

        int yearFrom = faker().number().numberBetween(2016, 2026);
        int yearTo = faker().number().numberBetween(yearFrom, 2026);

        int month = faker().number().numberBetween(1, 12);
        String monthName = monthName(month);

        String filename = "Brutto-Netto-Abrechnung " + monthName + " " + yearTo + ".pdf";
        String filenameFragment = monthName;

        String creatorName = faker().name().fullName();
        String docPhrase = "Brutto-Netto-Abrechnung " + monthName + " " + yearTo;

        List<String> searchTerms = List.of(
                "Brutto-Netto-Abrechnung",
                "Abrechnung " + monthName,
                monthName + " " + yearTo
        );
        String searchTerm = searchTerms.get(faker().random().nextInt(searchTerms.size()));

        Map<String, String> queryParams = Map.of(
                "payroll_type", payrollType,
                "language", language,
                "year_from", String.valueOf(yearFrom),
                "year_to", String.valueOf(yearTo),
                "month", String.valueOf(month),
                "filename", filename,
                "filename_fragment", filenameFragment,
                "creator_name", creatorName,
                "doc_phrase", docPhrase,
                "search_terms", searchTerm
        );

        return new AnoDisMaxExpensiveQuery(queryParams, "queries/compound/q4_ano_dis_max.json");
    }

    private static String monthName(int month) {
        return switch (month) {
            case 1 -> "Januar";
            case 2 -> "Februar";
            case 3 -> "März";
            case 4 -> "April";
            case 5 -> "Mai";
            case 6 -> "Juni";
            case 7 -> "Juli";
            case 8 -> "August";
            case 9 -> "September";
            case 10 -> "Oktober";
            case 11 -> "November";
            case 12 -> "Dezember";
            default -> "Januar";
        };
    }
}
