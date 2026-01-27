module com.opensearchloadtester.ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires javafx.base;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.dataformat.yaml;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.opensearchloadtester.ui to javafx.fxml;
    opens com.opensearchloadtester.ui.config to com.fasterxml.jackson.databind;
    exports com.opensearchloadtester.ui;
}
