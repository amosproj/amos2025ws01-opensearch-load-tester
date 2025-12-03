package com.opensearchloadtester.testdatagenerator.model.duo;

import net.datafaker.Faker;

import com.opensearchloadtester.testdatagenerator.model.AbstractDocument;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.time.LocalDate;
import java.time.Instant;
import java.util.Locale;
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

        // Fill with random values
        duoDocument.ocrFulltext = OcrTextGenerator.generateOcrText();
        duoDocument.dssCustomMetadataDuo = DuoMetadata.random();

        duoDocument.dssDocumentName = "Rechnung " +
                duoDocument.dssCustomMetadataDuo.getInvoiceNumber() +
                " - " + duoDocument.dssCustomMetadataDuo.getInvoiceBusinessPartner() + ".pdf";

        duoDocument.customAll = duoDocument.dssCustomMetadataDuo.getInvoiceBusinessPartner() + " " +
                duoDocument.dssCustomMetadataDuo.getInvoiceNumber() + " " +
                duoDocument.ocrFulltext;
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
        private Integer invoiceBusinessPartnerId;
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

            // Fill with random values
            duoMetadata.bookingState = "TO_BOOK";
            duoMetadata.bookingStateChangedAt = null;
            duoMetadata.companyId = RANDOM.nextLong(100000);
            duoMetadata.currency = RANDOM.nextBoolean() ? "EUR" : "USD";
            duoMetadata.customerNumber = String.valueOf(faker.random().nextDouble() < 0.7
                    ? faker.number().numberBetween(1000, 1000000)
                    : null);
            duoMetadata.deletedAt = null;
            duoMetadata.documentType = RANDOM.nextInt(-4,4000);
            List<String> documentCategoryTypes = List.of("SUPPLIER_INVOICE", "OTHER", "SALES_INVOICE");
            duoMetadata.documentCategory = documentCategoryTypes.get(RANDOM.nextInt(documentCategoryTypes.size()));
            List<String> invTypes = List.of("null", "E_INVOICE", "OTHER");
            duoMetadata.documentInvoiceType = invTypes.get(RANDOM.nextInt(invTypes.size()));
            duoMetadata.hasPositionCorrection = false;
            // Set to null in ~30% of cases, otherwise a random long
            duoMetadata.invoiceBusinessPartnerId = faker.random().nextDouble() < 0.7
                    ? faker.number().numberBetween(1000, 1000000)
                    : null;
            duoMetadata.invoiceBusinessPartner = faker.company().name();
            duoMetadata.invoiceDate = faker.timeAndDate().past(3650, TimeUnit.DAYS);
            duoMetadata.invoiceNumber = RANDOM.nextInt(99999) + "/" + RANDOM.nextInt(9999);

            if ("E_INVOICE".equals(duoMetadata.documentInvoiceType)) {
                String sender = faker.internet().emailAddress();
                String recipient = faker.internet().emailAddress();
                String subject = faker.options().option(
                        "Rechnung " + duoMetadata.invoiceNumber,
                        "Ihre Rechnung " + duoMetadata.invoiceNumber + " von " + duoMetadata.invoiceBusinessPartner,
                        "Neue Rechnung: " + duoMetadata.invoiceNumber
                );
                String body = faker.options().option(
                        "Sehr geehrte Damen und Herren, " +
                                "anbei erhalten Sie unsere Rechnung " + duoMetadata.invoiceNumber + ". " +
                                "Mit freundlichen Grüßen,  " +
                                duoMetadata.invoiceBusinessPartner,
                        "Hallo, " +
                                "anbei die Rechnung " + duoMetadata.invoiceNumber + ". " +
                                "Viele Grüße,  " +
                                duoMetadata.invoiceBusinessPartner,
                        "Guten Tag, " +
                                "im Anhang finden Sie die Rechnung " + duoMetadata.invoiceNumber + " für Ihre Unterlagen.  " +
                                "Bei Fragen stehen wir Ihnen gerne zur Verfügung. " +
                                "Beste Grüße,  " +
                                duoMetadata.invoiceBusinessPartner
                );
                duoMetadata.einvoiceFulltext = "From: " + sender + "  " +
                        "To: " + recipient + "  " +
                        "Subject: " + subject + " " +
                        body;
            } else {
                duoMetadata.einvoiceFulltext = null;
            }
            duoMetadata.lastModifiedDatetime = faker.timeAndDate().past(90, TimeUnit.DAYS);

            int variant = faker.random().nextInt(3);
            switch (variant) {
                case 0 -> duoMetadata.lastModifiedUserIdKey = "rzId-not-set";
                case 1 -> duoMetadata.lastModifiedUserIdKey = java.util.UUID.randomUUID().toString();
                default ->
                        duoMetadata.lastModifiedUserIdKey = faker.number().numberBetween(0, 1000) + "@sca.dt3v.de";
            }

            List<String> loc = List.of("BELEGE", "BELEGFREIGABE");
            duoMetadata.location = loc.get(RANDOM.nextInt(loc.size()));
            List<String> states = List.of("NOT_PAID", "FULLY_PAID");
            duoMetadata.paidStatus = states.get(RANDOM.nextInt(states.size()));
            duoMetadata.paidAt = "FULLY_PAID".equals(duoMetadata.paidStatus)
                    ? faker.timeAndDate().past(180, TimeUnit.DAYS)
                    : null;

            List<Position> pos = new ArrayList<>();
            // From examples always 0 positions
            int numPos = RANDOM.nextInt(0,1);
            for (int i = 0; i < numPos; i++) {
                pos.add(Position.random());
            }
            duoMetadata.positions = pos;
            duoMetadata.totalGrossAmount = RANDOM.nextDouble(10000000);
            duoMetadata.uploaderScId = RANDOM.nextInt(1000) + "@sca.dt3v.de";
            duoMetadata.timeOfUpload =  faker.timeAndDate().past(3560, TimeUnit.DAYS);
            List<String> appStates = List.of("APPROVED", "NOT_RELEVANT", "UNDISPATCHED");
            duoMetadata.documentApprovalState = appStates.get(RANDOM.nextInt(appStates.size()));
            // From examples
            duoMetadata.transactionIds = "[]";
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
            position.serviceDate = faker.timeAndDate().past(365, TimeUnit.DAYS);
            return position;
        }
    }

    /**
     * Nested Class for OCR Fulltext of DuoDocument
     */
    @Getter
    @Setter
    public static class OcrTextGenerator {

        private static final DecimalFormat df = new DecimalFormat("#,##0.00");
        private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        public static String generateOcrText() {
            StringBuilder sb = new StringBuilder();

            // Invoice Header
            String sellerCompany = faker.company().name();
            String sellerStreet = faker.address().streetAddress();
            String sellerCity = faker.address().zipCode() + " " + faker.address().city();
            String sellerTaxId = faker.numerify("DE#########");

            String buyerCompany = faker.random().nextBoolean() ? faker.company().name() : faker.name().fullName();
            String buyerStreet = faker.address().streetAddress();
            String buyerCity = faker.address().zipCode() + " " + faker.address().city();

            // Header assemble
            sb.append(sellerCompany).append(" ").append(sellerStreet).append(" ").append(sellerCity).append(" ");
            sb.append(buyerCompany).append(" ");
            sb.append(buyerStreet).append(" ");
            sb.append(buyerCity).append(" ");

            // Invoice Data
            LocalDate date = LocalDate.now().minusDays(faker.number().numberBetween(0, 3560));
            String invoiceNr = String.valueOf(faker.number().randomNumber());

            sb.append("RECHNUNG Nr. ").append(invoiceNr).append(" ");
            sb.append("Datum: ").append(date.format(dtf)).append(" ");
            // Sometimes Due Date for Payment
            if (faker.random().nextBoolean()) {
                sb.append("Leistungsdatum: ").append(date.minusDays(faker.number().numberBetween(1, 10)).format(dtf)).append(" ");
            }
            sb.append(" ");

            // From Examples
            String intro;
            int introVariant = faker.number().numberBetween(0, 6);
            switch (introVariant) {
                case 0 -> intro = "Für die Lieferung/Leistung berechnen wir Ihnen:";
                case 1 -> intro = "Gemäß Auftrag stellen wir folgende Positionen in Rechnung:";
                case 2 -> intro = "Vielen Dank für Ihr Vertrauen. Hiermit stellen wir Ihnen folgende Leistungen in Rechnung:";
                case 3 -> intro = "Für die Lieferung vom " + date.format(dtf) + " berechnen wir:";
                case 4 -> intro = "Geliefert wurden am " + date.format(dtf) + ":";
                case 5 -> intro = "Verbindungsnachweis für den Zeitraum " + date.getMonthValue() + "/" + date.getYear() + ":";
                default -> intro = "Rechnungspositionen:";
            }
            sb.append(intro).append(" ");

            // Invoice Items
            sb.append("Bezeichnung Einzelpreis Menge Gesamt ");

            double totalNet = 0.0;
            int itemCount = faker.number().numberBetween(1, 20);

            for (int i = 0; i < itemCount; i++) {
                String item;
                int itemType = faker.number().numberBetween(0, 3);
                // FRom examples
                switch (itemType) {
                    case 0 -> item = faker.commerce().productName();
                    case 1 -> item = faker.commerce().material();
                    default -> item = "Service: " + faker.company().profession();
                }

                int qty = faker.number().numberBetween(1, 50);
                double price = faker.number().randomDouble(2, 5, 200);
                double lineTotal = qty * price;
                totalNet += lineTotal;

                sb.append(item).append(" ");
                sb.append(df.format(price)).append(" EUR ");
                sb.append(qty).append(" Stk ");
                sb.append(df.format(lineTotal)).append(" EUR ");
            }
            sb.append(" ");


            double taxRate = 0.19; // Standard 19%
            double taxAmount = totalNet * taxRate;
            double totalGross = totalNet + taxAmount;

            sb.append("Nettosumme: ").append(df.format(totalNet)).append(" EUR ");
            sb.append("zzgl. 19% MwSt: ").append(df.format(taxAmount)).append(" EUR ");
            sb.append("Gesamtbetrag: ").append(df.format(totalGross)).append(" EUR ");

            // Footer
            LocalDate dueDate = date.plusDays(faker.number().numberBetween(7, 30));
            sb.append("Zahlbar ohne Abzug bis zum ").append(dueDate.format(dtf)).append(". ");

            sb.append("Bankverbindung: ");
            sb.append("Bank: ").append(faker.company().name()).append(" Bank ");
            sb.append("IBAN: ").append(faker.finance().iban("DE")).append(" ");
            sb.append("BIC:  ").append(faker.finance().bic()).append(" ");
            sb.append("USt-IdNr.: ").append(sellerTaxId).append(" ");
            sb.append("Steuernummer: ").append(faker.numerify("###/###/#####"));

            return sb.toString();
        }
    }
}
