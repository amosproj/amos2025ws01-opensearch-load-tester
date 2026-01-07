package com.opensearchloadtester.testdatagenerator;

import com.opensearchloadtester.testdatagenerator.config.DataGenerationProperties.DocumentType;
import com.opensearchloadtester.testdatagenerator.model.ano.AnoDocument;
import com.opensearchloadtester.testdatagenerator.model.Document;
import com.opensearchloadtester.testdatagenerator.service.DynamicDataGenerator;
import com.opensearchloadtester.testdatagenerator.service.FileStorageService;
import com.opensearchloadtester.testdatagenerator.service.PersistentDataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

@SpringBootTest
class TestdataGeneratorApplicationTests {

    @BeforeEach
    void setup() throws IOException {
        Path path = Paths.get("data/testdata.json");
        Files.deleteIfExists(path);
    }

    @Test
    void testDynamicGeneratorService() {
        /*
         * Checks if dynamic data generator generates correct amount
         * of data and if each Document object has an ID
         */
        DynamicDataGenerator dynamicGen = new DynamicDataGenerator();
        int numData = 10;
        List<Document> list = dynamicGen.generateData(DocumentType.ANO, numData);
        assert list.size() == numData;
        for (Document document : list) {
            AnoDocument ano = (AnoDocument) document;
            assert ano.getId() != null;
        }
    }

    @Test
    void testPersistentGeneratorService() {
        /*
         * Checks if persistent data generator generates correct amount
         * of data and if each Document object has an ID.
         * Here there is no distinction, whether there already exists data or not.
         */
        PersistentDataGenerator persistentGen = new PersistentDataGenerator(
                new FileStorageService(),
                "data/testdata.json",
                new DynamicDataGenerator());
        int numData = 10;
        List<Document> list = persistentGen.generateData(DocumentType.ANO, numData);
        assert list.size() == numData;
        for (Document document : list) {
            AnoDocument ano = (AnoDocument) document;
            assert ano.getId() != null;
        }
    }
}
