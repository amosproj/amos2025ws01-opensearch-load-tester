package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class AnoPrefixMatchQuery extends AbstractQuery {

    private AnoPrefixMatchQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static AnoPrefixMatchQuery random() {
        String prefix = "B";

        List<String> tokens = List.of(
                "Brutto", "Netto", "Abrechnung",
                "Januar", "März", "Juli", "Oktober",
                "2018", "2020", "2025"
        );
        String matchQuery = tokens.get(faker().random().nextInt(tokens.size()));

        Map<String, String> queryParams = Map.of(
                "prefix", prefix,
                "match_query", matchQuery
        );

        return new AnoPrefixMatchQuery(
                queryParams,
                "query-templates/complex/q4_ano_prefix.json"
        );
    }
}
