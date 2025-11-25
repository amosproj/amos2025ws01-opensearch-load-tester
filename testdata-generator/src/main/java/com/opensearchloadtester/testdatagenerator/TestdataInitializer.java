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
        if (!dataGenerationProperties.isPreloadOnStartup()) {
            log.debug("Preload on startup is disabled. Skipping batch pre-loading job.");
            return;
        }
        log.debug("Preload on startup is enabled. Running batch pre-loading job at startup.");

        try {
            preloadService.preloadTestdata();
            log.info("Batch pre-loading job finished. Application will now terminate.");
            System.exit(0);// because service_completed_successfully in yaml and Spring Scheduling is still enabled even if schedule is false

        } catch (Exception e) {
            log.error("Unexpected error during batch pre-loading", e);
            throw new RuntimeException("Failed to initialize test data", e);
            System.exit(1);
        }
    }
}
