package com.opensearchloadtester.testdatagenerator;

import com.opensearchloadtester.testdatagenerator.config.DataGenerationProperties;
import com.opensearchloadtester.testdatagenerator.model.Index;
import com.opensearchloadtester.testdatagenerator.model.ano.AnoIndex;
import com.opensearchloadtester.testdatagenerator.model.duo.DuoIndex;
import com.opensearchloadtester.testdatagenerator.service.OpenSearchDataService;
import com.opensearchloadtester.testdatagenerator.service.DataGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestdataInitializer implements CommandLineRunner {

    private final DataGenerationProperties dataGenerationProperties;
    private final OpenSearchDataService openSearchDataService;
    private final DataGenerationService dataGenerationService;

    @Override
    public void run(String... args) {
        final Index index = switch (dataGenerationProperties.getDocumentType()) {
            case ANO -> AnoIndex.getInstance();
            case DUO -> DuoIndex.getInstance();
        };

        log.info("Started test data initialization in '{}' mode with '{}' documents",
                dataGenerationProperties.getMode(),
                dataGenerationProperties.getDocumentType());

        openSearchDataService.createIndex(index.getName(), index.getSettings(), index.getMapping());
        dataGenerationService.generateAndIndexTestData(index.getName());
        openSearchDataService.refreshIndex(index.getName());

        log.info("Finished test data initialization successfully");
    }
}
