package com.opensearchloadtester.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class StartController {
    @FXML
    private Label welcomeText;
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

    private final Path envPath = Path.of(".env");

    @FXML
    public void initialize() {
        testdataGenerationMode.getItems().addAll(
                "DYNAMIC",
                "PERSISTANT"
        );
        testdataGenerationMode.setValue("dynamic");

        testdataGenerationDocumentType.getItems().addAll(
                "ANO",
                "DUO"
        );
        // When document type changes, update scenario files dropdown
        testdataGenerationDocumentType.valueProperty().addListener((obs,
                                                                    oldValue, newValue) -> {
            scenarioConfig.getItems().clear();
            scenarioConfig.getItems().addAll(
                    getScenarioFiles(newValue)
            );
        });


        testdataGenerationDocumentType.setValue("ANO");

//        scenarioConfig.getItems().addAll(
//                getScenarioFiles(testdataGenerationDocumentType.getValue())
//        );

        testdataGenerationCount.setText("10000");
        loadGeneratorReplicas.setText("3");
        metricsBatchSize.setText("100");

    }

    @FXML
    protected void onStartLoadTest() {

        try {

            Alert alertStart = new Alert(Alert.AlertType.INFORMATION);
            alertStart.setTitle("Info");
            alertStart.setHeaderText("Docker Startup...");
            alertStart.setContentText("Old Docker containers are being removed, build and started. This may take a while. Please visit http://localhost:3000 afterwards.");
            alertStart.showAndWait();

            writeEnvFile();
            dockerClean();
            dockerBuild();
            dockerRun();
            
            alertStart.close();

        } catch (IOException e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Fehler beim Starten des Load Tests");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Info");
        alert.setHeaderText("Load Test gestartet");
        alert.setContentText("Der Load Test läuft jetzt.");

        alert.showAndWait();

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
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", "docker compose -f docker-compose.yaml build");
        processBuilder.inheritIO();
        try {
            processBuilder.start();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void dockerRun() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", "docker compose up -d");
        processBuilder.inheritIO();
        try {
            processBuilder.start();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void dockerStop() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", "docker compose down");
        processBuilder.inheritIO();
        try {
            processBuilder.start();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void dockerClean() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", "docker compose down --volumes --rmi local --remove-orphans");
        processBuilder.inheritIO();
        try {
            processBuilder.start();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private void dockerRestart() {
        ProcessBuilder processBuilder = new ProcessBuilder();
        // TODO: maybe just again docker compose up -d
        processBuilder.command("sh", "-c", "docker compose restart alloy load-generator metrics-reporter");
        processBuilder.inheritIO();
        try {
            processBuilder.start();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Es ist ein Fehler aufgetreten: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private List<String> getScenarioFiles(String docType) {
        List<String> result = new ArrayList<>();
        Path path;

        try {
            // try to load through classloader
            URL url = getClass().getClassLoader().getResource("scenarios");
            if (url != null) {

                path = Paths.get(url.toURI());
                Files.list(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".yaml"))
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.contains(docType.toLowerCase()) || (!name.contains("ano") && !name.contains("duo"));
                        })
                        .forEach(p -> result.add(p.getFileName().toString()));

            }
            // if nothing found, try with absolute path
            if (result.isEmpty()) {
                path = Paths.get("./load-generator/src/main/resources/scenarios");
                Files.list(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".yaml"))
                        .filter(p -> {
                            String name = p.getFileName().toString().toLowerCase();
                            return name.contains(docType.toLowerCase()) || (!name.contains("ano") && !name.contains("duo"));
                        })
                        .forEach(p -> result.add(p.getFileName().toString()));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
