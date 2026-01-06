package com.opensearchloadtester.testdatagenerator.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.opensearchloadtester.testdatagenerator.model.ano.AnoDocument;
import com.opensearchloadtester.testdatagenerator.model.duo.DuoDocument;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AbstractDocumentTests {

    private final ObjectMapper mapper;

    public AbstractDocumentTests() {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    void openRandomAno_hasCommonFields_and_dateSerialization() throws Exception {
        AnoDocument a = AnoDocument.random();
        assertThat(a.getId()).isNotNull();
        assertThat(a.getId()).contains("_");
        int idx = a.getId().indexOf('_');
        assertThat(idx).isGreaterThan(0);

        assertThat(a.getContentType()).isEqualTo("application/pdf");

        assertThat(a.getEtag()).isNotNull().matches("[0-9a-fA-F]{32}");
        assertThat(a.getDssVersion()).isNotNull().matches("^1\\.\\d+$");

        // date fields
        String json = mapper.writeValueAsString(a);
        JsonNode root = mapper.readTree(json);
        String dateTimeWithOffsetRegex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:[+-]\\d{2}:\\d{2}|Z)$";

        JsonNode dssLastModifiedDatetime = root.path("dssLastModifiedDatetime");
        assertThat(dssLastModifiedDatetime.isNull()).isFalse();
        assertThat(dssLastModifiedDatetime.asText()).matches(dateTimeWithOffsetRegex);

        JsonNode lastDocumentChange = root.path("lastDocumentChange");
        assertThat(lastDocumentChange.isNull()).isFalse();
        assertThat(lastDocumentChange.asText()).matches(dateTimeWithOffsetRegex);
    }

    @Test
    void openRandomDuo_hasCommonFields_and_dateSerialization() throws Exception {
        DuoDocument d = DuoDocument.random();
        assertThat(d.getId()).isNotNull();
        assertThat(d.getId()).contains("_");
        int idx = d.getId().indexOf('_');
        assertThat(idx).isGreaterThan(0);

        assertThat(d.getContentType()).isEqualTo("application/pdf");

        // date fields
        String json = mapper.writeValueAsString(d);
        JsonNode root = mapper.readTree(json);
        String dateTimeWithOffsetRegex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:[+-]\\d{2}:\\d{2}|Z)$";

        JsonNode dssLastModifiedDatetime = root.path("dssLastModifiedDatetime");
        assertThat(dssLastModifiedDatetime.isNull()).isFalse();
        assertThat(dssLastModifiedDatetime.asText()).matches(dateTimeWithOffsetRegex);

        JsonNode lastDocumentChange = root.path("lastDocumentChange");
        assertThat(lastDocumentChange.isNull()).isFalse();
        assertThat(lastDocumentChange.asText()).matches(dateTimeWithOffsetRegex);
    }
}
