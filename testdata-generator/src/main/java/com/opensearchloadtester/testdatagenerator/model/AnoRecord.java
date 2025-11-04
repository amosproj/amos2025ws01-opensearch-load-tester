package com.opensearchloadtester.testdatagenerator.model;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Random;

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

    public String getId() {
        return id;
    }

    public String getCustomAll() {
        return customAll;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public String getContentType() {
        return contentType;
    }

    public Instant getDssCreationDatetime() {
        return dssCreationDatetime;
    }

    public String getDssCreationUserDisplayName() {
        return dssCreationUserDisplayName;
    }

    public String getDssCreationUserIdKey() {
        return dssCreationUserIdKey;
    }

    public PayrollInfo getDssCustomMetadataPayrollInfo() {
        return dssCustomMetadataPayrollInfo;
    }

    public String getDssDataspaceId() {
        return dssDataspaceId;
    }

    public Instant getDssDeleteRetentionMinRetention() {
        return dssDeleteRetentionMinRetention;
    }

    public String getDssDocumentId() {
        return dssDocumentId;
    }

    public String getDssDocumentName() {
        return dssDocumentName;
    }

    public Long getDssDocumentOrientation() {
        return dssDocumentOrientation;
    }

    public String getDssDocumentPath() {
        return dssDocumentPath;
    }

    public String getDssDocumentSource() {
        return dssDocumentSource;
    }

    public Instant getDssLastModifiedDatetime() {
        return dssLastModifiedDatetime;
    }

    public Instant getDssLastModifiedUserDatetime() {
        return dssLastModifiedUserDatetime;
    }

    public String getDssLastModifiedUserDisplayName() {
        return dssLastModifiedUserDisplayName;
    }

    public String getDssLastModifiedUserIdKey() {
        return dssLastModifiedUserIdKey;
    }

    public String getDssOriginalFilename() {
        return dssOriginalFilename;
    }

    public String getDssProcessingFlagOwner() {
        return dssProcessingFlagOwner;
    }

    public Boolean getDssRecyclebin() {
        return dssRecyclebin;
    }

    public String getDssVersion() {
        return dssVersion;
    }

    public String getEtag() {
        return etag;
    }

    public Instant getLastDocumentChange() {
        return lastDocumentChange;
    }

    // Method to generate a random AnoRecord Object
    public static AnoRecord random() {
        Random rand = new Random();
        AnoRecord res = new AnoRecord();
        // Filled with random values without purpose
        res.id = "id-" + rand.nextInt(100000);
        res.customAll = "This is customAll from "+res.id;
        res.contentLength = rand.nextLong(10000);
        res.contentType = "Document";
        res.dssCreationDatetime = Instant.now();
        res.dssCreationUserDisplayName = "RandomGenerator";
        res.dssCreationUserIdKey = "user-random";
        res.dssCustomMetadataPayrollInfo = PayrollInfo.random();
        res.dssDataspaceId = "dataspace-" + rand.nextInt(1001);
        res.dssDeleteRetentionMinRetention = Instant.now();
        res.dssDocumentId = "document-" + rand.nextInt(10000);
        res.dssDocumentName = "Payroll-"+res.dssDocumentId+"-random";
        res.dssDocumentOrientation = rand.nextLong(4);
        res.dssDocumentPath = "example/home/payrolls/" + res.dssDocumentName;
        res.dssDocumentSource = "https://example.de/payrolls/" + res.dssDocumentName;
        res.dssLastModifiedDatetime = Instant.now();
        res.dssLastModifiedUserDatetime = Instant.now();
        res.dssLastModifiedUserDisplayName = "RandomGenerator";
        res.dssLastModifiedUserIdKey = "user-random";
        res.dssOriginalFilename = res.dssDocumentName;
        res.dssProcessingFlagOwner = "owner-random";
        res.dssRecyclebin = false;
        res.dssVersion = "1.0";
        res.etag = "etag-" + rand.nextInt(100000000);
        res.lastDocumentChange = Instant.now();
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

        public Integer getAccountingMonth() {
            return accountingMonth;
        }

        public Integer getAccountingYear() {
            return accountingYear;
        }

        public Instant getFirstAccess() {
            return firstAccess;
        }

        public String getLanguage() {
            return language;
        }

        public String getPayrollType() {
            return payrollType;
        }

        public Instant getProvisionDate() {
            return provisionDate;
        }

        // Method to generate a random PayrollInfo Object
        public static PayrollInfo random() {
            Random rand = new Random();
            PayrollInfo res = new PayrollInfo();
            // Filled with random values without purpose
            res.accountingMonth = rand.nextInt(12)+1;
            res.accountingYear = 2020 + rand.nextInt(6);
            res.firstAccess = Instant.now();
            List<String> lang = List.of("German", "English", "Spanish", "French");
            res.language = lang.get(rand.nextInt(lang.size()));
            List<String> type = List.of("Monthly", "Yearly", "Quarterly");
            res.payrollType = type.get(rand.nextInt(type.size()));
            res.provisionDate = Instant.now();
            return res;
        }
    }
}