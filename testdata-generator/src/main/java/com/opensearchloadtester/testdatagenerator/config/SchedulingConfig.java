package com.opensearchloadtester.testdatagenerator.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(
        prefix = "data.generation",
        name = "preload-scheduler-enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class SchedulingConfig {
}
