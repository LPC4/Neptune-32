package org.lpc.visualization.debug;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lombok.Getter;

import java.io.OutputStream;
import java.io.PrintStream;

public class ExternalConsole {

    private final TextArea consoleTextArea = new TextArea();
    @Getter
    private Scene scene;

    public void start() {
        start(640, 400, 600, 600);
    }

    public void start(double width, double height, double x, double y) {
        Stage stage = new Stage();
        stage.setTitle("External Console");

        consoleTextArea.setEditable(false);
        consoleTextArea.setWrapText(true);
        consoleTextArea.setStyle(
                "-fx-control-inner-background: #121212; " +  // Dark background
                        "-fx-text-fill: #e0e0e0; " +                  // Light text
                        "-fx-font-family: 'Consolas'; " +            // Monospace font
                        "-fx-font-size: 12px; " +
                        "-fx-background-insets: 0;"                   // Remove padding background
        );

        consoleTextArea.setMouseTransparent(false);
        consoleTextArea.setFocusTraversable(true);

        VBox root = new VBox(consoleTextArea);
        root.setStyle("-fx-background-color: #121212;");
        VBox.setVgrow(consoleTextArea, javafx.scene.layout.Priority.ALWAYS);

        scene = new Scene(root, width, height);

        stage.setScene(scene);
        stage.setX(x);
        stage.setY(y);
        stage.show();

        redirectSystemStreams();
    }

    private void redirectSystemStreams() {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;

        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                appendText(String.valueOf((char) b));
                originalOut.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) {
                String text = new String(b, off, len);
                appendText(text);
                originalOut.write(b, off, len);
            }

            private void appendText(String text) {
                Platform.runLater(() -> consoleTextArea.appendText(text));
            }
        };

        System.setOut(new PrintStream(out, true));
        System.setErr(originalErr); // Leave System.err as-is
    }
}
