package com.opensearchloadtester.testdatagenerator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
     * @param data List of Document Items that have to be stored
     * @param path Destination File path where data should be written
     */
    public void save(List<Document> data, String path) throws IOException {
        log.info("Saving data to {}", path);
        File f = new File(path);
        if (!f.exists()) {              // creating new file if it doesn't exist
            File parentDir = f.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();     // creating missing dirs
            }
            f.createNewFile();          // create new file
        }
        // writerFor has to be used to ensure that class type is written to json too
        objectMapper.writerFor(new TypeReference<List<Document>>() {})
                .writeValue(f, data);
    }

    /**
     * Loads a list of Document objects from a file.
     * Each document is written in JSON format.
     * Polymorphy is supported -> automatic distinction between classes
     * @param path Source File path where data is located
     */
    public List<Document> load(String path) {
        try {
            File f = new File(path);
            return objectMapper.readValue(f, new TypeReference<List<Document>>() {});
        } catch (IOException e) {
            // On Error: return empty list
            return new ArrayList<Document>();
        }
    }
}
