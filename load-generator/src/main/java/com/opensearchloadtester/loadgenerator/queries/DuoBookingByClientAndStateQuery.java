package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoBookingByClientAndStateQuery extends AbstractQuery {

    private DuoBookingByClientAndStateQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoBookingByClientAndStateQuery random() {
        String clientName = faker().name().fullName();
        String bookingState = "TO_BOOK";

        Map<String, String> queryParams = Map.of(
                "client_name", clientName,
                "booking_state", bookingState
        );

        return new DuoBookingByClientAndStateQuery(queryParams,
                "queries/q4_duo_booking_by_client_and_state.json");
    }
}
