package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoBookingByCostcenterAndDateQuery extends AbstractQuery {

    private DuoBookingByCostcenterAndDateQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoBookingByCostcenterAndDateQuery random() {
        int startYear = faker().number().numberBetween(2015, 2025);
        String minYear = String.valueOf(startYear);
        String maxYear = String.valueOf(faker().number().numberBetween(startYear, 2025));

        Map<String, String> queryParams = Map.of(
                "min_year", minYear,
                "max_year", maxYear
        );

        return new DuoBookingByCostcenterAndDateQuery(queryParams,
                "queries/q10_duo_booking_by_costcenter_and_date.json");
    }
}
