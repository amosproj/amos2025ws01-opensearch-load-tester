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

    private static final long RETRY_DELAY_MS = 200;

    private final DataGenerator dataGenerator;
    private final OpenSearchDao openSearchDao;
    private final DataGenerationProperties dataGenerationProperties;

    public void generateAndIndexTestData(String indexName) {
        log.info("Starting test data generation (mode: {})", dataGenerationProperties.getMode());

        switch (dataGenerationProperties.getMode()) {
            case DYNAMIC -> generateDynamic(indexName);
            case PERSISTENT -> generatePersistent(indexName);
        }

        log.info("Finished test data generation");
    }

    private void generateDynamic(String indexName) {
        int generatedDocs = 0;
        int totalCount = dataGenerationProperties.getCount();
        int batchSize = dataGenerationProperties.getBatchSize();

        while (generatedDocs < totalCount) {
            int remainingDocs = totalCount - generatedDocs;
            int currentBatchSize = Math.min(batchSize, remainingDocs);

            try {
                List<Document> documents = dataGenerator.generateData(
                        dataGenerationProperties.getDocumentType(),
                        currentBatchSize
                );
                openSearchDao.bulkIndexDocuments(indexName, documents);
                generatedDocs += currentBatchSize;

                log.debug("Generated and indexed {}/{} documents", generatedDocs, totalCount);
            } catch (OpenSearchDataAccessException e) {
                batchSize = handleRetryOrThrow(e, batchSize);
            }
        }
    }

    private void generatePersistent(String indexName) {
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
                offset += currentBatchSize;

                log.debug("Indexed {}/{} documents", offset, documents.size());
            } catch (OpenSearchDataAccessException e) {
                batchSize = handleRetryOrThrow(e, batchSize);
            }
        }
    }

    /**
     * Handles retryable OpenSearch errors (413 / 429).
     * Returns the adjusted batch size or throws the exception if not retryable.
     */
    private int handleRetryOrThrow(OpenSearchDataAccessException e, int currentBatchSize) {
        Throwable cause = e.getCause();

        if (cause instanceof ResponseException responseException) {
            int status = responseException.status();

            if (status == 413 || status == 429) {
                int newBatchSize = Math.max(1, currentBatchSize / 2);

                log.warn("OpenSearch returned HTTP {}. Reducing batch size from {} to {}",
                        status, currentBatchSize, newBatchSize);

                try {
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }

                return newBatchSize;
            }
        }

        throw e;
    }
}
