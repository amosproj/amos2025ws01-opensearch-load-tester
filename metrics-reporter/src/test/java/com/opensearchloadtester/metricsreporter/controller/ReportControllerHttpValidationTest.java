package com.opensearchloadtester.metricsreporter.controller;

import com.opensearchloadtester.common.dto.MetricsDto;
import com.opensearchloadtester.metricsreporter.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportControllerHttpValidationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private ReportService reportService;

    @Test
    void submitMetrics_returnsBadRequest_forInvalidDto() throws Exception {
        List<MetricsDto> payload = List.of(
                new MetricsDto("", "query_type_test", -1L, 10L, 3, 99)
        );

        ResponseEntity<String> response = restTemplate.postForEntity("/api/metrics", payload, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo("Invalid metrics payload\n");
        verify(reportService, never()).processMetrics(anyList());
    }
}
