package com.opensearchloadtester.loadgenerator.queries;

import net.datafaker.Faker;

import java.util.List;
import java.util.Map;

public class DuoStateLocationQuery extends AbstractQuery {

    private DuoStateLocationQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoStateLocationQuery random() {
        List<String> appStates = List.of("APPROVED", "NOT_RELEVANT", "UNDISPATCHED");
        String documentApprovalState = appStates.get(faker().random().nextInt(appStates.size()));

        List<String> loc = List.of("BELEGE", "BELEGFREIGABE");
        String location = loc.get(faker().random().nextInt(loc.size()));

        Map<String, String> queryParams = Map.of(
                "approval_state", documentApprovalState,
                "location", location
        );

        return new DuoStateLocationQuery(queryParams, "queries/q3_duo_state_location.json");
    }
}
