package com.opensearchloadtester.testdatagenerator.model;

import lombok.Data;

import java.time.Instant;

@Data
public class AnoRecord implements Recordable {

    /**
     * Attributes of a Record Instance correspond to the data stored in an OpenSearch Interface.
     * Wrapper Classes are used for datatypes to ensure stability for null entries.
     */
    private String id;
    private String customAll;
    private Long contentLength;
    private String contentType;
    private Instant dssCreationDatetime;
    private String dssCreationUserDisplayName;
    private String dssCreationUserIdKey;
    private PayrollInfo dssCustomMetadataPayrollInfo;
    private String dssDataspaceId;
    private Instant dssDeleteRetentionMinRetention;
    private String dssDocumentId;
    private String dssDocumentName;
    private Long dssDocumentOrientation;
    private String dssDocumentPath;
    private String dssDocumentSource;
    private Instant dssLastModifiedDatetime;
    private Instant dssLastModifiedUserDatetime;
    private String dssLastModifiedUserDisplayName;
    private String dssLastModifiedUserIdKey;
    private String dssOriginalFilename;
    private String dssProcessingFlagOwner;
    private Boolean dssRecyclebin;
    private String dssVersion;
    private String etag;
    private Instant lastDocumentChange;

    // Method to generate a random AnoRecord Object
    public static Recordable random() {
        // TODO implement random values
        AnoRecord res = new AnoRecord();
        res.id = "id-" + Math.random();
        res.customAll = "Sample text ";
        res.contentLength = 1L;
        res.contentType = "text";
        res.dssCreationUserDisplayName = "User 1";
        res.dssCreationUserIdKey = "user-" + Math.random();
        res.dssVersion = "1";
        res.etag = "etag-" + Math.random();
        res.lastDocumentChange = Instant.now();
        res.dssRecyclebin = false;
        return res;
    }

    /**
     * Nested Class for PayrollInfo of Ano OpenSearch Indices
     */
    @Data
    public static class PayrollInfo {
        /**
         * Attributes correspond to the data stored in an OpenSearch Interface.
         * Wrapper Classes are used for datatypes to ensure stability for null entries.
         */
        private Integer accountingMonth;
        private Integer accountingYear;
        private Instant firstAccess;
        private String language;
        private String payrollType;
        private Instant provisionDate;

        // Method to generate a random PayrollInfo Object
        public static PayrollInfo random() {
            // TODO implement random values
            PayrollInfo res = new PayrollInfo();
            res.accountingMonth = 1;
            res.accountingYear = 2025;
            res.firstAccess = Instant.now();
            res.language = "DE";
            res.payrollType = "Monthly";
            res.provisionDate = Instant.now();
            return res;
        }
    }
}