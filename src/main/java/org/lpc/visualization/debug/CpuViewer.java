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
        VBox mainContainer = new VBox(15);
        mainContainer.setPadding(new Insets(20));
        mainContainer.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        // Title
        Label title = new Label("CPU State Monitor");
        title.setFont(TITLE_FONT);
        title.setTextFill(Color.web(TEXT_COLOR));
        title.setAlignment(Pos.CENTER);
        mainContainer.getChildren().add(title);

        // Registers section
        VBox registersSection = createRegistersSection();

        // Control registers section
        VBox controlSection = createControlRegistersSection();

        // Flags section
        VBox flagsSection = createFlagsSection();

        mainContainer.getChildren().addAll(registersSection, controlSection, flagsSection);

        refresh();

        Scene scene = new Scene(mainContainer, 480, 650);
        stage.setTitle("CPU Viewer - Debug Monitor");
        stage.setScene(scene);
        stage.show();
    }

    private VBox createRegistersSection() {
        VBox section = new VBox(10);

        Label sectionTitle = new Label("📊 General Purpose Registers");
        sectionTitle.setFont(SECTION_FONT);
        sectionTitle.setTextFill(Color.web(ACCENT_COLOR));

        GridPane registersGrid = new GridPane();
        registersGrid.setHgap(15);
        registersGrid.setVgap(8);
        registersGrid.setPadding(new Insets(15));
        registersGrid.setStyle("-fx-background-color: " + CARD_COLOR + "; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px;");

        // Create 4 columns for better organization
        for (int i = 0; i < 16; i++) {
            Label regName = new Label(String.format("R%02d", i));
            regName.setFont(LABEL_FONT);
            regName.setTextFill(Color.web(TEXT_COLOR));
            regName.setMinWidth(35);

            Label regValue = new Label();
            regValue.setFont(VALUE_FONT);
            regValue.setTextFill(Color.web(VALUE_COLOR));
            regValue.setStyle("-fx-background-color: #f8f9fa; " +
                    "-fx-padding: 4px 8px; " +
                    "-fx-border-radius: 4px; " +
                    "-fx-background-radius: 4px;");
            registerLabels[i] = regValue;

            int row = i / 4;
            int col = (i % 4) * 2;

            registersGrid.add(regName, col, row);
            registersGrid.add(regValue, col + 1, row);
        }

        section.getChildren().addAll(sectionTitle, registersGrid);
        return section;
    }

    private VBox createControlRegistersSection() {
        VBox section = new VBox(10);

        Label sectionTitle = new Label("🎛 Control Registers");
        sectionTitle.setFont(SECTION_FONT);
        sectionTitle.setTextFill(Color.web(ACCENT_COLOR));

        GridPane controlGrid = new GridPane();
        controlGrid.setHgap(15);
        controlGrid.setVgap(12);
        controlGrid.setPadding(new Insets(15));
        controlGrid.setStyle("-fx-background-color: " + CARD_COLOR + "; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px;");

        // Program Counter
        Label pcLabel = createStyledLabel("Program Counter (PC):");
        programCounterLabel.setFont(VALUE_FONT);
        programCounterLabel.setTextFill(Color.web(VALUE_COLOR));
        styleValueLabel(programCounterLabel);

        // Stack Pointer
        Label spLabel = createStyledLabel("Stack Pointer (SP):");
        stackPointerLabel.setFont(VALUE_FONT);
        stackPointerLabel.setTextFill(Color.web(VALUE_COLOR));
        styleValueLabel(stackPointerLabel);

        // Heap Pointer
        Label hpLabel = createStyledLabel("Heap Pointer (HP):");
        heapPointerLabel.setFont(VALUE_FONT);
        heapPointerLabel.setTextFill(Color.web(VALUE_COLOR));
        styleValueLabel(heapPointerLabel);

        controlGrid.add(pcLabel, 0, 0);
        controlGrid.add(programCounterLabel, 1, 0);
        controlGrid.add(spLabel, 0, 1);
        controlGrid.add(stackPointerLabel, 1, 1);
        controlGrid.add(hpLabel, 0, 2);
        controlGrid.add(heapPointerLabel, 1, 2);

        // Make labels expand
        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(150);
        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);
        controlGrid.getColumnConstraints().addAll(col1, col2);

        section.getChildren().addAll(sectionTitle, controlGrid);
        return section;
    }

    private VBox createFlagsSection() {
        VBox section = new VBox(10);

        Label sectionTitle = new Label("🚩 Status Flags");
        sectionTitle.setFont(SECTION_FONT);
        sectionTitle.setTextFill(Color.web(ACCENT_COLOR));

        HBox flagsContainer = new HBox(10);
        flagsContainer.setPadding(new Insets(15));
        flagsContainer.setAlignment(Pos.CENTER_LEFT);
        flagsContainer.setStyle("-fx-background-color: " + CARD_COLOR + "; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px;");

        Label flagsTitle = new Label("Flags:");
        flagsTitle.setFont(LABEL_FONT);
        flagsTitle.setTextFill(Color.web(TEXT_COLOR));

        flagsLabel.setFont(Font.font("Consolas", FontWeight.BOLD, 14));
        flagsLabel.setTextFill(Color.web(VALUE_COLOR));
        flagsLabel.setStyle("-fx-background-color: #e8f5e8; " +
                "-fx-padding: 6px 12px; " +
                "-fx-border-color: #28a745; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 6px; " +
                "-fx-background-radius: 6px;");

        // Add flag legend
        Label legend = new Label("(Z=Zero, N=Negative, C=Carry, O=Overflow)");
        legend.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 10));
        legend.setTextFill(Color.web("#6c757d"));

        flagsContainer.getChildren().addAll(flagsTitle, flagsLabel);

        VBox flagsWithLegend = new VBox(5);
        flagsWithLegend.getChildren().addAll(flagsContainer, legend);

        section.getChildren().addAll(sectionTitle, flagsWithLegend);
        return section;
    }

    private Label createStyledLabel(String text) {
        Label label = new Label(text);
        label.setFont(LABEL_FONT);
        label.setTextFill(Color.web(TEXT_COLOR));
        return label;
    }

    private void styleValueLabel(Label label) {
        label.setStyle("-fx-background-color: #f8f9fa; " +
                "-fx-padding: 6px 12px; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-min-width: 100px;");
    }

    public void refresh() {
        Platform.runLater(() -> {
            // Update registers
            for (int i = 0; i < 16; i++) {
                registerLabels[i].setText(String.format("0x%08X", cpu.getRegister(i)));
            }

            // Update control registers
            programCounterLabel.setText(String.format("0x%08X", cpu.getProgramCounter()));
            stackPointerLabel.setText(String.format("0x%08X", cpu.getStackPointer()));
            heapPointerLabel.setText(String.format("0x%08X", cpu.getHeapPointer()));

            // Update flags with color coding
            var flags = cpu.getFlags();
            StringBuilder f = new StringBuilder();
            f.append(flags.isZero() ? "Z" : "-");
            f.append(flags.isNegative() ? "N" : "-");
            f.append(flags.isCarry() ? "C" : "-");
            f.append(flags.isOverflow() ? "O" : "-");
            flagsLabel.setText(f.toString());

            // Change flag colors based on state
            boolean anyFlagSet = flags.isZero() || flags.isNegative() ||
                    flags.isCarry() || flags.isOverflow();
            if (anyFlagSet) {
                flagsLabel.setStyle("-fx-background-color: #fff3cd; " +
                        "-fx-padding: 6px 12px; " +
                        "-fx-border-color: #ffc107; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 6px; " +
                        "-fx-background-radius: 6px;");
                flagsLabel.setTextFill(Color.web("#856404"));
            } else {
                flagsLabel.setStyle("-fx-background-color: #e8f5e8; " +
                        "-fx-padding: 6px 12px; " +
                        "-fx-border-color: #28a745; " +
                        "-fx-border-width: 1px; " +
                        "-fx-border-radius: 6px; " +
                        "-fx-background-radius: 6px;");
                flagsLabel.setTextFill(Color.web(VALUE_COLOR));
            }
        });
    }
}