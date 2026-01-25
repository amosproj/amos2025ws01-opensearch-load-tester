package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.config.DataGenerationProperties;
import com.opensearchloadtester.testdatagenerator.dao.OpenSearchDao;
import com.opensearchloadtester.testdatagenerator.exception.OpenSearchDataAccessException;
import com.opensearchloadtester.testdatagenerator.model.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.transport.httpclient5.ResponseException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataGenerationService {

    private final DataGenerator dataGenerator;
    private final OpenSearchDao openSearchDao;
    private final DataGenerationProperties dataGenerationProperties;

    public void generateAndIndexTestData(String indexName) {
        log.info("Starting test data generation");

        switch (dataGenerationProperties.getMode()) {
            case DYNAMIC -> executeDynamicMode(indexName);
            case PERSISTENT -> executePersistentMode(indexName);
        }

        log.info("Finished test data generation");
    }

    private void executeDynamicMode(String indexName) {
        int generatedDocs = 0;
        int batchSize = dataGenerationProperties.getBatchSize();
        int totalCount = dataGenerationProperties.getCount();

        while (generatedDocs < totalCount) {
            int remainingDocs = totalCount - generatedDocs;
            int currentBatchSize = Math.min(batchSize, remainingDocs);

            try {
                List<Document> documents = dataGenerator.generateData(
                        dataGenerationProperties.getDocumentType(),
                        currentBatchSize
                );

                openSearchDao.bulkIndexDocuments(indexName, documents);
            } catch (OpenSearchDataAccessException e) {
                Throwable cause = e.getCause();

                if (cause instanceof ResponseException responseException) {
                    int statusCode = responseException.status();

                    if (statusCode == 413 || statusCode == 429) {
                        int newBatchSize = Math.max(1, batchSize / 2);

                        log.warn("OpenSearch returned HTTP {}. Adjusted batch size from {} to {}",
                                statusCode, batchSize, newBatchSize);

                        batchSize = newBatchSize;

                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                }
                throw e;
            }

            generatedDocs += currentBatchSize;
            log.debug("Generated and indexed {}/{} documents", generatedDocs, totalCount);
        }
    }

    private void executePersistentMode(String indexName) {
        int offset = 0;
        int batchSize = dataGenerationProperties.getBatchSize();

        List<Document> documents = dataGenerator.generateData(
                dataGenerationProperties.getDocumentType(),
                dataGenerationProperties.getCount()
        );

        while (offset < documents.size()) {
            int remainingDocs = documents.size() - offset;
            int currentBatchSize = Math.min(batchSize, remainingDocs);

            try {
                List<Document> batch = documents.subList(offset, offset + currentBatchSize);
                openSearchDao.bulkIndexDocuments(indexName, batch);
            } catch (OpenSearchDataAccessException e) {
                Throwable cause = e.getCause();

                if (cause instanceof ResponseException responseException) {
                    int statusCode = responseException.status();

                    if (statusCode == 413 || statusCode == 429) {
                        int newBatchSize = Math.max(1, batchSize / 2);

                        log.warn("OpenSearch returned HTTP {}. Adjusted batch size from {} to {}",
                                statusCode, batchSize, newBatchSize);

                        batchSize = newBatchSize;

                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                }
                throw e;
            }

            offset += currentBatchSize;
            log.debug("Indexed {}/{} documents", offset, documents.size());
        }
    }
}
