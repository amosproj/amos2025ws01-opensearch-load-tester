package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoMultiPrefixSortQuery extends AbstractQuery {

    private DuoMultiPrefixSortQuery(Map<String, String> params) {
        super(params, "query-templates/complex/q3_duo_multi_prefix_sort.json");
    }

    public static DuoMultiPrefixSortQuery random() {



        String inv1= String.valueOf(faker().random().nextDouble() < 0.7
                ? faker().number().numberBetween(1000, 1000000)
                : null);

        String inv2 = String.valueOf(
                faker().number().numberBetween(1, 10)
        );


        String partnerPrefix = faker().letterify("?").toLowerCase();
        String customerPrefix = String.valueOf(
                faker().number().numberBetween(1000, 1000000)
        );


        Map<String, String> params = Map.of(
                "inv_prefix_1", inv1,
                "inv_prefix_2", inv2,
                "partner_prefix", partnerPrefix,
                "customer_prefix", customerPrefix
        );

        return new DuoMultiPrefixSortQuery(params);
    }
}
