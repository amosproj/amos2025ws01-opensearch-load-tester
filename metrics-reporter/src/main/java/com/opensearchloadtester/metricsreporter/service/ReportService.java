package com.opensearchloadtester.metricsreporter.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.opensearchloadtester.metricsreporter.dto.Metrics;
import com.opensearchloadtester.metricsreporter.dto.QueryResult;
import com.opensearchloadtester.metricsreporter.dto.TestRunReport;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service responsible for creating and exporting test run reports.
 * Supports JSON and CSV export formats for raw query data.
 */
@Slf4j
@Service
public class ReportService {

    private final ObjectMapper objectMapper;
    
    @Value("${report.output.directory:./reports}")
    private String outputDirectory;
    
    @Value("${report.json.filename:test_run_report.json}")
    private String jsonFilename;
    
    @Value("${report.csv.filename:test_run_report.csv}")
    private String csvFilename;

    public ReportService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    /**
     * Initializes the report directory and creates empty report files if they don't exist.
     * Should be called at service startup or when starting a new test run.
     */
    public void initializeReportFiles() throws IOException {
        Path dirPath = Paths.get(outputDirectory);
        
        // Create directory if it doesn't exist
        if (!Files.exists(dirPath)) {
            Files.createDirectories(dirPath);
            log.info("Created report output directory: {}", dirPath.toAbsolutePath());
        }
        
        Path jsonPath = Paths.get(outputDirectory, jsonFilename);
        Path csvPath = Paths.get(outputDirectory, csvFilename);
        
        // Create empty JSON file with initial structure
        if (!Files.exists(jsonPath)) {
            TestRunReport emptyReport = new TestRunReport(
                LocalDateTime.now(),
                0,
                0,
                new ArrayList<>(),
                new ArrayList<>()
            );
            String jsonContent = objectMapper.writeValueAsString(emptyReport);
            Files.writeString(jsonPath, jsonContent);
            log.info("Created initial JSON report file: {}", jsonPath.toAbsolutePath());
        }
        
        // Create CSV file with headers
        if (!Files.exists(csvPath)) {
            String csvHeaders = "Load Generator Instance,Request Type,Roundtrip (ms),OpenSearch Took (ms),Hits Count,Has Error,JSON Response\n";
            Files.writeString(csvPath, csvHeaders);
            log.info("Created initial CSV report file: {}", csvPath.toAbsolutePath());
        }
    }
    
    /**
     * Appends new metrics to the existing JSON report file.
     * Reads the current report, adds new query results, and writes it back.
     *
     * @param metrics New metrics to append
     * @throws IOException if file operations fail
     */
    public void appendToJsonReport(Metrics metrics) throws IOException {
        Path jsonPath = Paths.get(outputDirectory, jsonFilename);
        
        // Ensure directory and file exist
        if (!Files.exists(jsonPath)) {
            initializeReportFiles();
        }
        
        // Read existing report
        String existingJson = Files.readString(jsonPath);
        TestRunReport existingReport = objectMapper.readValue(existingJson, TestRunReport.class);
        
        // Convert new metrics to QueryResults
        List<QueryResult> newResults = convertMetricsToQueryResults(metrics);
        
        // Append to existing results
        existingReport.getQueryResults().addAll(newResults);
        
        // Update statistics
        existingReport.setReportGeneratedAt(LocalDateTime.now());
        existingReport.setTotalQueries(existingReport.getQueryResults().size());
        
        // Count errors
        int totalErrors = (int) existingReport.getQueryResults().stream()
            .filter(QueryResult::getHasError)
            .count();
        existingReport.setTotalErrors(totalErrors);
        
        // Update load generator instances
        List<String> instances = existingReport.getQueryResults().stream()
            .map(QueryResult::getLoadGeneratorInstance)
            .distinct()
            .collect(Collectors.toList());
        existingReport.setLoadGeneratorInstances(instances);
        
        // Write updated report back to file
        String updatedJson = objectMapper.writeValueAsString(existingReport);
        Files.writeString(jsonPath, updatedJson);
        
        log.info("Appended {} query results to JSON report. Total queries: {}, Total errors: {}", 
            newResults.size(), existingReport.getTotalQueries(), existingReport.getTotalErrors());
    }
    
