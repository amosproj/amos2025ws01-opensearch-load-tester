package com.opensearchloadtester.testdatagenerator;

import com.opensearchloadtester.testdatagenerator.model.AnoRecord;
import com.opensearchloadtester.testdatagenerator.model.DuoRecord;
import com.opensearchloadtester.testdatagenerator.model.Recordable;
import com.opensearchloadtester.testdatagenerator.service.DynamicDataGeneratorService;
import com.opensearchloadtester.testdatagenerator.service.FileStorageService;
import com.opensearchloadtester.testdatagenerator.service.PersistentDataGeneratorService;
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
         * of data and if each Recordable object has an ID
         */
        DynamicDataGeneratorService dynamicGen = new DynamicDataGeneratorService();
        Random rand = new Random();
        int numData = rand.nextInt(40)+10;      // generate min 10 objects
        List<Recordable> list = dynamicGen.generateData(numData);
        assert list.size() == numData;
        for(Recordable recordable : list) {
            if(recordable instanceof AnoRecord) {
                AnoRecord ano = (AnoRecord) recordable;
                assert ano.getId() != null;
            }else{
                DuoRecord duo = (DuoRecord) recordable;
                assert duo.getId() != null;
            }
        }
	}
    @Test
    void testPersistentGeneratorService() {
        /**
         * Checks if persistent data generator generates correct amount
         * of data and if each Recordable object has an ID.
         * Here there is no distinction, whether there already exists data or not.
         */
        PersistentDataGeneratorService persistentGen = new PersistentDataGeneratorService(new FileStorageService(),"data/testdata.json");
        Random rand = new Random();
        int numData = rand.nextInt(40)+10;      // generate min 10 objects
        List<Recordable> list = persistentGen.generateData(numData);
        assert list.size() == numData;
        for(Recordable recordable : list) {
            if(recordable instanceof AnoRecord) {
                AnoRecord ano = (AnoRecord) recordable;
                assert ano.getId() != null;
            }else{
                DuoRecord duo = (DuoRecord) recordable;
                assert duo.getId() != null;
            }
        }
    }
}
