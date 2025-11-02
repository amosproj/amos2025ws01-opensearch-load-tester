package com.opensearchloadtester.testdatagenerator.model;

import com.opensearchloadtester.testdatagenerator.model.AnoRecord;

/**
 * Interface for OpenSearch Record Data.
 * A Record object stores all data from an OpenSearch Index in a Java Object.
 */
public interface Recordable {

    /**
     * Returns the Id of the record.
     *
     * @return Id of the record
     */
    public String getId();

    /**
     * Generate a new Record filled with random values.
     * With this method a random Record object can be created.
     * Default: Creating random AnoRecord entry
     *
     * @return Record with random values
     */
    public static Recordable random(){
        return AnoRecord.random();
    }

}