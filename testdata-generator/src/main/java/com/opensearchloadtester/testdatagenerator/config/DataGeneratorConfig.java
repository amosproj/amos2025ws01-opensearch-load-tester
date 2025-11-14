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
    public DataGenerator dataGenerator(DataGenerationProperties dataGenerationProperties,
                                       FileStorageService fileStorageService) {

        if (dataGenerationProperties.getMode() == DataGenerationProperties.Mode.PERSISTENT) {
            return new PersistentDataGenerator(fileStorageService, dataGenerationProperties.getOutputPath());
        }

        return new DynamicDataGenerator();
    }
}
