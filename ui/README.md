# Loadtester Java UI

This is a simple JavaFX UI to configure and start the Loadtester stack via Docker Compose.
It provides a graphical interface so you don’t need to run commands in the terminal.
Under the hood, it still executes the required Docker commands (building images, starting containers, etc.).

## Build & Start

To build the Java UI, you need to verify that you have `javafx` in the lib folder and Maven installed.
We have provided a Shell script (`start-ui.sh`) in the **root folder** of the LoadTester project to simplify the build process.
Run the following command in the `ui` directory:

```bash
  bash ./start-ui.sh
```

It will also start the UI after building it.
