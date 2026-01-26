package com.opensearchloadtester.loadgenerator.model;

import com.opensearchloadtester.loadgenerator.queries.*;

import java.util.function.Supplier;

public enum QueryType {
    ANO_PAYROLL_RANGE(AnoPayrollRangeQuery::random),
    DUO_INVOICE_CATEGORY(DuoInvoiceCategoryQuery::random),
    DUO_STATE_LOCATION(DuoStateLocationQuery::random),
    DUO_BOOKING_BY_CLIENT_AND_STATE(DuoBookingByClientAndStateQuery::random),
    ANO_CLIENTS_AGGREGATION(AnoClientsAggregationQuery::random),
    ANO_CLIENT_BY_YEAR(AnoClientByYearQuery::random),
    DUO_CLIENT_BY_CUSTOMER_NUMBER(DuoClientByCustomerNumberQuery::random),
    DUO_CLIENT_BY_NAME_AND_STATE(DuoClientByNameAndStateQuery::random),
    ANO_PAYROLL_TYPE_LANGUAGE(AnoPayrollTypeLanguageQuery::random),
    DUO_BOOKING_BY_COSTCENTER_AND_DATE(DuoBookingByCostcenterAndDateQuery::random),
    DUO_BOOKING_BY_AMOUNT_RANGE(DuoBookingByAmountRangeQuery::random),
    DUO_INVOICE_DIS_MAX(DuoInvoiceDisMax::random),
    DUO_INVOICE_DIS_MAX_2(DuoInvoiceDisMax2::random),
    DUO_COMPLEX(DuoComplexQuery::random),
    DOCNAME_REGEX(DocNameRegexQuery::random),
    ANO_MULTI_REGEX(AnoMultiRegexQuery::random),
    DUO_MULTI_REGEX(DuoMultiRegexQuery::random);

    private final Supplier<? extends Query> supplier;

    QueryType(Supplier<? extends Query> supplier) {
        this.supplier = supplier;
    }

    public Query createRandomQuery() {
        return supplier.get();
    }
}
