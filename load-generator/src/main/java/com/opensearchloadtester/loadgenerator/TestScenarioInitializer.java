package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import com.opensearchloadtester.loadgenerator.service.LoadRunnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TestScenarioInitializer implements CommandLineRunner {

    private final ScenarioConfig scenarioConfig;
    private final LoadRunnerService loadRunnerService;

    @Override
    public void run(String... args) {
        log.info("Started test scenario execution '{}'", scenarioConfig.getName());

        loadRunnerService.execute(scenarioConfig);

        log.info("Finished test scenario execution successfully");
    }
}
