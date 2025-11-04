package com.opensearchloadtester.testdatagenerator.model;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Data
public class DuoRecord implements Recordable {

    /**
     * Attributes of a Record Instance correspond to the data stored in an OpenSearch Interface.
     * Wrapper Classes are used for datatypes to ensure stability for null entries.
     */
    private String id;
    private String customAll;
    private Instant lastDocumentChange;
    private String dssDataspaceId;
    private String dssDocumentId;
    private String dssDocumentName;
    private Long contentLength;
    private String ocrFulltext;
    private String contentType;
    private String etag;
    private String dssVersion;
    private String dssLastModifiedUserIdKey;
    private Instant dssLastModifiedDatetime;
    private DuoMetadata dssCustomMetadataDuo;

    public String getId() {
        return id;
    }

    public String getCustomAll() {
        return customAll;
    }

    public Instant getLastDocumentChange() {
        return lastDocumentChange;
    }

    public String getDssDataspaceId() {
        return dssDataspaceId;
    }

    public String getDssDocumentId() {
        return dssDocumentId;
    }

    public String getDssDocumentName() {
        return dssDocumentName;
    }

    public Long getContentLength() {
        return contentLength;
    }

    public String getOcrFulltext() {
        return ocrFulltext;
    }

    public String getContentType() {
        return contentType;
    }

    public String getEtag() {
        return etag;
    }

    public String getDssVersion() {
        return dssVersion;
    }

    public String getDssLastModifiedUserIdKey() {
        return dssLastModifiedUserIdKey;
    }

    public Instant getDssLastModifiedDatetime() {
        return dssLastModifiedDatetime;
    }

    public DuoMetadata getDssCustomMetadataDuo() {
        return dssCustomMetadataDuo;
    }

    public String getDssCreationUserDisplayName() {
        return dssCreationUserDisplayName;
    }

    public String getDssLastModifiedUserDisplayName() {
        return dssLastModifiedUserDisplayName;
    }

    public String getDssCreationUserIdKey() {
        return dssCreationUserIdKey;
    }

    public String getDssProcessingFlagOwner() {
        return dssProcessingFlagOwner;
    }

    private String dssCreationUserDisplayName;
    private String dssLastModifiedUserDisplayName;
    private String dssCreationUserIdKey;
    private String dssProcessingFlagOwner;

    // Method to generate a random DuoRecord Object
    public static DuoRecord random() {
        Random rand = new Random();
        DuoRecord res = new DuoRecord();
        res.id = "id-" + rand.nextInt(100000);
        res.customAll = "This is customAll from "+res.id;
        res.lastDocumentChange = Instant.now();
        res.dssDataspaceId = "dataspace-" + rand.nextInt(1001);
        res.dssDocumentId = "document-" + rand.nextInt(10000);
        res.dssDocumentName = "Payroll-"+res.dssDocumentId+"-random";
        res.contentLength = rand.nextLong(10000);;
        res.ocrFulltext = "This is OCR fulltext from "+res.id;
        res.contentType = "Document";
        res.etag = "etag-" + rand.nextInt(100000000);
        res.dssVersion = "1.0";
        res.dssLastModifiedUserIdKey = "user-random";
        res.dssLastModifiedDatetime = Instant.now();
        res.dssCustomMetadataDuo = DuoMetadata.random();
        res.dssCreationUserDisplayName = "RandomGenerator";
        res.dssLastModifiedUserDisplayName = "RandomGenerator";
        res.dssCreationUserIdKey = "user-random";
        res.dssProcessingFlagOwner = "owner-random";
        return res;
    }

    /**
     * Nested Class for PayrollInfo of Duo OpenSearch Indices
     */
    @Data
    public static class DuoMetadata {
        /**
         * Attributes correspond to the data stored in an OpenSearch Interface.
         * Wrapper Classes are used for datatypes to ensure stability for null entries.
         */
        private String bookingState;
        private Instant bookingStateChangedAt;
        private Long companyId;
        private String currency;
        private String customerNumber;
        private Instant deletedAt;
        private Integer documentType;
        private String documentCategory;
        private String documentInvoiceType;
        private String einvoiceFulltext;
        private Boolean hasPositionCorrection;
        private String invoiceBusinessPartner;
        private Long invoiceBusinessPartnerId;
        private Instant invoiceDate;
        private String invoiceNumber;
        private Instant lastModifiedDatetime;
        private String lastModifiedUserIdKey;
        private String location;
        private Instant paidAt;
        private String paidStatus;
        private List<Position> positions;
        private Double totalGrossAmount;
        private String uploaderScId;
        private Instant timeOfUpload;
        private String documentApprovalState;
        private String transactionIds;

        public String getBookingState() {
            return bookingState;
        }

