package org.lpc.visualization.debug;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.lpc.CPU;

public class CpuViewer {
    private final CPU cpu;
    private final Label[] registerLabels = new Label[16];
    private final Label programCounterLabel = new Label();
    private final Label stackPointerLabel = new Label();
    private final Label heapPointerLabel = new Label();
    private final Label flagsLabel = new Label();

    private static final Font TITLE_FONT = Font.font("Segoe UI", FontWeight.BOLD, 20);
    private static final Font SECTION_FONT = Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14);
    private static final Font LABEL_FONT = Font.font("Consolas", FontWeight.NORMAL, 12);
    private static final Font VALUE_FONT = Font.font("Consolas", FontWeight.BOLD, 12);

    private static final String BACKGROUND_COLOR = "#f8f9fa";
    private static final String CARD_COLOR = "#ffffff";
    private static final String BORDER_COLOR = "#e9ecef";
    private static final String ACCENT_COLOR = "#007bff";
    private static final String TEXT_COLOR = "#495057";
    private static final String VALUE_COLOR = "#28a745";

    public CpuViewer(CPU cpu) {
        this.cpu = cpu;
    }

    public void start(Stage stage) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");
        root.getChildren().addAll(
                createTitle(),
                createRegistersSection(),
                createControlRegistersSection(),
                createFlagsSection()
        );

        refresh();

        stage.setTitle("CPU Viewer - Debug Monitor");
        stage.setScene(new Scene(root, 480, 650));
        stage.show();
    }

    private Label createTitle() {
        Label title = new Label("CPU State Monitor");
        title.setFont(TITLE_FONT);
        title.setTextFill(Color.web(TEXT_COLOR));
        title.setAlignment(Pos.CENTER);
        return title;
    }

    private VBox createRegistersSection() {
        VBox section = new VBox(10);
        Label header = new Label("📊 General Purpose Registers");
        header.setFont(SECTION_FONT);
        header.setTextFill(Color.web(ACCENT_COLOR));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        grid.setStyle(cardStyle());

        for (int i = 0; i < 16; i++) {
            Label name = createStyledLabel(String.format("R%02d", i));
            Label value = createValueLabel();
            registerLabels[i] = value;

            int row = i / 4;
            int col = (i % 4) * 2;

            grid.add(name, col, row);
            grid.add(value, col + 1, row);
        }

        section.getChildren().addAll(header, grid);
        return section;
    }

    private VBox createControlRegistersSection() {
        VBox section = new VBox(10);
        Label header = new Label("🎛 Control Registers");
        header.setFont(SECTION_FONT);
        header.setTextFill(Color.web(ACCENT_COLOR));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));
        grid.setStyle(cardStyle());

        addControlRow(grid, "Program Counter (PC):", programCounterLabel, 0);
        addControlRow(grid, "Stack Pointer (SP):", stackPointerLabel, 1);
        addControlRow(grid, "Heap Pointer (HP):", heapPointerLabel, 2);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(150);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        section.getChildren().addAll(header, grid);
        return section;
    }

    private VBox createFlagsSection() {
        VBox section = new VBox(10);
        Label header = new Label("🚩 Status Flags");
        header.setFont(SECTION_FONT);
        header.setTextFill(Color.web(ACCENT_COLOR));

        HBox container = new HBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle(cardStyle());

        Label label = createStyledLabel("Flags:");

        flagsLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        flagsLabel.setTextFill(Color.web(VALUE_COLOR));
        flagsLabel.setStyle(flagDefaultStyle());

        Label legend = new Label("(Z=Zero, N=Negative, C=Carry, O=Overflow)");
        legend.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        legend.setTextFill(Color.web("#6c757d"));

        container.getChildren().addAll(label, flagsLabel);

        section.getChildren().addAll(header, container, legend);
        return section;
    }

    private void addControlRow(GridPane grid, String name, Label valueLabel, int row) {
        Label nameLabel = createStyledLabel(name);
        valueLabel.setFont(VALUE_FONT);
        valueLabel.setTextFill(Color.web(VALUE_COLOR));
        valueLabel.setStyle(valueLabelStyle());

        grid.add(nameLabel, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setFont(LABEL_FONT);
        label.setTextFill(Color.web(TEXT_COLOR));
        return label;
    }

    private Label createValueLabel() {
        Label label = new Label();
        label.setFont(VALUE_FONT);
        label.setTextFill(Color.web(VALUE_COLOR));
        label.setStyle(valueLabelStyle());
        return label;
    }

    private String cardStyle() {
        return "-fx-background-color: " + CARD_COLOR + "; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px;";
    }

    private String valueLabelStyle() {
        return "-fx-background-color: #f8f9fa; " +
                "-fx-padding: 6px 12px; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-min-width: 100px;";
    }

    private String flagDefaultStyle() {
        return "-fx-background-color: #e8f5e8; " +
                "-fx-padding: 6px 12px; " +
                "-fx-border-color: #28a745; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-background-radius: 6px;";
    }

    private String flagActiveStyle() {
        return "-fx-background-color: #fff3cd; " +
                "-fx-padding: 6px 12px; " +
                "-fx-border-color: #ffc107; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-background-radius: 6px;";
    }

    public void refresh() {
        Platform.runLater(() -> {
            for (int i = 0; i < 16; i++) {
                registerLabels[i].setText(String.format("0x%08X", cpu.getRegister(i)));
            }

            programCounterLabel.setText(String.format("0x%08X", cpu.getProgramCounter()));
            stackPointerLabel.setText(String.format("0x%08X", cpu.getStackPointer()));
            heapPointerLabel.setText(String.format("0x%08X", cpu.getHeapPointer()));

            var flags = cpu.getFlags();
            StringBuilder str = new StringBuilder();
            str.append(flags.isZero() ? "Z" : "-");
            str.append(flags.isNegative() ? "N" : "-");
            str.append(flags.isCarry() ? "C" : "-");
            str.append(flags.isOverflow() ? "O" : "-");
            flagsLabel.setText(str.toString());

            boolean any = flags.isZero() || flags.isNegative() || flags.isCarry() || flags.isOverflow();
            if (any) {
                flagsLabel.setStyle(flagActiveStyle());
                flagsLabel.setTextFill(Color.web("#856404"));
            } else {
                flagsLabel.setStyle(flagDefaultStyle());
                flagsLabel.setTextFill(Color.web(VALUE_COLOR));
            }
        });
    }
}
