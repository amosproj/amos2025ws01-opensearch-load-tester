package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DocNameRegexQuery extends AbstractQuery {

    private DocNameRegexQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DocNameRegexQuery random() {
        String year = getRandomYear();

        Map<String, String> queryParams = Map.of(
                "year", year
        );

        return new DocNameRegexQuery(queryParams, "query-templates/leaf/q1_docName_regex.json");
    }
}
