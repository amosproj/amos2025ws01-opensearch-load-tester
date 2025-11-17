package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.model.ano.AnoDocument;
import com.opensearchloadtester.testdatagenerator.model.duo.DuoDocument;
import com.opensearchloadtester.testdatagenerator.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DynamicDataGenerator implements DataGenerator {

    /**
     * Generates random data dynamically at start of application (not persisted).
     * There will be a mix of Ano and Duo Documents generated.
     */
    @Override
    public List<Document> generateData(int count) {
        List<Document> res = new ArrayList<>();
        int mid = count/2;
        // Generate Ano Data
        for(int i = 0; i < mid; i++) {
            res.add(AnoDocument.random());
        }
        // Generate Duo Data
        for(int i = mid; i < count; i++) {
            res.add(DuoDocument.random());
        }
        log.info("Generated {} random documents", count);
        return res;
    }
}
