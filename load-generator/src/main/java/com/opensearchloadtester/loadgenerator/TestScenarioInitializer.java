package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import com.opensearchloadtester.loadgenerator.service.LoadRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestScenarioInitializer implements CommandLineRunner {

    private final ScenarioConfig scenarioConfig;
    private final LoadRunner loadRunner;

    @Override
    public void run(String... args) {
        try {
            log.info("Started load test");
            loadRunner.executeScenario(scenarioConfig);
            log.info("Finished load test successfully");
        } catch (Exception e) {
            log.error("Unexpected error while executing load test: {}", e.getMessage());
            throw new RuntimeException("Failed to execute load test", e);
        }
    }
}
