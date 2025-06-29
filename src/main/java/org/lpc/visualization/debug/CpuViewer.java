package org.lpc.visualization.debug;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.lpc.CPU;
import org.lpc.util.Colors;
import org.lpc.util.Fonts;
import org.lpc.util.Styles;

public class CpuViewer {
    private final CPU cpu;
    private final Label[] registerLabels = new Label[16];
    private final Label programCounterLabel = Styles.valueLabel();
    private final Label stackPointerLabel = Styles.valueLabel();
    private final Label heapPointerLabel = Styles.valueLabel();
    private final Label flagsLabel = Styles.flagLabel();
    private long lastUpdate = 0;

    public CpuViewer(CPU cpu) {
        this.cpu = cpu;
    }

    public void start(Stage stage) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: " + Colors.BACKGROUND + ";");
        root.getChildren().addAll(
                Styles.title("CPU State Monitor"),
                createRegistersSection(),
                createControlRegistersSection(),
                createFlagsSection()
        );

        startAutoRefresh();

        stage.setTitle("CPU Viewer - Debug Monitor");
        stage.setScene(new Scene(root, 700, 650));
        stage.show();
    }

    private void startAutoRefresh() {
        AnimationTimer refreshTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 20_000_000) {
                    refresh();
                    lastUpdate = now;
                }
            }
        };
        refreshTimer.start();
    }

    private VBox createRegistersSection() {
        VBox section = new VBox(10);
        section.getChildren().addAll(Styles.sectionHeader("📊 General Purpose Registers"));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setPadding(new Insets(15));
        grid.setStyle(Styles.cardStyle());

        for (int i = 0; i < 16; i++) {
            Label name = Styles.monoLabel(String.format("R%02d", i));
            Label value = Styles.valueLabel();
            registerLabels[i] = value;

            int row = i / 4;
            int col = (i % 4) * 2;

            grid.add(name, col, row);
            grid.add(value, col + 1, row);
        }

        section.getChildren().add(grid);
        return section;
    }

    private VBox createControlRegistersSection() {
        VBox section = new VBox(10);
        section.getChildren().addAll(Styles.sectionHeader("🎛 Control Registers"));

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(15));
        grid.setStyle(Styles.cardStyle());

        addControlRow(grid, "Program Counter (PC):", programCounterLabel, 0);
        addControlRow(grid, "Stack Pointer (SP):", stackPointerLabel, 1);
        addControlRow(grid, "Heap Pointer (HP):", heapPointerLabel, 2);

        ColumnConstraints c1 = new ColumnConstraints();
        c1.setMinWidth(150);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(c1, c2);

        section.getChildren().add(grid);
        return section;
    }

    private VBox createFlagsSection() {
        VBox section = new VBox(10);
        section.getChildren().addAll(Styles.sectionHeader("🚩 Status Flags"));

        HBox container = new HBox(10);
        container.setPadding(new Insets(15));
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle(Styles.cardStyle());

        Label label = Styles.monoLabel("Flags:");
        container.getChildren().addAll(label, flagsLabel);

        Label legend = new Label("(Z=Zero, N=Negative, C=Carry, O=Overflow)");
        legend.setFont(Fonts.SECTION);
        legend.setTextFill(Color.web(Colors.MUTED));

        section.getChildren().addAll(container, legend);
        return section;
    }

    private void addControlRow(GridPane grid, String name, Label valueLabel, int row) {
        Label nameLabel = Styles.monoLabel(name);
        grid.add(nameLabel, 0, row);
        grid.add(valueLabel, 1, row);
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
                flagsLabel.setStyle(Styles.highlightRow());
                flagsLabel.setTextFill(Color.web("#856404"));
            } else {
                flagsLabel.setStyle(Styles.faintHighlightRow());
                flagsLabel.setTextFill(Color.web(Colors.VALUE));
            }
        });
    }
}