    /**
     * Appends new metrics to the existing CSV report file.
     * Adds new rows to the CSV without rewriting the entire file.
     *
     * @param metrics New metrics to append
     * @throws IOException if file operations fail
     */
    public void appendToCsvReport(Metrics metrics) throws IOException {
        Path csvPath = Paths.get(outputDirectory, csvFilename);
        
        // Ensure directory and file exist
        if (!Files.exists(csvPath)) {
            initializeReportFiles();
        }
        
        // Convert metrics to QueryResults
        List<QueryResult> queryResults = convertMetricsToQueryResults(metrics);
        
        // Append to CSV file
        try (FileWriter fileWriter = new FileWriter(csvPath.toFile(), true);
             CSVPrinter csvPrinter = new CSVPrinter(fileWriter, CSVFormat.DEFAULT)) {
            
            for (QueryResult result : queryResults) {
                csvPrinter.printRecord(
                    result.getLoadGeneratorInstance(),
                    result.getRequestType(),
                    result.getRoundtripMs(),
                    result.getOpensearchTookMs(),
                    result.getHitsCount(),
                    result.getHasError(),
                    escapeForCsv(result.getJsonResponse())
                );
            }
            
            csvPrinter.flush();
        }
        
        log.info("Appended {} query results to CSV report", queryResults.size());
    }
    
    /**
     * Helper method to convert a Metrics object to a list of QueryResult objects.
     *
     * @param metrics Metrics object to convert
     * @return List of QueryResult objects
     */
    private List<QueryResult> convertMetricsToQueryResults(Metrics metrics) {
        List<QueryResult> queryResults = new ArrayList<>();
        String instanceName = metrics.getLoadGeneratorInstance();
        
        for (int i = 0; i < metrics.getRequestType().size(); i++) {
            String requestType = metrics.getRequestType(i);
            Long roundtripMs = metrics.getRoundtripMilSec(i);
            String jsonResponse = metrics.getJsonResponse(i);
            
            // Check if response contains an error
            boolean hasError = jsonResponse != null && jsonResponse.contains("\"error\"");
            
            // Extract took and hits from JSON response
            Long opensearchTookMs = extractTookFromResponse(jsonResponse);
            Integer hitsCount = extractHitsCountFromResponse(jsonResponse);
            
            QueryResult queryResult = new QueryResult(
                requestType,
                roundtripMs,
                opensearchTookMs,
                hitsCount,
                jsonResponse,
                hasError,
                instanceName
            );
            
            queryResults.add(queryResult);
        }
        
        return queryResults;
    }
    
