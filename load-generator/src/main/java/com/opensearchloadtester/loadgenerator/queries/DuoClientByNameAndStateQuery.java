package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class DuoClientByNameAndStateQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/q8_duo_client_by_name_and_state.json";

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        String clientName = FAKER.name().fullName();

        List<String> appStates = List.of("APPROVED", "NOT_RELEVANT", "UNDISPATCHED");
        String documentApprovalState = appStates.get(RANDOM.nextInt(appStates.size()));

        Map<String, String> queryParams = Map.of(
                "client_name", clientName,
                "approval_state", documentApprovalState
        );
        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}
