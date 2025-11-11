package com.opensearchloadtester.testdatagenerator;

import com.opensearchloadtester.testdatagenerator.model.Recordable;
import com.opensearchloadtester.testdatagenerator.service.DataGenerator;
import com.opensearchloadtester.testdatagenerator.service.DynamicDataGeneratorService;
import com.opensearchloadtester.testdatagenerator.service.FileStorageService;
import com.opensearchloadtester.testdatagenerator.service.PersistentDataGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

@Slf4j
@SpringBootApplication
public class TestdataGeneratorApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(TestdataGeneratorApplication.class, args);
    }

    // Value taken from application.properties, default: persistent
    @Value("${data.generation.mode:persistent}")
    private String mode;
    // Value taken from application.properties, default: data/testdata.json
    @Value("${data.output.path:data/testdata.json}")
    private String outputPath;
    @Value("${data.generation.count}")
    private int recordsCount;
    private DataGenerator dataGenerator;

    @Override
    public void run(String... args) {
        log.info("Starting test-data generation (mode: {}) ...", mode);

        if ("dynamic".equalsIgnoreCase(mode)) {
            this.dataGenerator = new DynamicDataGeneratorService();
        } else {
            this.dataGenerator = new PersistentDataGeneratorService(new FileStorageService(), outputPath);
        }

        List<Recordable> data = dataGenerator.generateData(recordsCount);

        log.info("Test-data generation completed.");

        // Debug Output:
        log.debug("Listing data:");
        for (Recordable item : data) {
            log.debug("Data Class: {}", item.getClass());
        }

    }

}
