package com.opensearchloadtester.testdatagenerator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "data.generation")
@Data
public class DataGenerationProperties {

    public enum Mode {
        DYNAMIC, PERSISTENT
    }

    private Mode mode;
    private String outputPath;
    private int count;
}
