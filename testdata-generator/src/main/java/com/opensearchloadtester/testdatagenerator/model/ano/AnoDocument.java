package com.opensearchloadtester.testdatagenerator.model.ano;

import com.opensearchloadtester.testdatagenerator.model.AbstractDocument;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
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
    private Long dssDocumentOrientation;
    private String dssDocumentPath;
    private String dssDocumentSource;
    private Instant dssLastModifiedUserDatetime;
    private String dssOriginalFilename;
    private Boolean dssRecyclebin;


    // Method to create a random AnoDocument object
    public static AnoDocument random() {
        AnoDocument anoDocument = fillCommonFieldsRandomly(new AnoDocument());

        // Fill with random values without purpose
        anoDocument.dssCreationDatetime = Instant.now();
        anoDocument.dssCustomMetadataPayrollInfo = PayrollInfo.random();
        anoDocument.dssDeleteRetentionMinRetention = Instant.now();
        anoDocument.dssDocumentOrientation = RANDOM.nextLong(4);
        anoDocument.dssDocumentPath = "example/home/payrolls/" + anoDocument.dssDocumentName;
        anoDocument.dssDocumentSource = "https://example.de/payrolls/" + anoDocument.dssDocumentName;
        anoDocument.dssLastModifiedUserDatetime = Instant.now();
        anoDocument.dssOriginalFilename = anoDocument.dssDocumentName;
        anoDocument.dssRecyclebin = false;
        return anoDocument;
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

            // Fill with random values without purpose
            payrollInfo.accountingMonth = RANDOM.nextInt(12) + 1;
            payrollInfo.accountingYear = 2020 + RANDOM.nextInt(6);
            payrollInfo.firstAccess = Instant.now();
            List<String> lang = List.of("German", "English", "Spanish", "French");
            payrollInfo.language = lang.get(RANDOM.nextInt(lang.size()));
            List<String> type = List.of("Monthly", "Yearly", "Quarterly");
            payrollInfo.payrollType = type.get(RANDOM.nextInt(type.size()));
            payrollInfo.provisionDate = Instant.now();
            return payrollInfo;
        }
    }
}
