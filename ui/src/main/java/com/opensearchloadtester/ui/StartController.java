package com.opensearchloadtester.ui;

//import com.opensearchloadtester.loadgenerator.model.QueryType;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
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

    private final Path envPath = Path.of(".env");
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
        testdataGenerationMode.valueProperty().addListener((obs,
                                                            oldValue, newValue) -> {
            if (!suppressListeners && Objects.equals(scenarioConfig.getValue(), "default-scenario")) {
                scenarioConfig.setValue(null);
            }
        });

        testdataGenerationDocumentType.getItems().addAll(
                "ANO",
                "DUO"
        );

        // When document type changes, update scenario files dropdown
        testdataGenerationDocumentType.valueProperty().addListener((obs,
                                                                    oldValue, newValue) -> {
            if (Objects.equals(newValue, "ANO") && Objects.equals(scenarioConfig.getValue(), "default-scenario")) {
                return;
            }

            updateScenarioConfigForDocumentType(newValue);
        });

        scenarioConfig.getItems().add("default-scenario");

        scenarioConfig.valueProperty().addListener((obs,
                                                    oldValue, newValue) -> {
            suppressListeners = true;
            if (Objects.equals(newValue, "default-scenario")) {
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

        testdataGenerationCount.textProperty().addListener((obs,
                                                            oldValue, newValue) -> {
            if (!suppressListeners && Objects.equals(scenarioConfig.getValue(), "default-scenario")) {
                scenarioConfig.setValue(null);
            }
        });

        loadGeneratorReplicas.textProperty().addListener((obs,
                                                          oldValue, newValue) -> {
            if (!suppressListeners && Objects.equals(scenarioConfig.getValue(), "default-scenario")) {
                scenarioConfig.setValue(null);
            }
        });

        metricsBatchSize.textProperty().addListener((obs,
                                                     oldValue, newValue) -> {
            if (!suppressListeners && Objects.equals(scenarioConfig.getValue(), "default-scenario")) {
                scenarioConfig.setValue(null);
            }
        });

//        ArrayList<String> queryTypes = new ArrayList<>();
//        for (QueryType qt : QueryType.values()) {
//            queryTypes.add(qt.name());
//        }
//        addCheckboxes(queryTypes, 3);
    }

    @FXML
    protected void onStartLoadTest() {
        try {

            Alert alertStart = new Alert(Alert.AlertType.INFORMATION);
            alertStart.setTitle("Info");
            alertStart.setHeaderText("Docker Startup...");
            alertStart.setContentText("Old Docker containers are being removed, build and started. This may take a while. Please visit http://localhost:3000 afterwards.");
            alertStart.show();

            writeEnvFile();

            if (!checkOpensearchRunning() && !checkCorrectTestdataInOpenSearch()) {
                dockerClean();
                dockerBuild();
                dockerRun();
            } else {
                dockerRestart();
            }

            alertStart.close();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Error while starting Docker containers");
            alert.setContentText("Errors have occurred : " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    protected void onCloseButtonClick() {
        dockerClean();
    }

    private void writeEnvFile() throws IOException {
        String content = Files.readString(envPath);

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

        Files.writeString(envPath, content);
    }

    private String regexInEnv(String content, String key, String value) {
        String regex = "(?m)^" + key + "=.*$";
        String replacement = key + "=" + value;
        return content.replaceAll(regex, replacement);
    }

    private void dockerBuild() {
        processBuilder.command("sh", "-c", "docker compose -f docker-compose.yaml build");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void dockerRun() {
        processBuilder.command("sh", "-c", "docker compose up -d");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void dockerStop() {
        processBuilder.command("sh", "-c", "docker compose down");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void dockerClean() {
        processBuilder.command("sh", "-c", "docker compose down --volumes --rmi local --remove-orphans");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private boolean checkOpensearchRunning() {
        processBuilder.command("sh", "-c", "docker ps --filter \"name=name=test-target-opensearch\" --filter \"status=running\" --format \"{{.Names}}\"");
        try {
            int exitCode = processBuilder.start().waitFor();
            if (exitCode == 0) {
                return true;
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }
        return false;
    }

    private boolean checkCorrectTestdataInOpenSearch() {
        String dataType = testdataGenerationDocumentType.getValue().toLowerCase();
        String url = "http://localhost:9200/" + dataType + "-index/_count";

        // TODO Exeption handling
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (response.statusCode() != 200) {
            return false;
        }

        String jsonResponse = response.body();
        Pattern pattern = Pattern.compile("\"count\":(\\d+)");
        Matcher matcher = pattern.matcher(jsonResponse);
        // respose: {"count":1000,"_shards":{"total":5,"successful":5,"skipped":0,"failed":0}}
        String countString = matcher.group(1);
        int count = Integer.parseInt(countString);

        // return true if more count in Opensearch than in env
        return count >= Integer.parseInt(testdataGenerationCount.getText());

    }


    private void dockerRestart() {
        processBuilder.command("sh", "-c", "docker compose restart load-generator metrics-reporter");
        try {
            processBuilder.start().waitFor();
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
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
                    .map(p -> {
                        String filenameWithExt = p.getFileName().toString();
                        return filenameWithExt.substring(0, filenameWithExt.length() - 5);
                    })
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

        if (!Objects.equals(oldValue, "default-scenario") && scenarios.contains(oldValue)) {
            scenarioConfig.setValue(oldValue);
        }
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
