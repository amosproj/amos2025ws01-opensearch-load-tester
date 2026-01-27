package com.opensearchloadtester.loadgenerator.queries.span;

import com.opensearchloadtester.loadgenerator.queries.AbstractQuery;

import java.time.Year;
import java.util.Map;

public class AnoSpanNearQuery extends AbstractQuery {

    private AnoSpanNearQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoSpanNearQuery random() {
        int currentYear = Year.now().getValue();
        String year = String.valueOf(
                faker().number().numberBetween(currentYear - 10, currentYear + 1)
        );

        Map<String, String> queryParams = Map.of(
                "year", year
        );

        return new AnoSpanNearQuery(queryParams, "query-templates/span/ano_span_near.json");
    }
}
