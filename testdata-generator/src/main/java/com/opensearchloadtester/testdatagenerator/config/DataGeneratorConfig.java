package com.opensearchloadtester.testdatagenerator.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opensearchloadtester.testdatagenerator.service.DataGenerator;
import com.opensearchloadtester.testdatagenerator.service.DynamicDataGenerator;
import com.opensearchloadtester.testdatagenerator.service.FileStorageService;
import com.opensearchloadtester.testdatagenerator.service.PersistentDataGenerator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataGeneratorConfig {

    @Bean
    @Primary
    public DataGenerator dataGenerator(DataGenerationProperties dataGenerationProperties,
                                       @Qualifier("timeJsonObjectMapper") ObjectMapper objectMapper) {

        return switch (dataGenerationProperties.getMode()) {
            case DYNAMIC -> new DynamicDataGenerator();
            case PERSISTENT -> new PersistentDataGenerator(
                    new FileStorageService(dataGenerationProperties.getOutputPath(), objectMapper),
                    new DynamicDataGenerator()
            );
        };
    }
}
