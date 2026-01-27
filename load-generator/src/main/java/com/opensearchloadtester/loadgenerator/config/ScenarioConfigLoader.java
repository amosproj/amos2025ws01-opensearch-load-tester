package com.opensearchloadtester.loadgenerator.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.loadgenerator.model.QueryType;
import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

@Slf4j
@Configuration
public class ScenarioConfigLoader {

    @NotBlank
    @Value("${scenario.config.path}")
    private String scenarioConfigPath;

    @NotBlank
    @Value("${scenario.config}")
    private String scenarioConfig;

    @Value("${load.generator.replicas}")
    private int numberLoadGenerators;

    private final ObjectMapper yamlMapper;

    public ScenarioConfigLoader(@Qualifier("yamlObjectMapper") ObjectMapper yamlMapper) {
        this.yamlMapper = yamlMapper;
    }

    @Bean
    public ScenarioConfig scenarioConfig() {
        Path path = Path.of(scenarioConfigPath + scenarioConfig);
        if (!Files.exists(path)) {
            throw new IllegalStateException("Test scenario config file not found at: " + path.toAbsolutePath());
        }

        try {
            ScenarioConfig config = yamlMapper.readValue(path.toFile(), ScenarioConfig.class);

            validateQueryMix(config);

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

    private void validateQueryMix(ScenarioConfig config) {
        JsonNode mix = config.getQueryMix();

        if (mix == null || mix.isNull() || !mix.isArray() || mix.isEmpty()) {
            throw new IllegalArgumentException(
                    "query_mix must be defined and contain at least one query type. " +
                            "Use short form (e.g. query_mix: [TYPE_A, TYPE_B]) for uniform distribution."
            );
        }

        HashSet<QueryType> seen = new HashSet<>();
        int totalWeight = 0;

        for (JsonNode entry : mix) {
            QueryType type;
            int weight;

            if (entry.isTextual()) {
                // short form: "query_mix:- ANO_PAYROLL_RANGE"
                type = QueryType.valueOf(entry.asText());

                if (totalWeight > 0) {
                    throw new IllegalArgumentException(
                            "Cannot mix short and long form entries in query_mix. " +
                                    "Either use all short form or all long form!"
                    );
                }

            } else if (entry.isObject()) {
                // long form: "query_mix: - type: ANO_MULTI_REGEX percent: 80"

                JsonNode typeNode = entry.get("type");
                JsonNode percentNode = entry.get("percent");

                if (typeNode == null || !typeNode.isTextual()) {
                    throw new IllegalArgumentException("query_mix entry missing string field 'type'");
                }
                if (percentNode == null || !percentNode.canConvertToInt()) {
                    throw new IllegalArgumentException("query_mix entry missing integer field 'percent'");
                }

                type = QueryType.valueOf(typeNode.asText());
                weight = percentNode.asInt();

                if (weight <= 0 || weight >= 100) {
                    throw new IllegalArgumentException("query_mix weights must be between " +
                            "0 and 100 for long form entries.");
                }

                totalWeight += weight;
                if (totalWeight > 100) {
                    throw new IllegalArgumentException(
                            "query_mix weights sum too large (" + totalWeight + "). " +
                                    "Please use smaller ratios (e.g. 20:50:30)."
                    );
                }
            } else {
                throw new IllegalArgumentException("Invalid query_mix entry: " + entry);
            }

            if (!seen.add(type)) {
                throw new IllegalArgumentException("Duplicate query_mix entry for type: " + type);
            }
        }
    }
}
