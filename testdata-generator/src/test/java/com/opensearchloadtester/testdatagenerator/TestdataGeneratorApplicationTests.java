package com.opensearchloadtester.testdatagenerator;

import com.opensearchloadtester.testdatagenerator.model.ano.AnoDocument;
import com.opensearchloadtester.testdatagenerator.model.duo.DuoDocument;
import com.opensearchloadtester.testdatagenerator.model.Document;
import com.opensearchloadtester.testdatagenerator.service.DynamicDataGenerator;
import com.opensearchloadtester.testdatagenerator.service.FileStorageService;
import com.opensearchloadtester.testdatagenerator.service.PersistentDataGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Random;

@SpringBootTest
class TestdataGeneratorApplicationTests {

	@Test
	void testDynamicGeneratorService() {
        /**
         * Checks if dynamic data generator generates correct amount
         * of data and if each Document object has an ID
         */
        DynamicDataGenerator dynamicGen = new DynamicDataGenerator();
        Random rand = new Random();
        int numData = rand.nextInt(40)+10;      // generate min 10 objects
        List<Document> list = dynamicGen.generateData(numData);
        assert list.size() == numData;
        for(Document document : list) {
            if(document instanceof AnoDocument) {
                AnoDocument ano = (AnoDocument) document;
                assert ano.getId() != null;
            }else{
                DuoDocument duo = (DuoDocument) document;
                assert duo.getId() != null;
            }
        }
	}
    @Test
    void testPersistentGeneratorService() {
        /**
         * Checks if persistent data generator generates correct amount
         * of data and if each Document object has an ID.
         * Here there is no distinction, whether there already exists data or not.
         */
        PersistentDataGenerator persistentGen = new PersistentDataGenerator(new FileStorageService(),"data/testdata.json");
        Random rand = new Random();
        int numData = rand.nextInt(40)+10;      // generate min 10 objects
        List<Document> list = persistentGen.generateData(numData);
        assert list.size() == numData;
        for(Document document : list) {
            if(document instanceof AnoDocument) {
                AnoDocument ano = (AnoDocument) document;
                assert ano.getId() != null;
            }else{
                DuoDocument duo = (DuoDocument) document;
                assert duo.getId() != null;
            }
        }
    }
}
