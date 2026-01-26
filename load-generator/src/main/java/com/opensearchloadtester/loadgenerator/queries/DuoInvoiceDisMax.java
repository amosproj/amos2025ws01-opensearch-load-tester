package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class DuoInvoiceDisMax extends AbstractQuery {

    private DuoInvoiceDisMax(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoInvoiceDisMax random() {
        String invoiceNumber = String.format(
                "%d/%d",
                faker().number().numberBetween(10000, 99999),
                faker().number().numberBetween(1, 9999)
        );

        String businessPartner = faker().company().name();

        List<String> termOptions = List.of("Rechnung", "Nettosumme", "Gesamtbetrag", "USt-IdNr.", "Bankverbindung");
        String searchTerms = faker().random().nextDouble() < 0.8
                ? "Rechnung"
                : termOptions.get(faker().random().nextInt(termOptions.size()));

        Map<String, String> queryParams = Map.of(
                "invoice_number", invoiceNumber,
                "business_partner", businessPartner,
                "search_terms", searchTerms
        );

        return new DuoInvoiceDisMax(queryParams, "queries/compound/q1_duo_dis_max.json");
    }
}
