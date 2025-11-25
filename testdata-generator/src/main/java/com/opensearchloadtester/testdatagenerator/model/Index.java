package com.opensearchloadtester.testdatagenerator.model;

import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.indices.IndexSettings;

/**
 * An Index represents an OpenSearch index.
 */
public interface Index {

    String getName();

    IndexSettings getSettings();

    TypeMapping getMapping();
}
