package com.opensearchloadtester.testdatagenerator.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.opensearchloadtester.testdatagenerator.model.ano.AnoDocument;
import com.opensearchloadtester.testdatagenerator.model.duo.DuoDocument;

/**
 * Interface for OpenSearch Record Data.
 * A Record object stores all data from an OpenSearch Index in a Java Object.
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

    /**
     * Returns the Id of the record.
     *
     * @return Id of the record
     */
    //public String getId();

    /**
     * Generates a new Record filled with random values.
     * With this method a random Record object can be created.
     * Default: Creating random AnoRecord entry
     *
     * @return Record with random values
     */
    public static Document random(){
        return AnoDocument.random();
    }

}
