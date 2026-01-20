package com.opensearchloadtester.loadgenerator.service;

import com.opensearchloadtester.loadgenerator.model.QueryMixEntry;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class QueryTypePicker {

    private final QueryType[] types;
    private final int[] cumulative;
    private final int total;

    private QueryTypePicker(QueryType[] types, int[] cumulative, int total) {
        this.types = types;
        this.cumulative = cumulative;
        this.total = total;
    }

    public static QueryTypePicker fromScenario(ScenarioConfig config) {
        List<QueryMixEntry> mix = config.getQueryMix();
        if (mix != null && !mix.isEmpty()) {
            QueryType[] types = new QueryType[mix.size()];
            int[] cumulative = new int[mix.size()];

            int sum = 0;
            for (int i = 0; i < mix.size(); i++) {
                QueryMixEntry e = mix.get(i);
                sum += e.getPercent();
                types[i] = e.getType();
                cumulative[i] = sum;
            }

            if (sum <= 0) {
                throw new IllegalArgumentException("queryMix must contain at least one positive weight.");
            }

            return new QueryTypePicker(types, cumulative, sum);
        }

        // fallback: uniform over queryTypes
        List<QueryType> allowed = config.getQueryTypes();
        if (allowed == null || allowed.isEmpty()) {
            throw new IllegalArgumentException("queryTypes must not be empty.");
        }

        QueryType[] types = allowed.toArray(new QueryType[0]);
        int n = types.length;

        int[] cumulative = new int[n];
        for (int i = 0; i < n; i++) {
            cumulative[i] = i + 1; // 1..n
        }
        return new QueryTypePicker(types, cumulative, n);
    }

    public QueryType next() {
        int r = ThreadLocalRandom.current().nextInt(total);
        for (int i = 0; i < cumulative.length; i++) {
            if (r < cumulative[i]) return types[i];
        }
        return types[types.length - 1];
    }
}
