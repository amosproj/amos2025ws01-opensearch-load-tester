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
            // Create empty statistics
            TestRunReport.RoundtripStats emptyRoundtripStats = new TestRunReport.RoundtripStats(0.0, 0L, 0L);
            TestRunReport.OpenSearchTookStats emptyOpenSearchTookStats = new TestRunReport.OpenSearchTookStats(0.0, 0L, 0L);
            TestRunReport.Statistics emptyStatistics = new TestRunReport.Statistics(emptyRoundtripStats, emptyOpenSearchTookStats);
            
            TestRunReport emptyReport = new TestRunReport(
                emptyStatistics,
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
        
        // Recalculate statistics with all query results
        TestRunReport.Statistics updatedStatistics = calculateStatistics(existingReport.getQueryResults());
        existingReport.setStatistics(updatedStatistics);
        
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
            String jsonResponseStr = metrics.getJsonResponse(i);
            
            // Parse JSON response
            com.fasterxml.jackson.databind.JsonNode jsonResponse = null;
            Long opensearchTookMs = null;
            Integer hitsCount = null;
            boolean hasError = false;
            
            try {
                jsonResponse = objectMapper.readTree(jsonResponseStr);
                
                // Extract took
                if (jsonResponse.has("took")) {
                    opensearchTookMs = jsonResponse.get("took").asLong();
                }
                
                // Extract hits count
                if (jsonResponse.has("hits")) {
                    com.fasterxml.jackson.databind.JsonNode hitsNode = jsonResponse.get("hits");
                    if (hitsNode.has("total") && hitsNode.get("total").has("value")) {
                        hitsCount = hitsNode.get("total").get("value").asInt();
                    }
                }
                
                // Check for errors
                hasError = jsonResponse.has("error");
                
            } catch (Exception e) {
                log.warn("Failed to parse JSON response for {}: {}", requestType, e.getMessage());
                hasError = true;
            }
            
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
                String jsonResponseStr = metrics.getJsonResponse(i);
                
                // Parse JSON response
                com.fasterxml.jackson.databind.JsonNode jsonResponse = null;
                Long opensearchTookMs = null;
                Integer hitsCount = null;
                boolean hasError = false;
                
                try {
                    jsonResponse = objectMapper.readTree(jsonResponseStr);
                    
                    // Extract took
                    if (jsonResponse.has("took")) {
                        opensearchTookMs = jsonResponse.get("took").asLong();
                    }
                    
                    // Extract hits count
                    if (jsonResponse.has("hits")) {
                        com.fasterxml.jackson.databind.JsonNode hitsNode = jsonResponse.get("hits");
                        if (hitsNode.has("total") && hitsNode.get("total").has("value")) {
                            hitsCount = hitsNode.get("total").get("value").asInt();
                        }
                    }
                    
                    // Check for errors
                    hasError = jsonResponse.has("error");
                    if (hasError) {
                        totalErrors++;
                    }
                    
                } catch (Exception e) {
                    log.warn("Failed to parse JSON response for {}: {}", requestType, e.getMessage());
                    hasError = true;
                    totalErrors++;
                }
                
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

        // Calculate statistics
        TestRunReport.Statistics statistics = calculateStatistics(queryResults);

        TestRunReport report = new TestRunReport(
            statistics,
            LocalDateTime.now(),
            queryResults.size(),
            totalErrors,
            queryResults,
            instances
        );

        log.info("Report created with {} total queries, {} errors, from {} instances", 
            report.getTotalQueries(), report.getTotalErrors(), instances.size());
        log.info("Statistics - Roundtrip: avg={}ms, min={}ms, max={}ms | OpenSearch Took: avg={}ms, min={}ms, max={}ms",
            String.format("%.2f", statistics.getRoundtripMs().getAverage()),
            statistics.getRoundtripMs().getMin(),
            statistics.getRoundtripMs().getMax(),
            String.format("%.2f", statistics.getOpensearchTookMs().getAverage()),
            statistics.getOpensearchTookMs().getMin(),
            statistics.getOpensearchTookMs().getMax());

        return report;
    }
    
    /**
     * Calculates aggregated statistics from query results.
     *
     * @param queryResults List of query results to analyze
     * @return Statistics object with averages, min, and max values
     */
    private TestRunReport.Statistics calculateStatistics(List<QueryResult> queryResults) {
        // Collect roundtrip values (filter out nulls)
        List<Long> roundtripValues = queryResults.stream()
            .map(QueryResult::getRoundtripMs)
            .filter(value -> value != null)
            .collect(Collectors.toList());
        
        // Collect OpenSearch took values (filter out nulls)
        List<Long> opensearchTookValues = queryResults.stream()
            .map(QueryResult::getOpensearchTookMs)
            .filter(value -> value != null)
            .collect(Collectors.toList());
        
        // Calculate roundtrip statistics
        TestRunReport.RoundtripStats roundtripStats = new TestRunReport.RoundtripStats();
        if (!roundtripValues.isEmpty()) {
            double roundtripAvg = roundtripValues.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
            long roundtripMin = roundtripValues.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0L);
            long roundtripMax = roundtripValues.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
            
            roundtripStats.setAverage(roundtripAvg);
            roundtripStats.setMin(roundtripMin);
            roundtripStats.setMax(roundtripMax);
        } else {
            roundtripStats.setAverage(0.0);
            roundtripStats.setMin(0L);
            roundtripStats.setMax(0L);
        }
        
        // Calculate OpenSearch took statistics
        TestRunReport.OpenSearchTookStats opensearchTookStats = new TestRunReport.OpenSearchTookStats();
        if (!opensearchTookValues.isEmpty()) {
            double opensearchTookAvg = opensearchTookValues.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
            long opensearchTookMin = opensearchTookValues.stream()
                .mapToLong(Long::longValue)
                .min()
                .orElse(0L);
            long opensearchTookMax = opensearchTookValues.stream()
                .mapToLong(Long::longValue)
                .max()
                .orElse(0L);
            
            opensearchTookStats.setAverage(opensearchTookAvg);
            opensearchTookStats.setMin(opensearchTookMin);
            opensearchTookStats.setMax(opensearchTookMax);
        } else {
            opensearchTookStats.setAverage(0.0);
            opensearchTookStats.setMin(0L);
            opensearchTookStats.setMax(0L);
        }
        
        return new TestRunReport.Statistics(roundtripStats, opensearchTookStats);
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
     * Helper method to convert JsonNode to compact string for CSV export.
     *
     * @param jsonNode The JsonNode to convert
     * @return Compact string representation suitable for CSV
     */
    private String escapeForCsv(com.fasterxml.jackson.databind.JsonNode jsonNode) {
        if (jsonNode == null) {
            return "";
        }
        // Convert to compact string (no pretty printing for CSV)
        return jsonNode.toString();
    }
}


