package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.model.Recordable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Generates and stores data persistently (e.g., in /data/testdata.json).
 * On subsequent runs, existing data is reused unless deleted manually.
 */
@Slf4j
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
                    log.warn("Existing test-data ({} records) is not desired data amount. Generating new ramdom test-data.", existingData.size());
                    List<Recordable> list = new DynamicDataGeneratorService().generateData(count);
                    storageService.save(list, outputPath);
                    log.info("Generated and saved new random test-data ({} records)", list.size());
                    return list;
                }
                log.info("{} test-data loaded", existingData.size());
                return existingData;
            }else{
                List<Recordable> list = new DynamicDataGeneratorService().generateData(count);
                storageService.save(list, outputPath);
                log.info("Generated and saved new random test-data ({} records)", list.size());
                return list;
            }
        } catch (IOException e) {
            log.error("Error while generating test-data", e);
            throw new RuntimeException("Error while generating test-data", e);
        }
    }
}