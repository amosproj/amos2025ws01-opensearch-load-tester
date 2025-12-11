package com.opensearchloadtester.testdatagenerator;

import com.opensearchloadtester.testdatagenerator.config.DataGenerationProperties;
import com.opensearchloadtester.testdatagenerator.service.TestdataPreloadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestdataInitializer implements CommandLineRunner {

    private final DataGenerationProperties dataGenerationProperties;
    private final TestdataPreloadService preloadService;

    @Override
    public void run(String... args) {

        try {
            preloadService.preloadTestdata();
        } catch (Exception e) {
            log.error("Unexpected error during batch pre-loading", e);
            throw new RuntimeException("Failed to initialize test data", e);
        }
    }
}
