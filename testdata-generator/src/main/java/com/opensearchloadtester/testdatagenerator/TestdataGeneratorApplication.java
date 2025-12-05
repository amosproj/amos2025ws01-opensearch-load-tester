package com.opensearchloadtester.testdatagenerator;

import com.opensearchloadtester.testdatagenerator.config.DataGenerationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableConfigurationProperties(DataGenerationProperties.class)
@EnableScheduling
@SpringBootApplication
public class TestdataGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestdataGeneratorApplication.class, args);
    }

}
