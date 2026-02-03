package com.opensearchloadtester.ui;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;

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

    stage.setMinWidth(800);
    stage.setMinHeight(500);

    stage.setWidth(Math.min(800, screenBounds.getWidth()));
    stage.setHeight(screenBounds.getHeight());

    stage
        .getIcons()
        .add(
            new Image(
                Objects.requireNonNull(
                    UIApplication.class.getResourceAsStream("loadtester-logo.png"))));

    if (Taskbar.isTaskbarSupported()) {
      Taskbar taskbar = Taskbar.getTaskbar();
      if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
        URL imageResource = UIApplication.class.getResource("loadtester-logo.png");
        java.awt.Image image = Toolkit.getDefaultToolkit().getImage(imageResource);
        taskbar.setIconImage(image);
      }
    }

    stage.show();
  }
}
