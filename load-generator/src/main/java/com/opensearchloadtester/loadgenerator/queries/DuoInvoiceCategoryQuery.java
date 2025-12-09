package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class DuoInvoiceCategoryQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/q2_duo_invoice_category.json";

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        List<String> documentCategoryTypes = List.of("SUPPLIER_INVOICE", "OTHER", "SALES_INVOICE");
        String documentCategory = documentCategoryTypes.get(RANDOM.nextInt(documentCategoryTypes.size()));

        Map<String, String> queryParams = Map.of(
                "category", documentCategory
        );

        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}




