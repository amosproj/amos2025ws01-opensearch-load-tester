package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoBookingByCostcenterAndDateQuery extends AbstractQuery {

    private DuoBookingByCostcenterAndDateQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoBookingByCostcenterAndDateQuery random() {

        String fromYear = getRandomYear();
        String toYear = getRandomYearAfter(fromYear);

        Map<String, String> queryParams = Map.of(
                "date_from", fromYear,
                "date_to", toYear
        );

        return new DuoBookingByCostcenterAndDateQuery(queryParams,
                "queries/q10_duo_booking_by_costcenter_and_date.json");
    }
}
