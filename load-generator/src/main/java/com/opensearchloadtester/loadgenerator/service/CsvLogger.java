package com.opensearchloadtester.loadgenerator.service;

import lombok.SneakyThrows;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
/**
 * Simple CSV writer utility used to log query results to disk.
 *
 * Features:
 * - Ensures parent directory exists.
 * - Optionally writes a header row when not appending.
 * - Escapes quotation marks in values.
 * - Thread-safe writeRow() via 'synchronized'.
 */
public class CsvLogger {

    private final String filePath;
    private final boolean append;

    public CsvLogger(String filePath, boolean append) {
        this.filePath = filePath;
        this.append = append;

        // Ensure output directory exists
        Path parentDir = Path.of(filePath).getParent();
        if (parentDir != null) {
            try {
                Files.createDirectories(parentDir);
            } catch (IOException e) {
                System.err.println("⚠️ Could not create output directory: " + parentDir);
            }
        }

        // Write header if we are not in append mode (fresh CSV)
        if (!append) {
            writeRow(
                    "id",
                    "indexName",
                    "queryFile",
                    "docName",
                    "payrollType",
                    "language",
                    "year",
                    "month",
                    "status",
                    "clientTimeMs",
                    "osTookMs",
                    "totalHits"
            );
        }
    }
    /**
     * Writes a single CSV row to the configured file.
     * Each column is converted to String, quotes are escaped and
     * columns are separated by commas.
     */
    @SneakyThrows
    public synchronized void writeRow(Object... cols) {
        try (FileWriter fw = new FileWriter(filePath, true)) {
            for (int i = 0; i < cols.length; i++) {
                if (i > 0) fw.write(",");
                String value = cols[i] != null ? cols[i].toString() : "";
                value = value.replace("\"", "\"\""); // escapamos comillas
                fw.write(value);
            }
            fw.write("\n");
        }
    }
}


