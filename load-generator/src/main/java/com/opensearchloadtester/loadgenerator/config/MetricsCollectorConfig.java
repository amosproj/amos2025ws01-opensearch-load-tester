package com.opensearchloadtester.loadgenerator.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Getter
@Component
public class MetricsCollectorConfig {

    @Value("${spring.application.name:load-generator}")
    private String applicationName;

    public String getCsvInstanceName() {
        String instanceName = System.getenv("HOSTNAME");
        if (instanceName == null || instanceName.isBlank()) {
            log.info("Not running in a container -> using application name for csv file");
            instanceName = applicationName;
        }
        return instanceName;
    }

    public String getCsvFilePath() {
        return "./data/metrics_" + getCsvInstanceName() + ".csv";
    }
}
