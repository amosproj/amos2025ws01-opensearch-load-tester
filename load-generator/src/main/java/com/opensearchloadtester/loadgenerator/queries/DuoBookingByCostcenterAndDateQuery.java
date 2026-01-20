package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoBookingByCostcenterAndDateQuery extends AbstractQuery {

    private DuoBookingByCostcenterAndDateQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoBookingByCostcenterAndDateQuery random() {
        int startYear = faker().number().numberBetween(2015, 2026);
        String year_from = String.valueOf(startYear);
        String year_to = String.valueOf(faker().number().numberBetween(startYear, 2026));

        Map<String, String> queryParams = Map.of(
                "date_from", year_from,
                "date_to", year_to
        );

        return new DuoBookingByCostcenterAndDateQuery(queryParams,
                "query-templates/q10_duo_booking_by_costcenter_and_date.json");
    }
}