        public Instant getBookingStateChangedAt() {
            return bookingStateChangedAt;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public String getCurrency() {
            return currency;
        }

        public String getCustomerNumber() {
            return customerNumber;
        }

        public Instant getDeletedAt() {
            return deletedAt;
        }

        public Integer getDocumentType() {
            return documentType;
        }

        public String getDocumentCategory() {
            return documentCategory;
        }

        public String getDocumentInvoiceType() {
            return documentInvoiceType;
        }

        public String getEinvoiceFulltext() {
            return einvoiceFulltext;
        }

        public Boolean getHasPositionCorrection() {
            return hasPositionCorrection;
        }

        public String getInvoiceBusinessPartner() {
            return invoiceBusinessPartner;
        }

        public Long getInvoiceBusinessPartnerId() {
            return invoiceBusinessPartnerId;
        }

        public Instant getInvoiceDate() {
            return invoiceDate;
        }

        public String getInvoiceNumber() {
            return invoiceNumber;
        }

        public Instant getLastModifiedDatetime() {
            return lastModifiedDatetime;
        }

        public String getLastModifiedUserIdKey() {
            return lastModifiedUserIdKey;
        }

        public String getLocation() {
            return location;
        }

        public Instant getPaidAt() {
            return paidAt;
        }

        public String getPaidStatus() {
            return paidStatus;
        }

        public List<Position> getPositions() {
            return positions;
        }

        public Double getTotalGrossAmount() {
            return totalGrossAmount;
        }

        public String getUploaderScId() {
            return uploaderScId;
        }

        public Instant getTimeOfUpload() {
            return timeOfUpload;
        }

        public String getDocumentApprovalState() {
            return documentApprovalState;
        }

        public String getTransactionIds() {
            return transactionIds;
        }

        // Method to generate a random DuoMetadata Object
        public static DuoMetadata random() {
            Random rand = new Random();
            DuoMetadata meta = new DuoMetadata();
            List<String> states = List.of("Open", "Paid", "Closed");
            meta.bookingState = states.get(rand.nextInt(states.size()));
            meta.bookingStateChangedAt = Instant.now();
            meta.companyId = rand.nextLong(10000);
            List<String> curr = List.of("EUR", "USD");
            meta.currency = curr.get(rand.nextInt(curr.size()));
            meta.customerNumber = "customer-" +  rand.nextInt(10000);
            meta.deletedAt = Instant.MAX;
            meta.documentType = rand.nextInt(6);
            meta.documentCategory = "Invoice";
            List<String> invTypes = List.of("Standard", "Urgent", "Reminder");
            meta.documentInvoiceType = invTypes.get(rand.nextInt(invTypes.size()));
            meta.einvoiceFulltext = "This is fulltext of invoice " + meta.invoiceNumber;
            meta.hasPositionCorrection = false;
            meta.invoiceBusinessPartnerId = rand.nextLong(10000);
            meta.invoiceBusinessPartner = "partner-" + meta.invoiceBusinessPartnerId.toString();
            meta.invoiceDate = Instant.now();
            meta.invoiceNumber = "inv-"+rand.nextInt(10000);
            meta.lastModifiedDatetime = Instant.now();
            meta.lastModifiedUserIdKey = "user-random";
            List<String> loc = List.of("Germany", "England", "Spain", "France");
            meta.location = loc.get(rand.nextInt(loc.size()));
            meta.paidAt = Instant.now();
            meta.paidStatus = states.get(rand.nextInt(states.size()));
            List<Position> pos = new ArrayList<>();
            int numPos = rand.nextInt(50);
            for(int i = 0; i < numPos; i++) {
                pos.add(Position.random());
            }
            meta.positions = pos;
            meta.totalGrossAmount = rand.nextDouble(10000000);
            meta.uploaderScId = "up-" + rand.nextInt(1000);
            meta.timeOfUpload = Instant.now();
            List<String> appStates = List.of("Approved", "Not approved", "In progress");
            meta.documentApprovalState = appStates.get(rand.nextInt(appStates.size()));
            meta.transactionIds = "trans-" + rand.nextInt(10000000);
            return meta;
        }
    }

    /**
     * Nested Class for Positions of Duo OpenSearch Indices
     */
    @Data
    public static class Position {
        /**
         * Attributes correspond to the data stored in an OpenSearch Interface.
         * Wrapper Classes are used for datatypes to ensure stability for null entries.
         */
        private String note;
        private String costCenter1;
        private String costCenter2;
        private Instant serviceDate;

        public String getNote() {
            return note;
        }

        public String getCostCenter1() {
            return costCenter1;
        }

        public String getCostCenter2() {
            return costCenter2;
        }

        public Instant getServiceDate() {
            return serviceDate;
        }

        // Method to generate a random Position Object
        public static Position random() {
            Random rand = new Random();
            Position res = new Position();
            res.note = "This is a sample note " + rand.nextInt(10000);
            res.costCenter1 = "cc-" + rand.nextInt(10000);
            res.costCenter2 = "cc-" + rand.nextInt(10000);
            res.serviceDate = Instant.now();
            return res;
        }
    }
}
