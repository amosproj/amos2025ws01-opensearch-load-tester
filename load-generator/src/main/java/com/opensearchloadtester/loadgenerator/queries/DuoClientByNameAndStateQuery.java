package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class DuoClientByNameAndStateQuery extends AbstractQuery {

    private DuoClientByNameAndStateQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoClientByNameAndStateQuery random() {
        String clientName = faker().name().fullName();

        List<String> appStates = List.of("APPROVED", "NOT_RELEVANT", "UNDISPATCHED");
        String documentApprovalState = appStates.get(faker().random().nextInt(appStates.size()));

        Map<String, String> queryParams = Map.of(
                "client_name", clientName,
                "approval_state", documentApprovalState
        );

        return new DuoClientByNameAndStateQuery(queryParams,
                "queries/q8_duo_client_by_name_and_state.json");
    }
}
