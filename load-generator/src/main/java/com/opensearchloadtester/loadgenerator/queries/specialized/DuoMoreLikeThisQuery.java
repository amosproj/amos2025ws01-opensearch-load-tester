package com.opensearchloadtester.loadgenerator.queries.specialized;

import com.opensearchloadtester.loadgenerator.queries.AbstractQuery;

import java.util.Map;

public class DuoMoreLikeThisQuery extends AbstractQuery {

    private DuoMoreLikeThisQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoMoreLikeThisQuery random() {
        String productName = faker().commerce().productName();
        String material = productName.split(" ")[1];
        String product = productName.split(" ")[2];

        Map<String, String> queryParams = Map.of(
                "material", material,
                "product", product
        );

        return new DuoMoreLikeThisQuery(queryParams, "query-templates/specialized/duo_more_like_this.json");
    }
}
