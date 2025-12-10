package com.opensearchloadtester.loadgenerator.queries;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DuoComplexQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/complex/q1_duo_complex.json";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        // String invoiceBusinessPartner = FAKER.company().name();
        // String searchTerms = invoiceBusinessPartner + " Rechnung";
        // String businessPartnerWildcard = "*" + invoiceBusinessPartner + "*";
        // Use broad patterns to increase hit probability on generated data
        String searchTerms = "Rechnung";
        // Broad wildcard to avoid filtering out generated partners (keeps leading wildcard cost)
        String businessPartnerWildcard = "*GmbH*";
        String invoiceNumberFragment = String.valueOf(FAKER.number().numberBetween(1, 99999));

        LocalDate startDate = LocalDate.now().minusDays(FAKER.number().numberBetween(365, 3650));
        LocalDate endDate = startDate.plusDays(FAKER.number().numberBetween(30, 365));
        if (endDate.isAfter(LocalDate.now())) {
            endDate = LocalDate.now();
        }

        List<String> categories = new ArrayList<>(List.of("SALES_INVOICE", "SUPPLIER_INVOICE", "OTHER"));
        Collections.shuffle(categories, RANDOM);

        Map<String, String> queryParams = Map.of(
                "search_terms", searchTerms,
                "invoice_date_from", startDate.format(DATE_FORMATTER),
                "invoice_date_to", endDate.format(DATE_FORMATTER),
                "business_partner_wildcard", businessPartnerWildcard,
                "invoice_number_fragment", invoiceNumberFragment,
                "category_1", categories.get(0),
                "category_2", categories.get(1),
                "category_3", categories.get(2)
        );
        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}
