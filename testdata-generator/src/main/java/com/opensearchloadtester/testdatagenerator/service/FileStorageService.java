package com.opensearchloadtester.testdatagenerator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opensearchloadtester.testdatagenerator.model.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FileStorageService {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())                // Java-Time-Support
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS); // ISO-strings instead of Timestamps

    /**
     * Saves a list of Document objects to a file.
     * Each document is written in JSON format.
     *
     * @param data List of Document Items that have to be stored
     * @param path Destination File path where data should be written
     */
    public void save(List<Document> data, String path) {
        log.info("Saving data to file '{}'", path);

        File file = new File(path);

        try {
            if (!file.exists()) {           // creating new file if it doesn't exist
                File parentDir = file.getParentFile();

                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();     // creating missing dirs
                }
                file.createNewFile();       // create new file
            }

            objectMapper.writeValue(file, data);
        } catch (IOException e) {
            log.error("Failed to save data to file '{}': {}", path, e.getMessage());
            throw new RuntimeException(String.format("Failed to save data to file '%s'", path), e);
        }
    }

    /**
     * Loads a list of Document objects from a file.
     * Each document is written in JSON format.
     *
     * @param path        Source file path where data is located
     * @param targetClass Concrete Document class to deserialize (e.g. AnoDocument.class)
     */
    public List<Document> load(String path, Class<? extends Document> targetClass) {
        File file = new File(path);

        if (!file.exists()) {
            return new ArrayList<>();
        }

        CollectionType collectionType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, targetClass);

        try {
            log.info("Loading data from file '{}'", path);
            List<? extends Document> tmp = objectMapper.readValue(file, collectionType);
            return new ArrayList<>(tmp);
        } catch (IOException e) {
            log.error("Failed to read or parse from file '{}': {}", path, e.getMessage());
            throw new RuntimeException(String.format("Failed to read or parse from file '%s'", path), e);
        }
    }
}
