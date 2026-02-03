package com.opensearchloadtester.loadgenerator.queries;

import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractQuery {

    protected static final ThreadLocal<Faker> FAKER =
            ThreadLocal.withInitial(() -> new Faker(Locale.GERMAN));

    protected final Map<String, String> queryParams;
    protected final String queryTemplatePath;

    protected AbstractQuery(Map<String, String> queryParams, String queryTemplatePath) {
        this.queryParams = queryParams;
        this.queryTemplatePath = queryTemplatePath;
    }

    public String toJsonString() {
        return applyQueryParams(getQueryTemplate(), queryParams);
    }

    // Cache the loaded template to avoid repeated I/O
    private volatile String cachedTemplate;

    protected String getQueryTemplate() {
        if (cachedTemplate == null) {
            cachedTemplate = loadQueryTemplate(queryTemplatePath);
        }
        return cachedTemplate;
    }

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

    protected static Faker faker() {
        return FAKER.get();
    }

    // Generate a random year within the last 10 years
    protected static String getRandomYear() {
        Instant i = Date.from(faker().timeAndDate().past(3650, TimeUnit.DAYS)).toInstant();
        java.time.ZonedDateTime zdt = i.atZone(java.time.ZoneId.systemDefault());
        return String.valueOf(zdt.getYear());
    }

    protected static String getRandomYearAfter(String fromYear) {
        return String.valueOf(faker().number().numberBetween(Integer.parseInt(fromYear), LocalDate.now().getYear()));
    }
}
