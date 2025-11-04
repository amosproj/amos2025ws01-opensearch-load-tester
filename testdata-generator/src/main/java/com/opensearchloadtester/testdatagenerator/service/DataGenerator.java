package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.model.Recordable;

import java.util.List;

/**
 * Interface for data generators.
 * A data generator generates test data for the OpenSearch instance
 */
public interface DataGenerator {

    /**
     * Generates a list of test data records.
     *
     * @param count of records to generate
     * @return list of Recordable objects
     */
    List<Recordable> generateData(int count);
}