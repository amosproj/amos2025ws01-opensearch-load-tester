package com.opensearchloadtester.loadgenerator.queries.span;

import com.opensearchloadtester.loadgenerator.queries.AbstractQuery;

import java.util.Map;

public class DuoSpanNearQuery extends AbstractQuery {

    private DuoSpanNearQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoSpanNearQuery random() {
        String productName = faker().commerce().productName();
        String material = productName.split(" ")[1];
        String product = productName.split(" ")[2];

        Map<String, String> queryParams = Map.of(
                "material", material,
                "product", product
        );

        return new DuoSpanNearQuery(queryParams, "query-templates/span/duo_span_near.json");
    }
}
