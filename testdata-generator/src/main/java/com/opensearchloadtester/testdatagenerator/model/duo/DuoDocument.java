package com.opensearchloadtester.testdatagenerator.model.duo;

import com.opensearchloadtester.testdatagenerator.model.AbstractDocument;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PRIVATE) // DuoDocument objects must be created via random() method
public class DuoDocument extends AbstractDocument {

    /**
     * Attributes of a DuoDocument correspond to the data stored in an OpenSearch index.
     * Wrapper Classes are used for datatypes to ensure stability for null entries.
     */
    private String ocrFulltext;
    private DuoMetadata dssCustomMetadataDuo;

    // Method to create a random DuoDocument object
    public static DuoDocument random() {
        DuoDocument duoDocument = fillCommonFieldsRandomly(new DuoDocument());

        // Fill with random values without purpose
        duoDocument.ocrFulltext = "This is OCR fulltext from " + duoDocument.id;
        duoDocument.dssCustomMetadataDuo = DuoMetadata.random();
        return duoDocument;
    }

    /**
     * Nested Class for DuoMetadata of DuoDocument
     */
    @Getter
    @Setter
    public static class DuoMetadata {

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


        // Method to create a random DuoMetadata object
        public static DuoMetadata random() {
            DuoMetadata duoMetadata = new DuoMetadata();

            // Fill with random values without purpose
            List<String> states = List.of("Open", "Paid", "Closed");
            duoMetadata.bookingState = states.get(RANDOM.nextInt(states.size()));
            duoMetadata.bookingStateChangedAt = Instant.now();
            duoMetadata.companyId = RANDOM.nextLong(10000);
            List<String> curr = List.of("EUR", "USD");
            duoMetadata.currency = curr.get(RANDOM.nextInt(curr.size()));
            duoMetadata.customerNumber = "customer-" + RANDOM.nextInt(10000);
            duoMetadata.deletedAt = Instant.MAX;
            duoMetadata.documentType = RANDOM.nextInt(6);
            duoMetadata.documentCategory = "Invoice";
            List<String> invTypes = List.of("Standard", "Urgent", "Reminder");
            duoMetadata.documentInvoiceType = invTypes.get(RANDOM.nextInt(invTypes.size()));
            duoMetadata.einvoiceFulltext = "This is fulltext of invoice " + duoMetadata.invoiceNumber;
            duoMetadata.hasPositionCorrection = false;
            duoMetadata.invoiceBusinessPartnerId = RANDOM.nextLong(10000);
            duoMetadata.invoiceBusinessPartner = "partner-" + duoMetadata.invoiceBusinessPartnerId.toString();
            duoMetadata.invoiceDate = Instant.now();
            duoMetadata.invoiceNumber = "inv-" + RANDOM.nextInt(10000);
            duoMetadata.lastModifiedDatetime = Instant.now();
            duoMetadata.lastModifiedUserIdKey = "user-random";
            List<String> loc = List.of("Germany", "England", "Spain", "France");
            duoMetadata.location = loc.get(RANDOM.nextInt(loc.size()));
            duoMetadata.paidAt = Instant.now();
            duoMetadata.paidStatus = states.get(RANDOM.nextInt(states.size()));
            List<Position> pos = new ArrayList<>();
            int numPos = RANDOM.nextInt(50);
            for (int i = 0; i < numPos; i++) {
                pos.add(Position.random());
            }
            duoMetadata.positions = pos;
            duoMetadata.totalGrossAmount = RANDOM.nextDouble(10000000);
            duoMetadata.uploaderScId = "up-" + RANDOM.nextInt(1000);
            duoMetadata.timeOfUpload = Instant.now();
            List<String> appStates = List.of("Approved", "Not approved", "In progress");
            duoMetadata.documentApprovalState = appStates.get(RANDOM.nextInt(appStates.size()));
            duoMetadata.transactionIds = "trans-" + RANDOM.nextInt(10000000);
            return duoMetadata;
        }
    }

    /**
     * Nested Class for Positions of DuoMetadata
     */
    @Getter
    @Setter
    public static class Position {

        private String note;
        private String costCenter1;
        private String costCenter2;
        private Instant serviceDate;


        // Method to generate a random Position object
        public static Position random() {
            Position position = new Position();

            // Fill with random values without purpose
            position.note = "This is a sample note " + RANDOM.nextInt(10000);
            position.costCenter1 = "cc-" + RANDOM.nextInt(10000);
            position.costCenter2 = "cc-" + RANDOM.nextInt(10000);
            position.serviceDate = Instant.now();
            return position;
        }
    }
}
