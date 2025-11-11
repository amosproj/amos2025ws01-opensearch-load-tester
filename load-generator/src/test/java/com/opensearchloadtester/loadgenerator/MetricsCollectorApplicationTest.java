package com.opensearchloadtester.loadgenerator;

import com.opensearchloadtester.loadgenerator.config.MetricsCollectorConfig;
import com.opensearchloadtester.loadgenerator.service.MetricsCollectorService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MetricsCollectorApplicationTest {

    @Autowired
    private MetricsCollectorService service;

    @Autowired
    private MetricsCollectorConfig config;

    private File csvFile;


    /*
    @BeforeEach
    void setUp() {}

    @AfterEach
    void tearDown() {}
    */

    @Test
    void testAppendMetricsAndValidateCsv() throws IOException, InterruptedException {

        // JSON from opensearch http response
        String json1 = """
            {
              "took": 2,
              "timed_out": false,
              "_shards": {
                "total": 1,
                "successful": 1,
                "skipped": 0,
                "failed": 0
              },
              "hits": {
                "total": {
                  "value": 1,
                  "relation": "eq"
                },
                "max_score": 2.5859058,
                "hits": [
                  {
                    "_index": "test-index",
                    "_id": "TpH-bZoBPYUzGkOLK8F6",
                    "_score": 2.5859058,
                    "_source": {
                      "message": "The quick brown fox"
                    }
                  }
                ]
              }
            }
            """;

        String json2 = """
            {
              "took": 3,
              "timed_out": false,
              "_shards": {
                "total": 1,
                "successful": 1,
                "skipped": 0,
                "failed": 0
              },
              "hits": {
                "total": {
                  "value": 1,
                  "relation": "eq"
                },
                "max_score": 3.3342385,
                "hits": [
                  {
                    "_index": "test-index",
                    "_id": "TpH-bZoBPYUzGkOLK8F6",
                    "_score": 3.3342385,
                    "_source": {
                      "message": "Hello OpenSearch"
                    }
                  }
                ]
              }
            }
            """;

        String json3 = """
            {
              "took": 2,
              "timed_out": false,
              "_shards": {
                "total": 1,
                "successful": 1,
                "skipped": 0,
                "failed": 0
              },
              "hits": {
                "total": {
                  "value": 1,
                  "relation": "eq"
                },
                "max_score": 10.369164,
                "hits": [
                  {
                    "_index": "test-index",
                    "_id": "UZH-bZoBPYUzGkOLK8HR",
                    "_score": 4.369164,
                    "_source": {
                      "message": "Specific test message"
                    }
                  }
                ]
              }
            }
            """;

        service.appendMetrics("quick brown", 100, json1);
        service.appendMetrics("Hello OpenSearch", 210, json2);
        service.appendMetrics("Specific test message", 90, json3);

        // get the csv file
        File metricsFile = service.getMetricFile();

        assertNotNull(metricsFile, "The method getMetricsFile() returned null.");
        assertTrue(metricsFile.exists(), "The file returned by getMetricsFile() does not exist.");

        BufferedReader reader = new BufferedReader(new FileReader(metricsFile));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        reader.close();
    }
}
