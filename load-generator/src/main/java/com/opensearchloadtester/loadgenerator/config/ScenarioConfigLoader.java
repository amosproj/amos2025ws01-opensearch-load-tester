package com.opensearchloadtester.loadgenerator.config;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Configuration
public class ScenarioConfigLoader {

    @Value("${scenario.config.path}")
    private String scenarioConfigPath;

    @Value("${load.generator.replicas}")
    private int numberLoadGenerators;

    @Bean
    public ScenarioConfig scenarioConfig() {
        Path path = Path.of(scenarioConfigPath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Test scenario config file not found at: " + path.toAbsolutePath());
        }

        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        try {
            ScenarioConfig config =
                    yamlMapper.readValue(path.toFile(), ScenarioConfig.class);

            if (numberLoadGenerators <= 0) {
                throw new IllegalStateException("Invalid configuration: load generator replicas must be > 0.");
            }
            if (config.getQueriesPerSecond() < numberLoadGenerators) {
                throw new IllegalStateException(
                        "Invalid scenario configuration: queriesPerSecond must be >= load generator replicas."
                );
            }

            return config;
        } catch (IOException e) {
            log.error("Failed to read or parse from file '{}': {}", path.toAbsolutePath(), e.getMessage());
            throw new RuntimeException(String.format("Failed to read or parse from file '%s'", path), e);
        }
    }
}
