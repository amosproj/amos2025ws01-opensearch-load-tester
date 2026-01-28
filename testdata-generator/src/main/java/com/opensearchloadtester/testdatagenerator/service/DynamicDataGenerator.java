package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.model.DocumentType;
import com.opensearchloadtester.testdatagenerator.model.ano.AnoDocument;
import com.opensearchloadtester.testdatagenerator.model.duo.DuoDocument;
import com.opensearchloadtester.testdatagenerator.model.Document;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DynamicDataGenerator implements DataGenerator {

    /**
     * Generates random documents dynamically at start of application (not persisted).
     */
    @Override
    public List<Document> generateData(DocumentType documentType, int count) {
        List<Document> documents = new ArrayList<>(count);

        switch (documentType) {
            case ANO -> {
                for (int i = 0; i < count; i++) {
                    documents.add(AnoDocument.random());
                }
            }
            case DUO -> {
                for (int i = 0; i < count; i++) {
                    documents.add(DuoDocument.random());
                }
            }
            default -> throw new IllegalArgumentException("Unsupported document type: " + documentType.name());
        }

        log.debug("Generated {} random {} documents", documents.size(), documentType.name());
        return documents;
    }
}
