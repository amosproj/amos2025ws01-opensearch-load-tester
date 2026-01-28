package com.opensearchloadtester.loadgenerator.queries;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DocNameRegexQuery extends AbstractQuery {

    private DocNameRegexQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DocNameRegexQuery random() {
        // exact same random generation as in testdata generator
        Instant i = Date.from(faker().timeAndDate().past(3650, TimeUnit.DAYS)).toInstant();
        java.time.ZonedDateTime zdt = i.atZone(java.time.ZoneId.systemDefault());
        String year = String.valueOf(zdt.getYear());

        Map<String, String> queryParams = Map.of(
                "year", year
        );

        return new DocNameRegexQuery(queryParams, "query-templates/leaf/q1_docName_regex.json");
    }
}
