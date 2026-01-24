package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.model.Document;
import com.opensearchloadtester.testdatagenerator.model.DocumentType;
import com.opensearchloadtester.testdatagenerator.model.ano.AnoDocument;
import com.opensearchloadtester.testdatagenerator.model.duo.DuoDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Generates and stores data persistently.
 * On subsequent runs, existing data is reused unless deleted manually.
 */
@Slf4j
@RequiredArgsConstructor
public class PersistentDataGenerator implements DataGenerator {

    private final FileStorageService storageService;
    private final DynamicDataGenerator dynamicDataGenerator;

    /**
     * Loads existing data from persistent file.
     * If there is no data available, new random data is generated
     */
    @Override
    public List<Document> generateData(DocumentType documentType, int count) {
        List<Document> existingData = switch (documentType) {
            case ANO -> storageService.load(AnoDocument.class);
            case DUO -> storageService.load(DuoDocument.class);
        };

        if (existingData.isEmpty()) {
            List<Document> newData = dynamicDataGenerator.generateData(documentType, count);
            storageService.save(newData);
            log.info("Generated and saved {} random {} documents", newData.size(), documentType.name());
            return newData;
        }

        return existingData;
    }
}
