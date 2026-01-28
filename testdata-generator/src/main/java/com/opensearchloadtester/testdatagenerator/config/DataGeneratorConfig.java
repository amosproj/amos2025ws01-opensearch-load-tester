package com.opensearchloadtester.testdatagenerator.config;

import com.opensearchloadtester.testdatagenerator.service.DataGenerator;
import com.opensearchloadtester.testdatagenerator.service.DynamicDataGenerator;
import com.opensearchloadtester.testdatagenerator.service.FileStorageService;
import com.opensearchloadtester.testdatagenerator.service.PersistentDataGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataGeneratorConfig {

    @Bean
    @Primary
    public DataGenerator dataGenerator(DataGenerationProperties dataGenerationProperties) {

        return switch (dataGenerationProperties.getMode()) {
            case DYNAMIC -> new DynamicDataGenerator();
            case PERSISTENT -> new PersistentDataGenerator(
                    new FileStorageService(dataGenerationProperties.getOutputPath()),
                    new DynamicDataGenerator()
            );
        };
    }
}
