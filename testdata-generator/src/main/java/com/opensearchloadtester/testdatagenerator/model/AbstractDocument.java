package com.opensearchloadtester.testdatagenerator.model;

import net.datafaker.Faker;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.util.Locale;
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
    protected static final Faker faker = new Faker(Locale.GERMAN);

    protected static <T extends AbstractDocument> T fillCommonFieldsRandomly(T document) {
        // Fill with random values
        document.id = UUID.randomUUID().toString() + "_" + UUID.randomUUID().toString();
        document.contentLength = RANDOM.nextLong(20000, 30000);
        document.contentType = "application/pdf";
        document.dssCreationUserDisplayName = faker.name().fullName();
        document.dssCreationUserIdKey = UUID.randomUUID().toString();
        // first part of id
        document.dssDataspaceId = document.id.substring(0, document.id.indexOf('_'));
        // last part of id
        document.dssDocumentId =  document.id.substring(document.id.indexOf('_') + 1);


        document.dssLastModifiedDatetime = faker.timeAndDate().past(365, TimeUnit.DAYS);
        document.dssLastModifiedUserDisplayName = faker.name().fullName();
        document.dssLastModifiedUserIdKey = UUID.randomUUID().toString();
        document.dssProcessingFlagOwner = faker.name().nameWithMiddle();
        document.dssVersion = "1." + RANDOM.nextInt(9);
        document.etag = UUID.randomUUID().toString().replace("-", "");
        document.lastDocumentChange = faker.timeAndDate().past(90, TimeUnit.DAYS);
        return document;
    }
}
