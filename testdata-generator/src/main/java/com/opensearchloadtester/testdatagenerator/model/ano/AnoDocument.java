package com.opensearchloadtester.testdatagenerator.model.ano;

import com.opensearchloadtester.testdatagenerator.model.AbstractDocument;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.List;

@Slf4j
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
    private Long dssDocumentOrientation;
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
        // irrelevant, bc not queried on
        anoDocument.dssCreationDatetime = Instant.now();
        // All examples where null
        anoDocument.dssDeleteRetentionMinRetention = null;
        // All examples were 0
        anoDocument.dssDocumentOrientation = (long) 0;
        anoDocument.dssDocumentPath = "/" + anoDocument.dssDataspaceId
                + "/documents/" + anoDocument.dssDocumentId + "/document";
        anoDocument.dssDocumentSource = null;
        // irrelevant, bc not queried on
        anoDocument.dssLastModifiedUserDatetime = Instant.now();
        anoDocument.dssOriginalFilename = "Brutto-Netto-Abrechnung "
                + translateMonthToName(anoDocument.dssCustomMetadataPayrollInfo.getAccountingMonth())
                + " " + anoDocument.dssCustomMetadataPayrollInfo.getAccountingYear() + ".pdf";
        anoDocument.dssRecyclebin = false;
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

            // Example query was from 2018 to 2024 --> 7 years
            // We generate from 2017 to 2025 -> 10 years for ~20% misses
            java.time.LocalDate startDate = java.time.LocalDate.of(2017, 1, 1);
            java.time.LocalDate endDate = java.time.LocalDate.of(2025, 12, 31);
            Instant start = startDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant();
            Instant end = endDate.atTime(23, 59, 59).atZone(java.time.ZoneId.systemDefault()).toInstant();
            long startSec = start.getEpochSecond();
            long endSec = end.getEpochSecond();
            long randomSec = java.util.concurrent.ThreadLocalRandom.current().nextLong(startSec, endSec + 1);
            payrollInfo.provisionDate = Instant.ofEpochSecond(randomSec);
            log.info("provisionDate: {}", payrollInfo.provisionDate);

            // Extract month and year from provisionDate
            java.time.ZonedDateTime zdt = payrollInfo.provisionDate.atZone(java.time.ZoneId.systemDefault());
            payrollInfo.accountingMonth = zdt.getMonthValue();
            payrollInfo.accountingYear = zdt.getYear();

            // can be null (30%) or typically next month (16 days later)
            if (RANDOM.nextDouble() < 0.3) {
                payrollInfo.firstAccess = null;
            } else {
                // 16 days == 23040 Minutes
                int randomMinutes = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 23040 + 1);
                Instant potentiallyFirstAccess = payrollInfo.provisionDate;
                payrollInfo.firstAccess = potentiallyFirstAccess.plus(java.time.Duration.ofMinutes(randomMinutes));
            }

            List<String> lang = List.of("German", "English", "Spanish", "French");
            payrollInfo.language = lang.get(RANDOM.nextInt(lang.size()));
            List<String> type = List.of("Monthly", "Yearly", "Quarterly");
            payrollInfo.payrollType = type.get(RANDOM.nextInt(type.size()));

            return payrollInfo;
        }
    }
}
