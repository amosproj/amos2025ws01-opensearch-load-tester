package com.opensearchloadtester.testdatagenerator.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "data.generation")
@Validated
@Data
public class DataGenerationProperties {

    public enum Mode {
        DYNAMIC, PERSISTENT
    }

    public enum DocumentType {
        ANO, DUO
    }

    private Mode mode;
    private DocumentType documentType;
    private String outputPath;
    @Min(1)
    private int count;
}
