package com.opensearchloadtester.loadgenerator.queries.specialized;

import com.opensearchloadtester.loadgenerator.queries.AbstractQuery;

import java.time.Year;
import java.util.Map;

public class AnoMoreLikeThisQuery extends AbstractQuery {

    private AnoMoreLikeThisQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoMoreLikeThisQuery random() {
        int currentYear = Year.now().getValue();
        String year = String.valueOf(
                faker().number().numberBetween(currentYear - 10, currentYear + 1)
        );

        Map<String, String> queryParams = Map.of(
                "year", year
        );

        return new AnoMoreLikeThisQuery(queryParams, "query-templates/specialized/ano_more_like_this.json");
    }
}