    /**
     * Extracts the "took" value from OpenSearch JSON response.
     *
     * @param jsonResponse The raw JSON response string
     * @return The took value in milliseconds, or null if not found
     */
    private Long extractTookFromResponse(String jsonResponse) {
        if (jsonResponse == null) {
            return null;
        }
        try {
            // Simple regex to extract "took":123
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"took\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                return Long.parseLong(matcher.group(1));
            }
        } catch (Exception e) {
            log.debug("Could not extract 'took' from response: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Extracts the hits count from OpenSearch JSON response.
     *
     * @param jsonResponse The raw JSON response string
     * @return The number of hits, or null if not found
     */
    private Integer extractHitsCountFromResponse(String jsonResponse) {
        if (jsonResponse == null) {
            return null;
        }
        try {
            // Look for "hits":{"total":{"value":123
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "\"hits\"\\s*:\\s*\\{\\s*\"total\"\\s*:\\s*\\{\\s*\"value\"\\s*:\\s*(\\d+)"
            );
            java.util.regex.Matcher matcher = pattern.matcher(jsonResponse);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            log.debug("Could not extract 'hits.total.value' from response: {}", e.getMessage());
        }
        return null;
    }

    
    /**
     * Returns the absolute path to the JSON report file.
     *
     * @return Path to JSON report file
     */
    public Path getJsonReportPath() {
        return Paths.get(outputDirectory, jsonFilename).toAbsolutePath();
    }
    
    /**
     * Returns the absolute path to the CSV report file.
     *
     * @return Path to CSV report file
     */
    public Path getCsvReportPath() {
        return Paths.get(outputDirectory, csvFilename).toAbsolutePath();
    }

    /**
     * Creates a TestRunReport from a list of Metrics objects.
     * Converts all individual query results into a structured report format.
     *
     * @param metricsList List of Metrics from all load generator instances
     * @return TestRunReport containing all query results
     */
    public TestRunReport createReport(List<Metrics> metricsList) {
        log.info("Creating test run report from {} metrics objects", metricsList.size());

        List<QueryResult> queryResults = new ArrayList<>();
        int totalErrors = 0;

        // Process each Metrics object from different load generator instances
        for (Metrics metrics : metricsList) {
            String instanceName = metrics.getLoadGeneratorInstance();
            
            // Each Metrics object contains arrays of results
            for (int i = 0; i < metrics.getRequestType().size(); i++) {
                String requestType = metrics.getRequestType(i);
                Long roundtripMs = metrics.getRoundtripMilSec(i);
                String jsonResponse = metrics.getJsonResponse(i);
                
                // Check if response contains an error
                boolean hasError = jsonResponse != null && jsonResponse.contains("\"error\"");
                if (hasError) {
                    totalErrors++;
                }
                
                // Extract took and hits from JSON response
                Long opensearchTookMs = extractTookFromResponse(jsonResponse);
                Integer hitsCount = extractHitsCountFromResponse(jsonResponse);
                
                QueryResult queryResult = new QueryResult(
                    requestType,
                    roundtripMs,
                    opensearchTookMs,
                    hitsCount,
                    jsonResponse,
                    hasError,
                    instanceName
                );
                
                queryResults.add(queryResult);
            }
        }

        // Extract unique load generator instance names
        List<String> instances = metricsList.stream()
            .map(Metrics::getLoadGeneratorInstance)
            .distinct()
            .collect(Collectors.toList());

        TestRunReport report = new TestRunReport(
            LocalDateTime.now(),
            queryResults.size(),
            totalErrors,
            queryResults,
            instances
        );

        log.info("Report created with {} total queries, {} errors, from {} instances", 
            report.getTotalQueries(), report.getTotalErrors(), instances.size());

        return report;
    }

    /**
     * Exports a TestRunReport to JSON format.
     *
     * @param report The report to export
     * @return JSON string representation of the report
     * @throws IOException if JSON serialization fails
     */
    public String exportToJson(TestRunReport report) throws IOException {
        log.info("Exporting report to JSON format");
        return objectMapper.writeValueAsString(report);
    }

    /**
     * Exports a TestRunReport to CSV format.
     * Each row represents a single query result with all relevant fields.
     *
     * @param report The report to export
     * @return CSV string representation of the report
     * @throws IOException if CSV generation fails
     */
    public String exportToCsv(TestRunReport report) throws IOException {
        log.info("Exporting report to CSV format");

        StringWriter stringWriter = new StringWriter();
        
        // Define CSV headers
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
            .setHeader(
                "Load Generator Instance",
                "Request Type",
                "Roundtrip (ms)",
                "OpenSearch Took (ms)",
                "Hits Count",
                "Has Error",
                "JSON Response"
            )
            .build();

        try (CSVPrinter csvPrinter = new CSVPrinter(stringWriter, csvFormat)) {
            // Write each query result as a CSV row
            for (QueryResult result : report.getQueryResults()) {
                csvPrinter.printRecord(
                    result.getLoadGeneratorInstance(),
                    result.getRequestType(),
                    result.getRoundtripMs(),
                    result.getOpensearchTookMs(),
                    result.getHitsCount(),
                    result.getHasError(),
                    // Escape newlines and quotes in JSON response for CSV
                    escapeForCsv(result.getJsonResponse())
                );
            }
            
            csvPrinter.flush();
        }

        log.info("CSV export completed with {} rows", report.getQueryResults().size());
        return stringWriter.toString();
    }

    /**
     * Helper method to escape special characters in JSON for CSV export.
     * Replaces newlines and handles quotes properly.
     *
     * @param json The JSON string to escape
     * @return Escaped string suitable for CSV
     */
    private String escapeForCsv(String json) {
        if (json == null) {
            return "";
        }
        // Remove excessive whitespace and newlines for better CSV readability
        return json.replaceAll("\\s+", " ").trim();
    }
}


