package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.config.DataGenerationProperties.DocumentType;
import com.opensearchloadtester.testdatagenerator.model.Document;

import java.util.List;

/**
 * Interface for data generators.
 * A data generator generates test data for the OpenSearch instance
 */
public interface DataGenerator {

    /**
     * Generates a list of test data documents.
     *
     * @param count of documents to generate
     * @return list of Document objects
     */
    List<Document> generateData(DocumentType documentType, int count);
}
