package com.opensearchloadtester.testdatagenerator.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.opensearchloadtester.testdatagenerator.model.ano.AnoDocument;
import com.opensearchloadtester.testdatagenerator.model.duo.DuoDocument;

/**
 * A Document represents a single record stored in an OpenSearch index.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = AnoDocument.class, name = "ano"),
        @JsonSubTypes.Type(value = DuoDocument.class, name = "duo")
})
public interface Document {
}
