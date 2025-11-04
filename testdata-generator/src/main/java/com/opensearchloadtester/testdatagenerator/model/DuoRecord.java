package com.opensearchloadtester.testdatagenerator.model;

import lombok.Data;

import java.time.Instant;
import java.util.List;

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
    private String dssCreationUserDisplayName;
    private String dssLastModifiedUserDisplayName;
    private String dssCreationUserIdKey;
    private String dssProcessingFlagOwner;

    // Method to generate a random DuoRecord Object
    public static DuoRecord random() {
        // TODO implement random values
        DuoRecord res = new DuoRecord();
        res.id = "id-" + Math.random();
        res.customAll = "Sample text";
        res.lastDocumentChange = Instant.now();
        res.dssDataspaceId = "dataspace-" + Math.random();
        res.dssDocumentId = "doc-" + Math.random();
        res.dssDocumentName = "Document 1";
        res.contentLength = 1L;
        res.ocrFulltext = "OCR text";
        res.contentType = "text";
        res.etag = "etag-" + Math.random();
        res.dssVersion = "1";
        res.dssLastModifiedUserIdKey = "user-" + Math.random();
        res.dssLastModifiedDatetime = Instant.now();
        res.dssCustomMetadataDuo = DuoMetadata.random();
        res.dssCreationUserDisplayName = "User 1";
        res.dssLastModifiedUserDisplayName = "User 1";
        res.dssCreationUserIdKey = "user-" + Math.random();
        res.dssProcessingFlagOwner = "ownerflag";
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

        // Method to generate a random DuoMetadata Object
        public static DuoMetadata random() {
            // TODO implement random values
            DuoMetadata meta = new DuoMetadata();
            meta.bookingState = "Open";
            meta.bookingStateChangedAt = Instant.now();
            meta.companyId = 1L;
            meta.currency = "EUR";
            meta.customerNumber = "Customer 1";
            meta.deletedAt = Instant.now();
            meta.documentType = 1;
            meta.documentCategory = "Invoice";
            meta.documentInvoiceType = "Standard";
            meta.einvoiceFulltext = "Fulltext";
            meta.hasPositionCorrection = false;
            meta.invoiceBusinessPartner = "Partner 1";
            meta.invoiceBusinessPartnerId = 1L;
            meta.invoiceDate = Instant.now();
            meta.invoiceNumber = "Invoice 1";
            meta.lastModifiedDatetime = Instant.now();
            meta.lastModifiedUserIdKey = "user-" + Math.random();
            meta.location = "Germany";
            meta.paidAt = Instant.now();
            meta.paidStatus = "Paid";
            meta.totalGrossAmount = 100.0;
            meta.uploaderScId = "Uploader-" + Math.random();
            meta.timeOfUpload = Instant.now();
            meta.documentApprovalState = "Approved";
            meta.transactionIds = "Trans-" + Math.random();
            meta.positions = List.of(Position.random(), Position.random());
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

        // Method to generate a random Position Object
        public static Position random() {
            // TODO implement random values
            Position res = new Position();
            res.note = "Sample Note";
            res.costCenter1 = "Costcenter-" + Math.random();
            res.costCenter2 = "Costcenter-" + Math.random();
            res.serviceDate = Instant.now();
            return res;
        }
    }
}
