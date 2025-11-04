package com.opensearchloadtester.testdatagenerator;

import com.opensearchloadtester.testdatagenerator.model.Recordable;
import com.opensearchloadtester.testdatagenerator.service.DataGenerator;
import com.opensearchloadtester.testdatagenerator.service.DynamicDataGeneratorService;
import com.opensearchloadtester.testdatagenerator.service.FileStorageService;
import com.opensearchloadtester.testdatagenerator.service.PersistentDataGeneratorService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

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
    private DataGenerator dataGenerator;

    @Override
    public void run(String... args) {
        System.out.println("Starting Test Data Generation (mode: " + mode + ")...");

        if ("dynamic".equalsIgnoreCase(mode)) {
            this.dataGenerator = new DynamicDataGeneratorService();
        } else {
            this.dataGenerator = new PersistentDataGeneratorService(new FileStorageService(), outputPath);
        }

        List<Recordable> data = dataGenerator.generateData(10); //example value 10

        // Debug Output:
        /*
        System.out.println("Listing data:");
        for(Recordable item: data){
            System.out.println("Data Class" + item.getClass());
        }
        */


    }

}