package com.opensearchloadtester.testdatagenerator;

import com.opensearchloadtester.testdatagenerator.config.DataGenerationProperties;
import com.opensearchloadtester.testdatagenerator.model.Document;
import com.opensearchloadtester.testdatagenerator.service.DataGenerator;
import com.opensearchloadtester.testdatagenerator.service.OpenSearchDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestdataInitializer implements CommandLineRunner {

    private final DataGenerationProperties dataGenerationProperties;
    private final DataGenerator dataGenerator;
    private final OpenSearchDataService openSearchDataService;

    @Override
    public void run(String... args) {
        final String indexName = "testdata-index";

        try {
            log.info("Started test data initialization in '{}' mode", dataGenerationProperties.getMode());

            openSearchDataService.createIndex(indexName, null, null);
            List<Document> documents = dataGenerator.generateData(dataGenerationProperties.getCount());
            openSearchDataService.bulkIndexDocuments(indexName, documents);

            log.info("Finished test data initialization successfully");
        } catch (Exception e) {
            log.error("Unexpected error while initializing test data", e);
            throw new RuntimeException("Failed to initialize test data", e);
        }
    }
}
