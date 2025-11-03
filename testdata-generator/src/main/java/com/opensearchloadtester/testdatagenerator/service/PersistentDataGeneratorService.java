package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.model.Recordable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Generates and stores data persistently (e.g., in /data/testdata.json).
 * On subsequent runs, existing data is reused unless deleted manually.
 */
@Service
public class PersistentDataGeneratorService implements DataGenerator {

    private final FileStorageService storageService;

    // Value taken from application.properties, default: data/testdata.json
    @Value("${data.output.path:data/testdata.json}")
    private String outputPath;

    public PersistentDataGeneratorService() {
        this.storageService = new FileStorageService();
    }

    /**
     * Loads existing data from persistent file.
     * If there is no data available, new random data is generated
     */
    @Override
    public List<Recordable> generateData(int count) {
        try {
            List<Recordable> existingData = storageService.load(outputPath);
            if (!existingData.isEmpty()) {
                System.out.println("Loaded existing test data (" + existingData.size() + " records)");
                return existingData;
            }else{
                return new DynamicDataGeneratorService().generateData(count);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error when generating data", e);
        }
    }
}