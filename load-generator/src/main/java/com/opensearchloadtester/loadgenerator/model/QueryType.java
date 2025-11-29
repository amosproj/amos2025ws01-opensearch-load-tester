package com.opensearchloadtester.loadgenerator.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QueryType {
    YEAR_RANGE("/queries/q1_ano_payroll_range.json"),
    CATEGORY("/queries/q2_duo_invoice_category.json"),
    STATE_LOCATION("/queries/q3_duo_state_location.json"),
    BOOKING_CLIENT_STATE("/queries/q4_duo_booking_by_client_and_state.json"),
    AGGREGATION("/queries/q5_ano_clients_aggregation.json"),
    CLIENT_YEAR("/queries/q6_ano_client_by_year.json"),
    CLIENT_CUSTOMER_NO("/queries/q7_duo_client_by_customer_number.json"),
    CLIENT_NAME_STATE("/queries/q8_duo_client_by_name_and_state.json"),
    PAYROLL_TYPE_LANGUAGE("/queries/q9_ano_payroll_type_language.json"),
    BOOKING_COSTCENTER_DATE("/queries/q10_duo_booking_by_costcenter_and_date.json"),
    BOOKING_AMOUNT_RANGE("/queries/q11_duo_booking_by_amount_range.json");

    private final String templatePath;
}
