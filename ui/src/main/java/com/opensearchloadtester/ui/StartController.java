package com.opensearchloadtester.ui;

//import com.opensearchloadtester.loadgenerator.model.QueryType;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class StartController {

    @FXML
    private ComboBox<String> testdataGenerationMode;
    @FXML
    private ComboBox<String> testdataGenerationDocumentType;
    @FXML
    private ComboBox<String> scenarioConfig;
    @FXML
    private TextField testdataGenerationCount;
    @FXML
    private TextField loadGeneratorReplicas;
    @FXML
    private TextField metricsBatchSize;
    @FXML
    private VBox customScenarioConfigurationBox;
    @FXML
    private VBox dynamicCheckboxWrapper;
    @FXML
    private TextArea outputText;

    private final Path ENV_PATH = Path.of(".env");
    private final ProcessBuilder processBuilder = new ProcessBuilder();

    private boolean suppressListeners = false;

    @FXML
    public void initialize() {

        // set all (sub)process outputs to console
        processBuilder.inheritIO();

        testdataGenerationMode.getItems().addAll(
                "DYNAMIC",
                "PERSISTANT"
        );
        testdataGenerationMode.valueProperty().addListener(getDefaultScenarioRemovalListener());

        testdataGenerationDocumentType.getItems().addAll(
                "ANO",
                "DUO"
        );

        // When document type changes, update scenario files dropdown
        testdataGenerationDocumentType.valueProperty().addListener((obs,
                                                                    oldValue, newValue) -> {
            if (Objects.equals(newValue, "ANO") && Objects.equals(scenarioConfig.getValue(), "default-scenario.yaml")) {
                return;
            }

            updateScenarioConfigForDocumentType(newValue);
        });

        updateScenarioConfigForDocumentType("ANO");

        scenarioConfig.valueProperty().addListener((obs,
                                                    oldValue, newValue) -> {
            suppressListeners = true;
            if (Objects.equals(newValue, "default-scenario.yaml")) {
                // set default values
                testdataGenerationDocumentType.setValue("ANO");
                testdataGenerationMode.setValue("DYNAMIC");
                testdataGenerationCount.setText("10000");
                loadGeneratorReplicas.setText("1");
                metricsBatchSize.setText("100");
            } else if (Objects.equals(newValue, "custom-scenario")) {
                customScenarioConfigurationBox.setVisible(true);
                customScenarioConfigurationBox.setDisable(false);
            } else {
                customScenarioConfigurationBox.setVisible(false);
                customScenarioConfigurationBox.setDisable(true);
            }
            suppressListeners = false;
        });

        testdataGenerationCount.textProperty().addListener(getDefaultScenarioRemovalListener());
        loadGeneratorReplicas.textProperty().addListener(getDefaultScenarioRemovalListener());
        metricsBatchSize.textProperty().addListener(getDefaultScenarioRemovalListener());

        // TODO implement query type checkboxes
//        ArrayList<String> queryTypes = new ArrayList<>();
//        for (QueryType qt : QueryType.values()) {
//            queryTypes.add(qt.name());
//        }
//        addCheckboxes(queryTypes, 3);
    }

    @FXML
    protected void onStartLoadTest() {
        outputText.setVisible(true);
        outputText.setText(
                "Starting OpenSearch Loadtester...\n" +
                        "\n" +
                        "Please wait until all steps are complete.\n" +
                        "Once finished, visit your dashboard at http://localhost:3000\n" +
                        "\n" +
                        "Please do not close this application while the load test is running.\n\n"
        );
        Thread thread = new Thread(() -> {
            String currScenario = scenarioConfig.getValue();
            executeTimed("Step 1: Applying test configuration...", this::writeEnvFile);

            if (!checkOpensearchRunning()) {
                executeTimed("Step 2: Cleaning old Docker containers...", this::dockerClean);
                executeTimed("Step 3: Building Docker images...", this::dockerBuild);
                executeTimed("Step 4: Running Docker containers...", this::dockerRun);
            } else {
                executeTimed("Step 2: Ensuring testdata amount...", this::ensureTestdataAmountInOpenSearch);
                ensureTestdataAmountInOpenSearch();
                executeTimed("Step 3: Restarting Docker containers...", this::dockerRestart);
            }
            Platform.runLater(() -> outputText.appendText("\n" + currScenario + " is running.\n"));
        });
        thread.start();

    }

    @FXML
    protected void onCloseButtonClick() {
        executeTimed("\nCleaning everything...", this::dockerClean);
    }

    private void writeEnvFile() {
        String content = null;
        try {
            content = Files.readString(ENV_PATH);
        } catch (IOException e) {
            System.err.println("Error reading .env file: " + e.getMessage());
            return;
        }

        content = regexInEnv(
                content,
                "TEST_DATA_GENERATION_MODE",
                testdataGenerationMode.getValue()
        );

        content = regexInEnv(
                content,
                "TEST_DATA_GENERATION_COUNT",
                testdataGenerationCount.getText()
        );

        content = regexInEnv(
                content,
                "TEST_DATA_GENERATION_DOCUMENT_TYPE",
                testdataGenerationDocumentType.getValue()
        );

        content = regexInEnv(
                content,
                "LOAD_GENERATOR_REPLICAS",
                loadGeneratorReplicas.getText()
        );

        content = regexInEnv(
                content,
                "METRICS_BATCH_SIZE",
                metricsBatchSize.getText()
        );

        content = regexInEnv(
                content,
                "SCENARIO_CONFIG",
                scenarioConfig.getValue()
        );

        try {
            Files.writeString(ENV_PATH, content);
        } catch (IOException e) {
            System.err.println("Error writing .env file: " + e.getMessage());
            return;
        }
        System.out.println(".env file updated successfully.");
    }

    private String regexInEnv(String content, String key, String value) {
        String regex = "(?m)^" + key + "=.*$";
        String replacement = key + "=" + value;
        return content.replaceAll(regex, replacement);
    }

    private void dockerBuild() {
        System.out.println("Building Docker images...");
        processBuilder.command("sh", "-c", "docker compose -f docker-compose.yaml build");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error building Docker images");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void dockerRun() {
        System.out.println("Running Docker containers...");
        processBuilder.command("sh", "-c", "docker compose up -d");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error running Docker containers");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void dockerStop() {
        System.out.println("Stopping Docker containers...");
        processBuilder.command("sh", "-c", "docker compose down");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error stopping Docker containers");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void dockerClean() {
        System.out.println("Cleaning old Docker containers, volumes, and images...");
        processBuilder.command("sh", "-c", "docker compose down --volumes --rmi local --remove-orphans");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error cleaning Docker");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private boolean checkOpensearchRunning() {
        String url = "http://localhost:9200";

        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .timeout(Duration.ofSeconds(2))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("OpenSearch is running.");
                return true;
            } else {
                System.out.println("OpenSearch responded with status: " + response.statusCode());
                return false;
            }

        } catch (Exception e) {
            System.out.println("OpenSearch is not running: " + e.getClass().getName());
            return false;
        }
    }

    private void ensureTestdataAmountInOpenSearch() {
        System.out.println("Checking if OpenSearch contains the required test data...");
        String dataType = testdataGenerationDocumentType.getValue().toLowerCase();
        String url = "http://localhost:9200/" + dataType + "-index/_count";

        int newTestdataAmount = Integer.parseInt(testdataGenerationCount.getText());

        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("OpenSearch count check failed with status code: " + response.statusCode());
                addMoreTestdata(newTestdataAmount);
                return;
            }

            String jsonResponse = response.body();
            Pattern pattern = Pattern.compile("\"count\":(\\d+)");
            Matcher matcher = pattern.matcher(jsonResponse);

            if (matcher.find()) {
                String countString = matcher.group(1);
                int count = Integer.parseInt(countString);

                System.out.println("OpenSearch contains " + count + " documents, required are "
                        + testdataGenerationCount.getText() + " documents.");

                if (count < newTestdataAmount) {
                    addMoreTestdata(newTestdataAmount - count);
                }
            }
        } catch (Exception e) {
            System.err.println("Error checking OpenSearch count: " + e.getMessage());
            addMoreTestdata(newTestdataAmount);
        }
    }


    private void dockerRestart() {
        processBuilder.command("sh", "-c", "docker compose up -d --no-deps load-generator metrics-reporter");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error restarting Docker containers");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void addMoreTestdata(int amount) {
        processBuilder.command("sh", "-c", "TEST_DATA_GENERATION_COUNT=" + amount + " docker compose up -d testdata-generator");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error restarting Docker containers");
            alert.setContentText("An error occurred: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private List<String> getScenarioFiles(String docType) {

        Path scenarioDir = Paths.get("./load-generator/src/main/resources/scenarios");

        if (!Files.exists(scenarioDir)) {
            System.err.println("Scenario directory not found: " + scenarioDir.toAbsolutePath());
            return List.of();
        }

        try (Stream<Path> files = Files.list(scenarioDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".yaml"))
                    .map(p -> p.getFileName().toString())
                    .filter(name -> {
                        String lower = name.toLowerCase();
                        return lower.contains(docType.toLowerCase())
                                || (!lower.contains("ano") && !lower.contains("duo"));
                    })
                    .toList();
        } catch (IOException e) {
            System.err.println("Error getting scenario files: " + e.getMessage());
            return List.of();
        }
    }

    private void updateScenarioConfigForDocumentType(String documentType) {
        List<String> scenarios = getScenarioFiles(documentType);
        if (scenarios.isEmpty()) {
            scenarioConfig.getItems().clear();
            System.err.println("No scenario files found for document type: " + documentType);
            return;
        }
        String oldValue = scenarioConfig.getValue();

        scenarioConfig.getItems().clear();
        scenarioConfig.getItems().addAll(scenarios);

        if (!Objects.equals(oldValue, "default-scenario.yaml") && scenarios.contains(oldValue)) {
            scenarioConfig.setValue(oldValue);
        }
    }

    private ChangeListener<String> getDefaultScenarioRemovalListener() {
        return (observable, oldValue, newValue) -> {
            if (!suppressListeners && Objects.equals(scenarioConfig.getValue(), "default-scenario.yaml")) {
                scenarioConfig.setValue(null);
            }
        };
    }

    private void executeTimed(String logBefore, Runnable runnable) {
        Platform.runLater(() -> outputText.appendText(logBefore));
        long startTime = System.currentTimeMillis();
        runnable.run();
        double duration = (System.currentTimeMillis() - startTime) / 1000.0;
        Platform.runLater(() -> outputText.appendText(" Done (" + duration + "s).\n\n"));
    }

    private void addCheckboxes(List<String> options, int perRow) {
        dynamicCheckboxWrapper.getChildren().clear(); // alte HBoxes entfernen

        HBox currentHBox = null;
        int count = 0;

        for (String option : options) {
            if (count % perRow == 0) {
                currentHBox = new HBox();
                currentHBox.setSpacing(10);
                currentHBox.setAlignment(javafx.geometry.Pos.CENTER);
                dynamicCheckboxWrapper.getChildren().add(currentHBox);
            }

            CheckBox checkBox = new CheckBox(option);
            checkBox.setId("checkbox_" + option);
            currentHBox.getChildren().add(checkBox);
            count++;
        }
    }
}
