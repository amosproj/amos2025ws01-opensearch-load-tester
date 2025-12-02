package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import com.opensearchloadtester.loadgenerator.service.LoadRunnerService;
import com.opensearchloadtester.loadgenerator.service.WarmUpService;
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
    private final WarmUpService warmUpService;

    @Override
    public void run(String... args) {

        //1. Warm-up if enabled true
        if (scenarioConfig.isWarmUpEnabled()) {
            warmUpService.runWarmUp(scenarioConfig);
        } else {
            log.info("Warm-up disabled for this scenario.");
        }

        //2. Then the real scenario
        log.info("Started test scenario execution '{}'", scenarioConfig.getName());
        loadRunnerService.execute(scenarioConfig);
        log.info("Finished test scenario execution successfully");
    }
}

