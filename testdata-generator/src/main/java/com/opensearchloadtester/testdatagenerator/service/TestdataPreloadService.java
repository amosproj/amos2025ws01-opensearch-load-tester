package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.TestdataInitializer;
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
 * <p>
 * This service is responsible for generating test data (either dynamically or
 * from a persistent data set) and indexing it into OpenSearch in batches.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestdataPreloadService {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;
    private static final int MAX_SPLIT_DEPTH = 4;

    private static final int MIN_BATCH_SIZE = 1_000;
    private static final int TARGET_BATCHES = 20;

    private final DataGenerationProperties dataGenerationProperties;
    private final DataGenerator dataGenerator;
    private final OpenSearchDataService openSearchDataService;

    private int dynamicMaxBatchSize(int totalCount) {
        if (totalCount <= 200_000) {
            return 10_000;        // small
        } else if (totalCount <= 1_000_000) {
            return 15_000;        // mid-size
        } else {
            return 25_000;        // bigger dataset
        }
    }

    /**
     * Entry point for pre-loading test data into OpenSearch in batches.
     * <p>
     * This is called once at application startup by {@link TestdataInitializer}.
     * The actual behavior depends on the configured mode:
     * <ul>
     *     <li>DYNAMIC – documents are generated freshly on each startup.</li>
     *     <li>PERSISTENT – documents are generated once and stored on disk,
     *     subsequent runs reuse the stored data.</li>
     * </ul>
     */
    public void preloadTestdata() {
        switch (dataGenerationProperties.getMode()) {
            case DYNAMIC -> preloadDynamic();
            case PERSISTENT -> preloadPersistent();
        }
    }

    /**
     * Resolves the effective batchSize:
     * <ul>
     *     <li>If a valid batchSize is configured (>0 and ≤ totalCount), it is used as-is.</li>
     *     <li>Otherwise a value is computed based on totalCount and clamped between
     *     MIN_BATCH_SIZE and MAX_BATCH_SIZE.</li>
     * </ul>
     */
    private int resolveBatchSize(int totalCount) {
        if (totalCount <= 0) {
            throw new IllegalArgumentException("totalCount must be > 0, but was " + totalCount);
        }

        int configured = dataGenerationProperties.getBatchSize();


        int MAX_BATCH_SIZE = dynamicMaxBatchSize(dataGenerationProperties.getCount());

        // 2) Auto-mode (no env): compute a reasonable batch size
        //    so that we get roughly TARGET_BATCHES batches, clamped to [MIN, MAX].
        int computed = (int) Math.ceil((double) totalCount / TARGET_BATCHES);

        if (computed < MIN_BATCH_SIZE) {
            // For small datasets, we don't want super tiny batches.
            computed = Math.min(MIN_BATCH_SIZE, totalCount);
        }
        if (computed > MAX_BATCH_SIZE) {
            computed = MAX_BATCH_SIZE;
        }

        log.info(
                "Auto-computed batchSize={} for totalCount={} (configured={}, env={})",
                computed,
                totalCount,
                configured
        );

        return computed;
    }


    /**
     * Dynamic mode: generates new random documents for each run and
     * indexes them into OpenSearch in batches.
     */
    private void preloadDynamic() {
        // 1) Select ANO / DUO index
        final Index index = switch (dataGenerationProperties.getDocumentType()) {
            case ANO -> AnoIndex.getInstance();
            case DUO -> DuoIndex.getInstance();
        };

        final int totalCount = dataGenerationProperties.getCount();
        final int batchSize = resolveBatchSize(totalCount);

        log.info(
                "Starting DYNAMIC batch pre-loading. documentType={}, totalCount={}, batchSize={}",
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

        // 3) Generate and index in batches
        while (generatedSoFar < totalCount) {
            batchNumber++;

            int remaining = totalCount - generatedSoFar;
            int currentBatchSize = Math.min(batchSize, remaining);

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
                log.warn(
                        "Generator returned {} documents for batch {} but expected {} (DYNAMIC)",
                        batchDocuments.size(),
                        batchNumber,
                        currentBatchSize
                );
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

        // 4) Refresh index at the end
        openSearchDataService.refreshIndex(index.getName());

        log.info("Finished DYNAMIC batch pre-loading successfully for index='{}'", index.getName());
    }

    /**
     * Persistent mode:
     * <ul>
     *     <li>First run: generates random documents, stores them to disk, then indexes them.</li>
     *     <li>Subsequent runs: loads the existing documents from disk and re-indexes them.</li>
     * </ul>
     */
    private void preloadPersistent() {
        final Index index = switch (dataGenerationProperties.getDocumentType()) {
            case ANO -> AnoIndex.getInstance();
            case DUO -> DuoIndex.getInstance();
        };

        final int totalCount = dataGenerationProperties.getCount();
        final int batchSize = resolveBatchSize(totalCount);

        log.info(
                "Starting PERSISTENT batch pre-loading. documentType={}, totalCount={}, batchSize={}",
                dataGenerationProperties.getDocumentType(),
                totalCount,
                batchSize
        );

        // 1) Create index if needed
        openSearchDataService.createIndex(
                index.getName(),
                index.getSettings(),
                index.getMapping()
        );

        // 2) Obtain the full set of documents from PersistentDataGenerator.
        List<Document> allDocuments = dataGenerator.generateData(
                dataGenerationProperties.getDocumentType(),
                totalCount
        );

        if (allDocuments.size() != totalCount) {
            log.warn(
                    "Persistent dataset size ({}) does not match configured count ({})",
                    allDocuments.size(),
                    totalCount
            );
        }

        int batchNumber = 0;
        int indexedSoFar = 0;

        // 3) In-memory batching
        for (int i = 0; i < allDocuments.size(); i += batchSize) {
            batchNumber++;
            int end = Math.min(i + batchSize, allDocuments.size());
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
        }

        // 4) Final refresh
        openSearchDataService.refreshIndex(index.getName());

        log.info(
                "Finished PERSISTENT batch pre-loading successfully for index='{}'. indexedSoFar={}",
                index.getName(),
                indexedSoFar
        );
    }
}
