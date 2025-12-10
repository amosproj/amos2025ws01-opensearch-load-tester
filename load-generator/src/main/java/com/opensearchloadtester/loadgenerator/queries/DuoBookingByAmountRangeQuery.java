package com.opensearchloadtester.loadgenerator.queries;

import java.util.Map;

public class DuoBookingByAmountRangeQuery extends AbstractQuery {

    private static final String QUERY_TEMPLATE_PATH = "queries/q11_duo_booking_by_amount_range.json";

    @Override
    public String generateQuery() {
        String queryTemplate = loadQueryTemplate(QUERY_TEMPLATE_PATH);

        double startTotalGrossAmount = RANDOM.nextDouble(9999999);
        String amountMin = String.format("%.2f", startTotalGrossAmount);
        String amountMax = String.format("%.2f", startTotalGrossAmount + RANDOM.nextDouble(9999999));

        Map<String, String> queryParams = Map.of(
                "amount_min", amountMin,
                "amount_max", amountMax
        );
        return applyQueryParams(queryTemplate, queryParams);
    }

    @Override
    public String getQueryTemplatePath() {
        return QUERY_TEMPLATE_PATH;
    }
}
