module com.opensearchloadtester.ui {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires javafx.base;


    opens com.opensearchloadtester.ui to javafx.fxml;
    exports com.opensearchloadtester.ui;
}
