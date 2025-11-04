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
    private final String outputPath;

    public PersistentDataGeneratorService(FileStorageService storageService, @Value("${data.output.path:data/testdata.json}") String outputPath) {
        this.storageService = storageService;
        this.outputPath = outputPath;
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
                if(existingData.size() != count) {
                    System.out.println("Loaded existing test data (" + existingData.size() + " records) - not desired data amount");
                    List<Recordable> list = new DynamicDataGeneratorService().generateData(count);
                    storageService.save(list, outputPath);
                    System.out.println("Generated new test data (" + list.size() + " records)");
                    return list;
                }
                System.out.println(existingData.size() + " test data loaded");
                return existingData;
            }else{
                List<Recordable> list = new DynamicDataGeneratorService().generateData(count);
                storageService.save(list, outputPath);
                System.out.println("Generated new test data (" + existingData.size() + " records)");
                return list;
            }
        } catch (IOException e) {
            throw new RuntimeException("Error when generating data", e);
        }

    }
}