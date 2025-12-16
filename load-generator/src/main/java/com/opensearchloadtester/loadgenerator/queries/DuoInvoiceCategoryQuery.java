package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class DuoInvoiceCategoryQuery extends AbstractQuery {

    private DuoInvoiceCategoryQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoInvoiceCategoryQuery random() {
        List<String> documentCategoryTypes = List.of("SUPPLIER_INVOICE", "OTHER", "SALES_INVOICE");
        String documentCategory = documentCategoryTypes.get(faker().number().numberBetween(0, 2));


        Map<String, String> queryParams = Map.of(
                "category", documentCategory
        );

        return new DuoInvoiceCategoryQuery(queryParams, "queries/q2_duo_invoice_category.json");
    }
}




