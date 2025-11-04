package com.opensearchloadtester.testdatagenerator.config;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class OpenSearchClientConfig {

    @Value("${opensearch.url}")
    private String openSearchUrl;

    @Bean
    public OpenSearchClient openSearchClient() {
        URI uri = URI.create(openSearchUrl);
        HttpHost host = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());

        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder
                .builder(host)
                .build();

        return new OpenSearchClient(transport);
    }
}
