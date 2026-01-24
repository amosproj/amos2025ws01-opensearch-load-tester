package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.config.DataGenerationProperties;
import com.opensearchloadtester.testdatagenerator.model.Document;
import com.opensearchloadtester.testdatagenerator.model.DocumentType;
import com.opensearchloadtester.testdatagenerator.model.Index;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataGenerationService {

    private static final int MIN_BATCH_SIZE = 1_000;
    private static final int TARGET_BATCHES = 20;

    private final DataGenerator dataGenerator;
    private final OpenSearchDataService openSearchDataService;

    public void generateAndIndexTestData(DataGenerationProperties dataGenerationProperties, Index index) {

        switch (dataGenerationProperties.getMode()) {
            case DYNAMIC -> preloadDynamic(dataGenerationProperties.getDocumentType(),
                    dataGenerationProperties.getCount(),
                    index);
            case PERSISTENT -> preloadPersistent(dataGenerationProperties.getDocumentType(),
                    dataGenerationProperties.getCount(),
                    index);
        }
    }

    private int dynamicMaxBatchSize(int totalCount) {
        if (totalCount <= 200_000) return 10_000;
        if (totalCount <= 1_000_000) return 15_000;
        return 35_000;
    }

    /**
     * Computes a batch size from totalCount aiming for ~TARGET_BATCHES batches,
     * clamped to [min(totalCount, MIN_BATCH_SIZE), dynamicMaxBatchSize(totalCount)].
     */
    private int resolveBatchSize(int totalCount) {
        int maxBatchSize = dynamicMaxBatchSize(totalCount);

        int computed = (int) Math.ceil((double) totalCount / TARGET_BATCHES);

        // Clamp to at least MIN_BATCH_SIZE, but never exceed totalCount (small datasets)
        int minBatch = Math.min(MIN_BATCH_SIZE, totalCount);
        if (computed < minBatch) computed = minBatch;

        // Clamp to dynamic max
        if (computed > maxBatchSize) computed = maxBatchSize;

        return computed;
    }

    private void preloadDynamic(DocumentType documentType, int totalCount, Index index) {
        final int initialBatchSize = resolveBatchSize(totalCount);

        log.debug(
                "Starting DYNAMIC batch pre-loading. documentType={}, totalCount={}, batchSize={}",
                documentType,
                totalCount,
                initialBatchSize
        );

        preloadLoop(documentType, index, totalCount, initialBatchSize, null);

        log.info("Finished DYNAMIC batch pre-loading successfully for index='{}' (count={})",
                index.getName(), totalCount);
    }

    private void preloadPersistent(DocumentType documentType, int totalCount, Index index) {
        final int initialBatchSize = resolveBatchSize(totalCount);

        log.info(
                "Starting PERSISTENT batch pre-loading. documentType={}, totalCount={}, batchSize={}",
                documentType,
                totalCount,
                initialBatchSize
        );

        List<Document> allDocuments = dataGenerator.generateData(
                documentType,
                totalCount
        );

        if (allDocuments.size() != totalCount) {
            throw new IllegalStateException(String.format(
                    "Persistent dataset size (%d) does not match configured count (%d). Aborting preload.",
                    allDocuments.size(), totalCount
            ));
        }
        preloadLoop(documentType, index, totalCount, initialBatchSize, allDocuments.iterator());

        log.info(
                "Finished PERSISTENT batch pre-loading successfully for index='{}' (count={})",
                index.getName(),
                totalCount
        );
    }


    private void preloadLoop(DocumentType documentType, Index index, int totalCount, int initialBatchSize, Iterator<Document> persistentIt) {
        int batchNumber = 0;
        int generatedSoFar = 0;
        int effectiveBatchSize = initialBatchSize;

        while (generatedSoFar < totalCount) {
            batchNumber++;

            int remaining = totalCount - generatedSoFar;

            int dynamicLimit = openSearchDataService.getDynamicBatchLimit();
            if (dynamicLimit < effectiveBatchSize) {
                int adapted = Math.max(dynamicLimit, MIN_BATCH_SIZE);
                if (adapted != effectiveBatchSize) {
                    log.debug(
                            "Adapting dynamic batch size from {} to {} based on OpenSearch feedback (remaining={})",
                            effectiveBatchSize,
                            adapted,
                            remaining
                    );
                    effectiveBatchSize = adapted;
                }
            }
            int currentBatchSize = Math.min(effectiveBatchSize, remaining);

            List<Document> batch;
            if (persistentIt == null) {
                batch = dataGenerator.generateData(documentType, currentBatchSize);
            } else {
                batch = new ArrayList<>(currentBatchSize);
                while (persistentIt.hasNext() && batch.size() < currentBatchSize) {
                    batch.add(persistentIt.next());
                }
            }

            if (batch.size() != currentBatchSize) {
                throw new IllegalStateException(String.format(
                        "Generator returned %d documents for batch %d but expected %d. Aborting preload.",
                        batch.size(), batchNumber, currentBatchSize
                ));
            }

            openSearchDataService.bulkIndexDocuments(index.getName(), batch);
            generatedSoFar += currentBatchSize;

            log.debug(
                    "Finished batch {}. generatedSoFar={}/{}",
                    batchNumber,
                    generatedSoFar,
                    totalCount
            );
        }
    }
}
