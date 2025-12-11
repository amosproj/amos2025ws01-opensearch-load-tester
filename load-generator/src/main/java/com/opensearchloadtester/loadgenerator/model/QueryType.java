package com.opensearchloadtester.loadgenerator.model;

import com.opensearchloadtester.loadgenerator.queries.*;

import java.util.function.Supplier;

public enum QueryType {
    ANO_PAYROLL_RANGE(AnoPayrollRangeQuery::new),
    DUO_INVOICE_CATEGORY(DuoInvoiceCategoryQuery::new),
    DUO_STATE_LOCATION(DuoStateLocationQuery::new),
    DUO_BOOKING_BY_CLIENT_AND_STATE(DuoBookingByClientAndStateQuery::new),
    ANO_CLIENTS_AGGREGATION(AnoClientsAggregationQuery::new),
    ANO_CLIENT_BY_YEAR(AnoClientByYearQuery::new),
    DUO_CLIENT_BY_CUSTOMER_NUMBER(DuoClientByCustomerNumberQuery::new),
    DUO_CLIENT_BY_NAME_AND_STATE(DuoClientByNameAndStateQuery::new),
    ANO_PAYROLL_TYPE_LANGUAGE(AnoPayrollTypeLanguageQuery::new),
    DUO_BOOKING_BY_COSTCENTER_AND_DATE(DuoBookingByCostcenterAndDateQuery::new),
    DUO_BOOKING_BY_AMOUNT_RANGE(DuoBookingByAmountRangeQuery::new),
    DUO_COMPLEX(DuoComplexQuery::new);

    private final Supplier<Query> constructor;

    QueryType(Supplier<Query> constructor) {
        this.constructor = constructor;
    }

    public Query createInstance() {
        return constructor.get();
    }
}
