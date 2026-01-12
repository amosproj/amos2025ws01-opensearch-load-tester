package com.opensearchloadtester.loadgenerator.config;

import com.opensearchloadtester.loadgenerator.model.ScenarioConfig;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
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

    @Value("${opensearch.url}")
    private final String openSearchUrl;
    private final ScenarioConfig scenarioConfig;

    public OpenSearchClientConfig(
            @Value("${opensearch.url}") String openSearchUrl,
            ScenarioConfig scenarioConfig) {
        this.openSearchUrl = openSearchUrl;
        this.scenarioConfig = scenarioConfig;
    }

    @Bean
    @Primary
    public OpenSearchClient openSearchClient() {
        URI uri = URI.create(openSearchUrl);
        HttpHost host = new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());

        long TIMEOUT_SECONDS = scenarioConfig.getQueryResponseTimeout().toSeconds();

        OpenSearchTransport transport = ApacheHttpClient5TransportBuilder
                .builder(host)
                .setHttpClientConfigCallback(httpClientBuilder -> {
                    ConnectionConfig connectionConfig = ConnectionConfig.custom()
                            .setSocketTimeout(Timeout.ofSeconds(TIMEOUT_SECONDS))
                            .setConnectTimeout(Timeout.ofSeconds(TIMEOUT_SECONDS))
                            .build();

                    PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder
                            .create()
                            .setDefaultConnectionConfig(connectionConfig)
                            .build();

                    RequestConfig requestConfig = RequestConfig.custom()
                            .setResponseTimeout(Timeout.ofSeconds(TIMEOUT_SECONDS))
                            .setConnectionRequestTimeout(Timeout.ofSeconds(TIMEOUT_SECONDS))
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
