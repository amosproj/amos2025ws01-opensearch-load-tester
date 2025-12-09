package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoBookingByClientAndStateQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/q4_duo_booking_by_client_and_state.json";

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        String clientName = FAKER.name().fullName();
        // TODO: rethink if we need more than the examples have
        String bookingState = "TO_BOOK";

        Map<String, String> queryParams = Map.of(
                "client_name", clientName,
                "booking_state", bookingState
        );
        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}
