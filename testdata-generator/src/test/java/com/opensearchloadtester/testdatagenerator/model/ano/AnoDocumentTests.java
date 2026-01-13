package com.opensearchloadtester.testdatagenerator.model.ano;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class AnoDocumentTests {

    private final ObjectMapper mapper;

    public AnoDocumentTests() {
        mapper = new ObjectMapper();
        mapper.findAndRegisterModules();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Test
    void anoSpecificFields_and_dateSerialization() throws Exception {
        AnoDocument a = AnoDocument.random();

        // Ano-specific fields
        assertThat(a.getDssCustomMetadataPayrollInfo()).isNotNull();
        assertThat(a.getDssCreationDatetime()).isNotNull();
        // dssDeleteRetentionMinRetention can be null
        assertThat(a.getDssOriginalFilename()).isNotNull();
        assertThat(a.getDssDocumentName()).isNotNull();
        assertThat(a.getDssRecyclebin()).isNotNull();

        // tests for payroll nested dates
        String json = mapper.writeValueAsString(a);
        JsonNode root = mapper.readTree(json);
        String dateTimeWithOffsetRegex = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:[+-]\\d{2}:\\d{2}|Z)$";
        String dateOnlyRegex = "^\\d{4}-\\d{2}-\\d{2}$";

        JsonNode payroll = root.path("dssCustomMetadataPayrollInfo");
        assertThat(payroll).as("payroll info node").isNotNull();

        // provisionDate -> yyyy-MM-dd
        JsonNode provisionDate = payroll.path("provisionDate");
        assertThat(provisionDate.isNull()).isFalse();
        assertThat(provisionDate.asText()).matches(dateOnlyRegex);

        // firstAccess may be null or datetime with offset
        JsonNode firstAccess = payroll.path("firstAccess");
        if (!firstAccess.isNull() && !firstAccess.asText().isEmpty()) {
            assertThat(firstAccess.asText()).matches(dateTimeWithOffsetRegex);
        }
    }
}
