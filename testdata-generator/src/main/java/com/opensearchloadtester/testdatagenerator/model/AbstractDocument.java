package com.opensearchloadtester.testdatagenerator.model;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Random;

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
        // Fill with random values without purpose
        document.id = "id-" + RANDOM.nextInt(100000);
        document.customAll = "This is customAll from " + document.id;
        document.contentLength = RANDOM.nextLong(10000);
        document.contentType = "Document";
        document.dssCreationUserDisplayName = "RandomGenerator";
        document.dssCreationUserIdKey = "user-random";
        document.dssDataspaceId = "dataspace-" + RANDOM.nextInt(1001);
        document.dssDocumentId = "document-" + RANDOM.nextInt(10000);
        document.dssDocumentName = "Payroll-" + document.dssDocumentId + "-random";
        document.dssLastModifiedDatetime = Instant.now();
        document.dssLastModifiedUserDisplayName = "RandomGenerator";
        document.dssLastModifiedUserIdKey = "user-random";
        document.dssProcessingFlagOwner = "owner-random";
        document.dssVersion = "1.0";
        document.etag = "etag-" + RANDOM.nextInt(100000000);
        document.lastDocumentChange = Instant.now();
        return document;
    }
}
