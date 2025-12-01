package com.opensearchloadtester.testdatagenerator.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Random;
import java.util.UUID;

/**
 * Base class for OpenSearch document data.
 */
@Getter
@Setter
public abstract class AbstractDocument implements Document {

    /**
     * Common attributes of Ano and Duo documents.
     * Wrapper Classes are used for datatypes to ensure stability for null entries.
     */
    protected String id;
    protected String customAll;
    protected Long contentLength;
    protected String contentType;
    protected String dssCreationUserDisplayName;
    protected String dssCreationUserIdKey;
    protected String dssDataspaceId;
    protected String dssDocumentId;
    protected String dssDocumentName;
    protected Instant dssLastModifiedDatetime;
    protected String dssLastModifiedUserDisplayName;
    protected String dssLastModifiedUserIdKey;
    protected String dssProcessingFlagOwner;
    protected String dssVersion;
    protected String etag;
    protected Instant lastDocumentChange;

    protected static final Random RANDOM = new Random();


    protected static <T extends AbstractDocument> T fillCommonFieldsRandomly(T document) {
        // Fill with random values
        document.id = UUID.randomUUID().toString() + "_" + UUID.randomUUID().toString();
        document.customAll = "This is customAll from " + document.id;
        document.contentLength = RANDOM.nextLong(20000, 30000);
        document.contentType = "application/pdf";
        document.dssCreationUserDisplayName = "";
        document.dssCreationUserIdKey = UUID.randomUUID().toString();
        // first part of id
        document.dssDataspaceId = document.id.substring(0, document.id.indexOf('_'));
        // last part of id
        document.dssDocumentId =  document.id.substring(document.id.indexOf('_') + 1);

        // TODO: Hilfe :((
        document.dssDocumentName = "Brutto-Netto-Abrechnung " + document.dssDocumentId + ".pdf";

        document.dssLastModifiedDatetime = Instant.now();
        document.dssLastModifiedUserDisplayName = "";
        document.dssLastModifiedUserIdKey = UUID.randomUUID().toString();
        document.dssProcessingFlagOwner = "owner-random";
        document.dssVersion = "1." + RANDOM.nextInt(9);
        document.etag = UUID.randomUUID().toString().replace("-", "");
        // irrelevant, bc not queried on
        document.lastDocumentChange = Instant.now();
        return document;
    }
}
