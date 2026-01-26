package com.opensearchloadtester.loadgenerator.queries;

import com.opensearchloadtester.common.utils.TimeFormatter;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class DuoInvoiceDisMaxExpensive extends AbstractQuery {

    private DuoInvoiceDisMaxExpensive(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoInvoiceDisMaxExpensive random() {
        String invoiceNumber = String.format(
                "%d/%d",
                faker().number().numberBetween(10000, 99999),
                faker().number().numberBetween(1, 9999)
        );

        String businessPartner = faker().company().name();

        List<String> termOptions = List.of("Wolle", "Leder", "Stahl", "Kupfer", "Marmor", "Gummi", "Papier", "Service");
        String searchTerms = termOptions.get(faker().random().nextInt(termOptions.size()));

        LocalDate startDate = LocalDate.now().minusDays(faker().number().numberBetween(30, 3650));
        LocalDate endDate = startDate.plusDays(faker().number().numberBetween(30, 180));
        if (endDate.isAfter(LocalDate.now())) {
            endDate = LocalDate.now();
        }

        List<String> categories = List.of("SALES_INVOICE", "SUPPLIER_INVOICE", "OTHER");
        String category = categories.get(faker().random().nextInt(categories.size()));

        List<String> paidStatuses = List.of("FULLY_PAID", "NOT_PAID");
        String paidStatus = paidStatuses.get(faker().random().nextInt(paidStatuses.size()));

        Map<String, String> queryParams = Map.of(
                "invoice_number", invoiceNumber,
                "business_partner", businessPartner,
                "search_terms", searchTerms,
                "invoice_date_from", startDate.format(TimeFormatter.ISO_LOCAL_DATE_FORMATTER),
                "invoice_date_to", endDate.format(TimeFormatter.ISO_LOCAL_DATE_FORMATTER),
                "category", category,
                "paid_status", paidStatus
        );

        return new DuoInvoiceDisMaxExpensive(queryParams, "queries/compound/q2_duo_dis_max.json");
    }
}
