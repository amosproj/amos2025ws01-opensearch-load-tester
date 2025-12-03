package com.opensearchloadtester.testdatagenerator.model.ano;

import net.datafaker.Faker;

import com.opensearchloadtester.testdatagenerator.model.AbstractDocument;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.util.Locale;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE) // AnoDocument objects must be created via random() method
public class AnoDocument extends AbstractDocument {

    /**
     * Attributes of an AnoDocument correspond to the data stored in an OpenSearch index.
     * Wrapper Classes are used for datatypes to ensure stability for null entries.
     */
    private Instant dssCreationDatetime;
    private PayrollInfo dssCustomMetadataPayrollInfo;
    private Instant dssDeleteRetentionMinRetention;
    private int dssDocumentOrientation;
    private String dssDocumentPath;
    private String dssDocumentSource;
    private Instant dssLastModifiedUserDatetime;
    private String dssOriginalFilename;
    private Boolean dssRecyclebin;

    // Method to create a random AnoDocument object
    public static AnoDocument random() {
        AnoDocument anoDocument = fillCommonFieldsRandomly(new AnoDocument());
        // Fill with random values
        anoDocument.dssCustomMetadataPayrollInfo = PayrollInfo.random();
        anoDocument.dssCreationDatetime = faker.timeAndDate().past(3650, TimeUnit.DAYS);
        // All examples where null
        anoDocument.dssDeleteRetentionMinRetention = switch (RANDOM.nextInt(3)) {
            case 0 -> faker.timeAndDate().past(90, TimeUnit.DAYS);
            case 1 -> faker.timeAndDate().future(3650, TimeUnit.DAYS);
            default -> null;
        };
        // All examples were 0
        anoDocument.dssDocumentOrientation = 0;
        anoDocument.dssDocumentPath = "/" + anoDocument.dssDataspaceId
                + "/documents/" + anoDocument.dssDocumentId + "/document";
        anoDocument.dssDocumentSource = faker.internet().url();
        anoDocument.dssLastModifiedUserDatetime = faker.timeAndDate().past(90, TimeUnit.DAYS);
        anoDocument.dssOriginalFilename = "Brutto-Netto-Abrechnung "
                + translateMonthToName(anoDocument.dssCustomMetadataPayrollInfo.getAccountingMonth())
                + " " + anoDocument.dssCustomMetadataPayrollInfo.getAccountingYear() + ".pdf";
        anoDocument.dssRecyclebin = false;

        anoDocument.dssDocumentName = "Brutto-Netto-Abrechnung "
                + translateMonthToName(anoDocument.dssCustomMetadataPayrollInfo.getAccountingMonth())
                + " " + anoDocument.dssCustomMetadataPayrollInfo.getAccountingYear() + ".pdf";

        anoDocument.customAll = anoDocument.dssOriginalFilename + " "
                + anoDocument.dssProcessingFlagOwner;

        return anoDocument;
    }

    public static String translateMonthToName(int month) {
        return switch (month) {
            case 1 -> "Januar";
            case 2 -> "Februar";
            case 3 -> "März";
            case 4 -> "April";
            case 5 -> "Mai";
            case 6 -> "Juni";
            case 7 -> "Juli";
            case 8 -> "August";
            case 9 -> "September";
            case 10 -> "Oktober";
            case 11 -> "November";
            case 12 -> "Dezember";
            default -> "Unknown";
        };
    }

    /**
     * Nested Class for PayrollInfo of AnoDocument
     */
    @Getter
    @Setter
    public static class PayrollInfo {

        private Integer accountingMonth;
        private Integer accountingYear;
        private Instant firstAccess;
        private String language;
        private String payrollType;
        private Instant provisionDate;

        // Method to create a random PayrollInfo object
        public static PayrollInfo random() {
            PayrollInfo payrollInfo = new PayrollInfo();
            payrollInfo.provisionDate = faker.timeAndDate().past(3650, TimeUnit.DAYS);
            // Extract month and year from provisionDate
            java.time.ZonedDateTime zdt = payrollInfo.provisionDate.atZone(java.time.ZoneId.systemDefault());
            payrollInfo.accountingMonth = zdt.getMonthValue();
            payrollInfo.accountingYear = zdt.getYear();
            // can be null (30%) or typically next month (16 days later)
            if (RANDOM.nextDouble() < 0.3) {
                payrollInfo.firstAccess = null;
            } else {
                payrollInfo.firstAccess = payrollInfo.provisionDate.plus(RANDOM.nextInt(16), ChronoUnit.DAYS);
            }

            List<String> lang = List.of("German", "English", "Spanish", "French");
            payrollInfo.language = lang.get(RANDOM.nextInt(lang.size()));
            List<String> type = List.of("Monthly", "Yearly", "Quarterly");
            payrollInfo.payrollType = type.get(RANDOM.nextInt(type.size()));

            return payrollInfo;
        }
    }
}
