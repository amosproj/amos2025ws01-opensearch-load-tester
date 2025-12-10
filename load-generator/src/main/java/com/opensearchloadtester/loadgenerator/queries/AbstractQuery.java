package com.opensearchloadtester.loadgenerator.queries;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Slf4j
public abstract class AbstractQuery implements Query {

    protected static final Random RANDOM = new Random();
    protected static final Faker FAKER = new Faker(Locale.GERMAN);

    protected String loadQueryTemplate(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException(String.format("Query template '%s' not found", path));
        }
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read query template '{}': {}", path, e.getMessage());
            throw new UncheckedIOException(String.format("Failed to read query template '%s'", path), e);
        }
    }

    protected String applyQueryParams(String template, Map<String, String> params) {
        String result = template;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
