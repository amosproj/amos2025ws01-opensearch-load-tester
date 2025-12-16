package com.opensearchloadtester.loadgenerator.queries;

import com.opensearchloadtester.common.utils.TimeFormatter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class DuoComplexQuery extends AbstractQuery {

    private DuoComplexQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoComplexQuery random() {
        String searchTerms = faker().random().nextDouble() < 0.8
                ? "Rechnung"
                : faker().options().option("Nettosumme", "Gesamtbetrag", "USt-IdNr.", "Datum");

        String suffix = faker().random().nextDouble() < 0.6
                ? "GmbH"
                : faker().options().option("AG", "KG", "OHG", "GbR", "UG", "e.K.");
        String businessPartnerWildcard = "*" + suffix + "*";

        String invoiceNumberFragment = String.valueOf(faker().number().numberBetween(1, 99999));

        LocalDate startDate = LocalDate.now().minusDays(faker().number().numberBetween(365, 3650));
        LocalDate endDate = startDate.plusDays(faker().number().numberBetween(30, 365));
        if (endDate.isAfter(LocalDate.now())) {
            endDate = LocalDate.now();
        }

        List<String> categories = new ArrayList<>(List.of("SALES_INVOICE", "SUPPLIER_INVOICE", "OTHER"));
        Collections.shuffle(categories, ThreadLocalRandom.current());

        Map<String, String> queryParams = Map.of(
                "search_terms", searchTerms,
                "invoice_date_from", startDate.format(TimeFormatter.ISO_LOCAL_DATE_FORMATTER),
                "invoice_date_to", endDate.format(TimeFormatter.ISO_LOCAL_DATE_FORMATTER),
                "business_partner_wildcard", businessPartnerWildcard,
                "invoice_number_fragment", invoiceNumberFragment,
                "category_1", categories.get(0),
                "category_2", categories.get(1),
                "category_3", categories.get(2)
        );

        return new DuoComplexQuery(queryParams, "queries/complex/q1_duo_complex.json");
    }
}
