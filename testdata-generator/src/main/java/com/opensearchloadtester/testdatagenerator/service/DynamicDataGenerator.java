package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.model.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

@Service
public class DynamicDataGenerator implements DataGenerator {

    /**
     * Generates random data dynamically at start of application (not persisted).
     * There will be a mix of Ano and Duo Records generated.
     */
    @Override
    public List<Recordable> generateData(int count) {
        List<Recordable> res = new ArrayList<>();
        int mid = count/2;
        // Generate Ano Data
        for(int i = 0; i < mid; i++) {
            res.add(AnoRecord.random());
        }
        // Generate Duo Data
        for(int i = mid; i < count; i++) {
            res.add(DuoRecord.random());
        }
        return res;
    }
}
