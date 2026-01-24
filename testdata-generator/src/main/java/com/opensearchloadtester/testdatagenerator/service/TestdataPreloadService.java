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

@Slf4j
@Service
@RequiredArgsConstructor
public class TestdataPreloadService {

    private static final int MIN_BATCH_SIZE = 1_000;
    private static final int TARGET_BATCHES = 20;

    private final DataGenerationProperties dataGenerationProperties;
    private final DataGenerator dataGenerator;
    private final OpenSearchDataService openSearchDataService;

    public void preloadTestdata() {
        // Defensive: @Min(1) should guarantee this, but keep it safe for tests/miswiring
        int totalCount = dataGenerationProperties.getCount();
        if (totalCount <= 0) {
            throw new IllegalArgumentException("data.generation.count must be > 0, but was " + totalCount);
        }

        switch (dataGenerationProperties.getMode()) {
            case DYNAMIC -> preloadDynamic(totalCount);
            case PERSISTENT -> preloadPersistent(totalCount);
        }
    }

    private Index resolveIndex() {
        return switch (dataGenerationProperties.getDocumentType()) {
            case ANO -> AnoIndex.getInstance();
            case DUO -> DuoIndex.getInstance();
        };
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

    private void preloadDynamic(int totalCount) {
        final Index index = resolveIndex();
        final int initialBatchSize = resolveBatchSize(totalCount);

        log.debug(
                "Starting DYNAMIC batch pre-loading. documentType={}, totalCount={}, batchSize={}",
                dataGenerationProperties.getDocumentType(),
                totalCount,
                initialBatchSize
        );

        openSearchDataService.createIndex(index.getName(), index.getSettings(), index.getMapping());

        int batchNumber = 0;
        int generatedSoFar = 0;

        // Batch size can adapt across iterations
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

            log.debug(
                    "Starting batch {} (DYNAMIC): generating {} documents (remaining={})",
                    batchNumber,
                    currentBatchSize,
                    remaining
            );

            List<Document> batchDocuments = dataGenerator.generateData(
                    dataGenerationProperties.getDocumentType(),
                    currentBatchSize
            );

            if (batchDocuments.size() != currentBatchSize) {
                throw new IllegalStateException(String.format(
                        "Generator returned %d documents for batch %d but expected %d (DYNAMIC). Aborting preload.",
                        batchDocuments.size(), batchNumber, currentBatchSize
                ));
            }

            openSearchDataService.bulkIndexDocuments(index.getName(), batchDocuments);
            generatedSoFar += currentBatchSize;

            log.debug(
                    "Finished batch {} (DYNAMIC). generatedSoFar={}/{}",
                    batchNumber,
                    generatedSoFar,
                    totalCount
            );
        }

        openSearchDataService.refreshIndex(index.getName());

        log.info("Finished DYNAMIC batch pre-loading successfully for index='{}' (count={})",
                index.getName(), totalCount);
    }

    private void preloadPersistent(int totalCount) {
        final Index index = resolveIndex();
        final int initialBatchSize = resolveBatchSize(totalCount);

        log.info(
                "Starting PERSISTENT batch pre-loading. documentType={}, totalCount={}, batchSize={}",
                dataGenerationProperties.getDocumentType(),
                totalCount,
                initialBatchSize
        );

        openSearchDataService.createIndex(index.getName(), index.getSettings(), index.getMapping());

        List<Document> allDocuments = dataGenerator.generateData(
                dataGenerationProperties.getDocumentType(),
                totalCount
        );

        if (allDocuments.size() != totalCount) {
            throw new IllegalStateException(String.format(
                    "Persistent dataset size (%d) does not match configured count (%d). Aborting preload.",
                    allDocuments.size(), totalCount
            ));
        }

        int batchNumber = 0;
        int indexedSoFar = 0;

        // Must persist across batches
        int effectiveBatchSize = initialBatchSize;

        int i = 0;
        while (i < allDocuments.size()) {
            batchNumber++;

            int remaining = allDocuments.size() - i;

            int dynamicLimit = openSearchDataService.getDynamicBatchLimit();
            if (dynamicLimit < effectiveBatchSize) {
                int adapted = Math.max(dynamicLimit, MIN_BATCH_SIZE);
                if (adapted != effectiveBatchSize) {
                    log.debug(
                            "Adapting persistent batch size from {} to {} based on OpenSearch feedback (remaining={})",
                            effectiveBatchSize,
                            adapted,
                            remaining
                    );
                    effectiveBatchSize = adapted;
                }
            }

            int currentBatchSize = Math.min(effectiveBatchSize, remaining);
            int end = i + currentBatchSize;

            List<Document> batch = allDocuments.subList(i, end);

            log.debug(
                    "Indexing batch {} (PERSISTENT): size={} (docs {}–{})",
                    batchNumber,
                    batch.size(),
                    i,
                    end - 1
            );

            openSearchDataService.bulkIndexDocuments(index.getName(), batch);

            indexedSoFar += batch.size();
            i = end;
        }

        openSearchDataService.refreshIndex(index.getName());

        if (indexedSoFar != totalCount) {

            throw new IllegalStateException(String.format(
                    "Indexed count mismatch (PERSISTENT): indexedSoFar=%d, expected=%d",
                    indexedSoFar, totalCount
            ));
        }

        log.info(
                "Finished PERSISTENT batch pre-loading successfully for index='{}' (count={})",
                index.getName(),
                totalCount
        );
    }
}
