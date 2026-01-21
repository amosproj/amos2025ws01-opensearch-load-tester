package com.opensearchloadtester.loadgenerator.model;

import com.opensearchloadtester.loadgenerator.queries.*;
import com.opensearchloadtester.loadgenerator.queries.span.AnoSpanNearQuery;
import com.opensearchloadtester.loadgenerator.queries.span.DuoSpanNearQuery;
import com.opensearchloadtester.loadgenerator.queries.specialized.AnoMoreLikeThisQuery;
import com.opensearchloadtester.loadgenerator.queries.specialized.DuoMoreLikeThisQuery;

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
    DUO_COMPLEX(DuoComplexQuery::random),
    DOCNAME_REGEX(DocNameRegexQuery::random),
    ANO_MULTI_REGEX(AnoMultiRegexQuery::random),
    DUO_MULTI_REGEX(DuoMultiRegexQuery::random),
    // Span Query
    ANO_SPAN_NEAR(AnoSpanNearQuery::random),
    DUO_SPAN_NEAR(DuoSpanNearQuery::random),
    // More-Like-This Query
    ANO_MORE_LIKE_THIS(AnoMoreLikeThisQuery::random),
    DUO_MORE_LIKE_THIS(DuoMoreLikeThisQuery::random);

    private final Supplier<? extends Query> supplier;

    QueryType(Supplier<? extends Query> supplier) {
        this.supplier = supplier;
    }

    public Query createRandomQuery() {
        return supplier.get();
    }
}
