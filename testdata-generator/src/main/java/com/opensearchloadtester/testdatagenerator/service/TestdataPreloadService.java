package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.config.DataGenerationProperties;
import com.opensearchloadtester.testdatagenerator.model.Document;
import com.opensearchloadtester.testdatagenerator.model.Index;
import com.opensearchloadtester.testdatagenerator.model.ano.AnoIndex;
import com.opensearchloadtester.testdatagenerator.model.duo.DuoIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Batch pre-loading job for ANO / DUO test data.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestdataPreloadService {

    private final DataGenerationProperties dataGenerationProperties;
    private final DataGenerator dataGenerator;
    private final OpenSearchDataService openSearchDataService;

    public void preloadTestdata() {
        // 1) Decide ANO / DUO index
        final Index index = switch (dataGenerationProperties.getDocumentType()) {
            case ANO -> AnoIndex.getInstance();
            case DUO -> DuoIndex.getInstance();
        };

        final int totalCount = dataGenerationProperties.getCount();
        final int batchSize = dataGenerationProperties.getBatchSize();

        if (totalCount <= 0) {
            log.warn("Total document count is <= 0 ({}). Skipping pre-load.", totalCount);
            return;
        }

        if (batchSize <= 0) {
            throw new IllegalArgumentException("batchSize must be > 0, but was " + batchSize);
        }

        log.info(
                "Starting batch pre-loading in '{}' mode. documentType={}, totalCount={}, batchSize={}",
                dataGenerationProperties.getMode(),
                dataGenerationProperties.getDocumentType(),
                totalCount,
                batchSize
        );

        // 2) Create index if needed
        openSearchDataService.createIndex(
                index.getName(),
                index.getSettings(),
                index.getMapping()
        );

        int batchNumber = 0;
        int generatedSoFar = 0;

        // 3) Generate & index in batches
        while (generatedSoFar < totalCount) {
            batchNumber++;

            int remaining = totalCount - generatedSoFar;
            int currentBatchSize = Math.min(batchSize, remaining);

            log.debug(
                    "Starting batch {}: generating {} documents (remaining={})",
                    batchNumber,
                    currentBatchSize,
                    remaining
            );

            // Generate this batch of documents
            List<Document> batchDocuments = dataGenerator.generateData(
                    dataGenerationProperties.getDocumentType(),
                    currentBatchSize
            );

            // Index batch with retry/skip logic
            indexBatchWithRetry(index.getName(), batchNumber, batchDocuments);

            generatedSoFar += currentBatchSize;

            log.debug(
                    "Finished batch {}. generatedSoFar={}/{}",
                    batchNumber,
                    generatedSoFar,
                    totalCount
            );
        }

        // 4) Refresh index at the end
        openSearchDataService.refreshIndex(index.getName());

        log.debug("Finished batch pre-loading successfully for index='{}'", index.getName());
    }

    private void indexBatchWithRetry(String indexName,
                                     int batchNumber,
                                     List<Document> batchDocuments) {

        int maxAttempts = 3;
        int attempt = 0;
        boolean success = false;

        while (!success && attempt < maxAttempts) {
            attempt++;
            try {
                log.debug(
                        "Indexing batch {} into index '{}' (attempt {}/{}, size={})",
                        batchNumber,
                        indexName,
                        attempt,
                        maxAttempts,
                        batchDocuments.size()
                );

                openSearchDataService.bulkIndexDocuments(indexName, batchDocuments);
                success = true;

            } catch (Exception e) {
                log.warn(
                        "Batch {} for index '{}' failed on attempt {}/{}: {}",
                        batchNumber,
                        indexName,
                        attempt,
                        maxAttempts,
                        e.getMessage(),
                        e
                );
            }

            if (!success && attempt >= maxAttempts) {
                log.error(
                        "Giving up on batch {} for index '{}'. action=SKIP",
                        batchNumber,
                        indexName
                );
            }
        }
    }
}
