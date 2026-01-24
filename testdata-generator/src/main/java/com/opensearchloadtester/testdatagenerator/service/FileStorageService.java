package com.opensearchloadtester.testdatagenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opensearchloadtester.testdatagenerator.model.Document;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private final String outputPath;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule()) // Java-Time-Support
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-strings instead of Timestamps

    /**
     * Saves a list of Document objects to a file.
     * Each document is written in JSON format.
     *
     * @param data List of Document items that have to be stored
     */
    public void save(List<Document> data) {
        log.info("Saving data to file '{}'", outputPath);

        File file = new File(outputPath);

        try {
            // create new file if it doesn't exist
            if (!file.exists()) {
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs(); // create missing dirs
                }
                file.createNewFile(); // create new file
            }

            objectMapper.writeValue(file, data);
        } catch (IOException e) {
            log.error("Failed to save data to file '{}': {}", outputPath, e.getMessage());
            throw new RuntimeException(String.format("Failed to save data to file '%s'", outputPath), e);
        }
    }

    /**
     * Loads a list of Document objects from a file.
     * Each document is written in JSON format.
     *
     * @param targetClass Concrete Document class to deserialize (e.g. AnoDocument.class)
     */
    public List<Document> load(Class<? extends Document> targetClass) {
        log.info("Loading data from file '{}'", outputPath);

        File file = new File(outputPath);

        if (!file.exists()) {
            return new ArrayList<>();
        }

        CollectionType collectionType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, targetClass);

        try {
            List<? extends Document> tmp = objectMapper.readValue(file, collectionType);
            return new ArrayList<>(tmp);
        } catch (IOException e) {
            log.error("Failed to read or parse from file '{}': {}", outputPath, e.getMessage());
            throw new RuntimeException(String.format("Failed to read or parse from file '%s'", outputPath), e);
        }
    }
}
