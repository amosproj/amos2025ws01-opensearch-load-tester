package com.opensearchloadtester.loadgenerator.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.generic.OpenSearchGenericClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.URI;

@Configuration
public class OpenSearchClientConfig {

    private static final int QUERY_TIMEOUT_SECONDS = 30;

    @Value("${opensearch.url}")
    private String openSearchUrl;

    @Bean
    @Primary
    public OpenSearchClient openSearchClient() {
        URI uri = URI.create(openSearchUrl);
        HttpHost host = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());

        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder
                .builder(host)
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    // Socket timeout - ensures read operations timeout after 30 seconds
                    ConnectionConfig connectionConfig = ConnectionConfig.custom()
                            .setSocketTimeout(Timeout.ofSeconds(QUERY_TIMEOUT_SECONDS))
                            .setConnectTimeout(Timeout.ofSeconds(QUERY_TIMEOUT_SECONDS))
                            .build();

                    var connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                            .setDefaultConnectionConfig(connectionConfig)
                            .build();

                    // Response timeout - ensures waiting for response times out after 30 seconds
                    RequestConfig requestConfig = RequestConfig.custom()
                            .setResponseTimeout(Timeout.ofSeconds(QUERY_TIMEOUT_SECONDS))
                            .setConnectionRequestTimeout(Timeout.ofSeconds(QUERY_TIMEOUT_SECONDS))
                            .build();

                    return httpClientBuilder
                            .setConnectionManager(connectionManager)
                            .setDefaultRequestConfig(requestConfig);
                })
                .build();

        return new OpenSearchClient(transport);
    }

    @Bean
    public OpenSearchGenericClient openSearchGenericClient(OpenSearchClient openSearchClient) {
        return openSearchClient.generic();
    }
}
