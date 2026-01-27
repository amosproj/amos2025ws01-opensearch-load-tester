package com.opensearchloadtester.loadgenerator.queries;

import java.util.Locale;
import java.util.Map;

public class DuoBookingByAmountRangeQuery extends AbstractQuery {

    private DuoBookingByAmountRangeQuery(Map<String, String> queryParams, String queryTemplatePath) {
        super(queryParams, queryTemplatePath);
    }

    public static DuoBookingByAmountRangeQuery random() {
        double startTotalGrossAmount = faker().number().numberBetween(0, 9_999_999);
        String amountMin = String.format(Locale.ROOT, "%.2f", startTotalGrossAmount);
        String amountMax = String.format(
                Locale.ROOT,
                "%.2f",
                startTotalGrossAmount + faker().number().randomDouble(2, 0, 9_999_999)
        );

        Map<String, String> queryParams = Map.of(
                "amount_min", amountMin,
                "amount_max", amountMax
        );

        return new DuoBookingByAmountRangeQuery(queryParams,
                "query-templates/q11_duo_booking_by_amount_range.json");
    }
}
