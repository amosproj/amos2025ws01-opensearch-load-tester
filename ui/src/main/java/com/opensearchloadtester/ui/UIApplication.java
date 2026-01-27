package com.opensearchloadtester.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;

public class UIApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(UIApplication.class.getResource("start-view.fxml"));

        ScrollPane root = fxmlLoader.load();

        Scene scene = new Scene(root);
        stage.setTitle("Load-Test Configuration");
        stage.setScene(scene);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        stage.setMaxWidth(screenBounds.getWidth());
        stage.setMaxHeight(screenBounds.getHeight());

        stage.setWidth(Math.min(800, screenBounds.getWidth()));
        stage.setHeight(Math.min(600, screenBounds.getHeight()));

        stage.sizeToScene();

        stage.show();
    }
}
