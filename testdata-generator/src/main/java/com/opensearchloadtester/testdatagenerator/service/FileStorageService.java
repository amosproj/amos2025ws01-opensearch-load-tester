package com.opensearchloadtester.testdatagenerator.service;

import com.opensearchloadtester.testdatagenerator.model.Recordable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class FileStorageService {

    /**
     * Saves a list of Recordable objects to a file.
     * Each record is written in JSON format.
     * @param data List of Recordable Items that have to be stored
     * @param path Destination File path where data should be written
     */
    public void save(List<? extends Recordable> data, String path) throws IOException {
        // TODO
    }

    /**
     * Loads a list of Recordable objects from a file.
     * Each record is written in JSON format.
     * @param path Source File path where data is located
     */
    public List<Recordable> load(String path) throws IOException {
        // TODO
        return null;
    }
}