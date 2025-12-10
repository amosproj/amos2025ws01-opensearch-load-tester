package com.opensearchloadtester.testdatagenerator.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.PropertiesPropertySource;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class OpenSearchClientConfigTests {

    @AfterEach
    void cleanup() {
        System.clearProperty("opensearch.url");
    }

    @Test
    void openSearchClientDefault() throws Exception {
        Properties defaultProps = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            defaultProps.load(is);
        }

        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("defaultProps", defaultProps));
            ctx.register(OpenSearchClientConfig.class);
            ctx.refresh();

            OpenSearchClientConfig cfg = ctx.getBean(OpenSearchClientConfig.class);

            Field f = OpenSearchClientConfig.class.getDeclaredField("openSearchUrl");
            f.setAccessible(true);
            String cfgUrl = (String) f.get(cfg);

            URI uri = URI.create(cfgUrl);
            assertThat(uri.getScheme()).isEqualTo("http");
            assertThat(uri.getHost()).isEqualTo("localhost");
            assertThat(uri.getPort()).isEqualTo(9200);
        }

    }

    @Test
    void openSearchClientOverrideURLProperties() throws Exception {
        Properties override = new Properties();
        override.setProperty("opensearch.url", "https://example.org:1234");

        try (AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext()) {
            ctx.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("overrideProps", override));
            ctx.register(OpenSearchClientConfig.class);
            ctx.refresh();

            OpenSearchClientConfig cfg = ctx.getBean(OpenSearchClientConfig.class);

            Field f = OpenSearchClientConfig.class.getDeclaredField("openSearchUrl");
            f.setAccessible(true);
            String cfgUrl = (String) f.get(cfg);

            URI uri = URI.create(cfgUrl);
            assertThat(uri.getScheme()).isEqualTo("https");
            assertThat(uri.getHost()).isEqualTo("example.org");
            assertThat(uri.getPort()).isEqualTo(1234);
        }
    }
}
