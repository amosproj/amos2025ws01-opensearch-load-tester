package com.opensearchloadtester.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.*;
import java.io.IOException;
import java.net.URL;

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

        stage.setWidth(Math.min(1000, screenBounds.getWidth()));
        stage.setHeight(Math.min(600, screenBounds.getHeight()));

        stage.getIcons().add(new Image(UIApplication.class.getResourceAsStream("loadtester-logo.png")));

        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
                URL imageResource = UIApplication.class.getResource("loadtester-logo.png");
                java.awt.Image image = Toolkit.getDefaultToolkit().getImage(imageResource);
                taskbar.setIconImage(image);
            }
        }

        stage.sizeToScene();

        stage.show();
    }
}
