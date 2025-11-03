package com.opensearchloadtester.testdatagenerator.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "opensearch")
public class OpenSearchConnectionProperties {

    private String url;
    private String username;
    private String password;
}
