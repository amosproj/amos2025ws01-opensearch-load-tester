package com.opensearchloadtester.loadgenerator.queries;

import java.time.LocalDate;
import java.util.Map;

public class AnoPrefixRangeQuery extends AbstractQuery {

    private AnoPrefixRangeQuery(Map<String, String> params) {
        super(params, "query-templates/complex/q2_ano_prefix_range.json");
    }

    public static AnoPrefixRangeQuery random() {
        String prefix = "B";
        int year = faker().random().nextInt(100) < 70 ? 2025 : 2026;

        String from = year + "-01-01T00:00:00Z";
        String to   = year   + "-12-31T23:59:59Z";

        Map<String, String> params = Map.of(
                "prefix", prefix,
                "gte", from,
                "lte", to
        );
        return new AnoPrefixRangeQuery(params);
    }
}
