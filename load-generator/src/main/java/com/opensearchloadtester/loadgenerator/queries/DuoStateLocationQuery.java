package com.opensearchloadtester.loadgenerator.queries;

import java.util.List;
import java.util.Map;

public class DuoStateLocationQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/q3_duo_state_location.json";

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        List<String> appStates = List.of("APPROVED", "NOT_RELEVANT", "UNDISPATCHED");
        String documentApprovalState = appStates.get(RANDOM.nextInt(appStates.size()));

        List<String> loc = List.of("BELEGE", "BELEGFREIGABE");
        String location = loc.get(RANDOM.nextInt(loc.size()));

        Map<String, String> queryParams = Map.of(
                "approval_state", documentApprovalState,
                "location", location
        );
        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}
