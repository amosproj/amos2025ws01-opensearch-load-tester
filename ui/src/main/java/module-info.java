module com.opensearchloadtester.ui {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.opensearchloadtester.ui to javafx.fxml;
    exports com.opensearchloadtester.ui;
}