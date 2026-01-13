package com.opensearchloadtester.testdatagenerator.model.duo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class DuoDocumentTests {

    private final ObjectMapper mapper;

    public DuoDocumentTests() {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    void duoSpecificFields_and_dateSerialization() throws Exception {
        DuoDocument d = DuoDocument.random();

        // Duo-specific checks
        assertThat(d.getOcrFulltext()).isNotNull().isNotBlank();
        assertThat(d.getDssCustomMetadataDuo()).isNotNull();

        String json = mapper.writeValueAsString(d);
        JsonNode root = mapper.readTree(json);

        String dateTimeWithOffsetRegex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:[+-]\\d{2}:\\d{2}|Z)$";
        String dateOnlyRegex = "^\\d{4}-\\d{2}-\\d{2}$";

        JsonNode duoMeta = root.path("dssCustomMetadataDuo");
        assertThat(duoMeta).isNotNull();

        // invoiceDate => yyyy-MM-dd
        JsonNode invoiceDate = duoMeta.path("invoiceDate");
        assertThat(invoiceDate.isNull()).isFalse();
        assertThat(invoiceDate.asText()).matches(dateOnlyRegex);

        // lastModifiedDatetime => datetime with offset
        JsonNode lastModified = duoMeta.path("lastModifiedDatetime");
        assertThat(lastModified.isNull()).isFalse();
        assertThat(lastModified.asText()).matches(dateTimeWithOffsetRegex);

        // timeOfUpload => datetime with offset
        JsonNode timeOfUpload = duoMeta.path("timeOfUpload");
        assertThat(timeOfUpload.isNull()).isFalse();
        assertThat(timeOfUpload.asText()).matches(dateTimeWithOffsetRegex);

        // paidAt may be null or datetime with offset
        JsonNode paidAt = duoMeta.path("paidAt");
        if (!paidAt.isNull() && !paidAt.asText().isEmpty()) {
            assertThat(paidAt.asText()).matches(dateTimeWithOffsetRegex);
        }

        // serviceDate must be yyyy-MM-dd
        JsonNode positions = duoMeta.path("positions");
        if (positions.isArray()) {
            Iterator<JsonNode> it = positions.elements();
            while (it.hasNext()) {
                JsonNode pos = it.next();
                JsonNode serviceDate = pos.path("serviceDate");
                if (!serviceDate.isNull() && !serviceDate.asText().isEmpty()) {
                    assertThat(serviceDate.asText()).matches(dateOnlyRegex);
                }
            }
        }
    }
}
