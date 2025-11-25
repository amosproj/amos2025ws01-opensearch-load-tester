package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.config.DataGenerationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job that periodically triggers the batch pre-loading.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TestdataPreloadScheduler {

    private final DataGenerationProperties dataGenerationProperties;
    private final TestdataPreloadService preloadService;

    /**
     * Periodic pre-load based on cron expression from configuration.
     * `data.generation.preload-cron` in application.properties.
     */
    @Scheduled(cron = "${data.generation.preload-cron}")
    public void scheduledPreload() {
        if (!dataGenerationProperties.isPreloadSchedulerEnabled()) {
            // scheduler global OFF
            log.debug("Preload scheduler is disabled. Skipping scheduled run.");
            return;
        }

        log.debug("Scheduled batch pre-loading job triggered by @Scheduled.");
        try {
            preloadService.preloadTestdata();
        } catch (Exception e) {
            log.error("Error during scheduled pre-load job", e);
        }
    }
}
