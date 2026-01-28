package com.opensearchloadtester.testdatagenerator.config;

import com.opensearchloadtester.testdatagenerator.model.DataGenerationMode;
import com.opensearchloadtester.testdatagenerator.model.DocumentType;
import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "data.generation")
@Validated
@Data
public class DataGenerationProperties {

    private DataGenerationMode mode;
    private DocumentType documentType;
    private String outputPath;
    @Min(1)
    private int count;
    @Min(1)
    private int batchSize;
}
