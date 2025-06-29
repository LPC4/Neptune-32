package org.lpc.visualization.debug;

import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import org.lpc.CPU;
import org.lpc.instructions.InstructionSet;
import org.lpc.memory.MemoryBus;
import org.lpc.memory.MemoryMap;
import org.lpc.util.Colors;
import org.lpc.util.Fonts;
import org.lpc.util.Styles;

import java.util.LinkedHashMap;
import java.util.Map;

public class MemoryViewer {

    private static final int ROW_SIZE = 4;
    private static final int ROWS_TO_SHOW = 16;

    private final CPU cpu;
    private final InstructionSet instructionSet;
    private final MemoryBus memory;
    private final MemoryMap memoryMap;

    private final Map<String, Integer> memorySections = new LinkedHashMap<>();

    private int currentAddress;
    private ViewMode viewMode = ViewMode.HEX;

    private TableSection tableSection;
    private ControlPanel controlPanel;
    private StatusBar statusBar;

    private long lastUpdate = 0;

    public MemoryViewer(final CPU cpu) {
        this.cpu = cpu;
        this.instructionSet = cpu.getInstructionSet();
        this.memory = cpu.getMemory();
        this.memoryMap = cpu.getMemoryMap();

        initMemorySections();
        currentAddress = memoryMap.getProgramStart();
    }

    private void initMemorySections() {
        memorySections.put("🔧 Boot ROM", memoryMap.getBootRomStart());
        memorySections.put("💾 RAM", memoryMap.getRamStart());
        memorySections.put("🖥️ VRAM", memoryMap.getVramStart());
        memorySections.put("🔌 I/O", memoryMap.getIoStart());
    }

    public void start(final Stage stage) {
        tableSection = new TableSection();
        controlPanel = new ControlPanel();
        statusBar = new StatusBar();

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #f8f9fa;");

        Label title = new Label("Memory Viewer");
        title.setFont(Fonts.TITLE);
        title.setTextFill(Color.web(Colors.TEXT));
        title.setAlignment(Pos.CENTER);
        BorderPane.setMargin(title, new Insets(15, 20, 10, 20));

        root.setTop(new VBox(5, title, controlPanel.get()));
        root.setCenter(tableSection.get());
        root.setBottom(statusBar.get());

        BorderPane.setMargin(controlPanel.get(), new Insets(0, 20, 10, 20));
        BorderPane.setMargin(tableSection.get(), new Insets(0, 20, 10, 20));
        BorderPane.setMargin(statusBar.get(), new Insets(0, 20, 15, 20));

        refresh();

        Scene scene = new Scene(root, 800, 700);
        stage.setScene(scene);
        stage.setTitle("Memory Viewer - Debug Monitor");
        stage.setResizable(false);
        stage.show();

        startAutoRefresh();
    }

    private void refresh() {
        tableSection.refresh();
        statusBar.refresh();
    }

    private void startAutoRefresh() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 25_000_000) {
                    refresh();
                    lastUpdate = now;
                }
            }
        };
        timer.start();
    }

    private int clampAddress(final int addr) {
        int maxStart = Math.max(0, memoryMap.getTotalMemorySize() - ROW_SIZE * ROWS_TO_SHOW);
        if (addr < 0) return 0;
        if (addr > maxStart) return maxStart;
        return addr & ~(ROW_SIZE - 1);
    }

    private enum ViewMode { HEX, BITS, NUM }

    private static class MemoryRow {
        private final SimpleStringProperty address;
        private final SimpleStringProperty bytes;
        private final SimpleStringProperty instruction;

        public MemoryRow(String address, String bytes, String instruction) {
            this.address = new SimpleStringProperty(address);
            this.bytes = new SimpleStringProperty(bytes);
            this.instruction = new SimpleStringProperty(instruction);
        }

        public String getAddress() {
            return address.get();
        }

        public String getBytes() {
            return bytes.get();
        }

        public String getInstruction() {
            return instruction.get();
        }
    }

    private class TableSection {
        private final TableView<MemoryRow> table;
        private final VBox container;

        public TableSection() {
            table = new TableView<>();
            container = new VBox(8);
            container.setFillWidth(true);

            Label label = new Label("📋 Memory Contents");
            label.setFont(Fonts.SECTION);
            label.setTextFill(Color.web(Colors.ACCENT));

            table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
            table.setStyle(Styles.cardStyle());

            TableColumn<MemoryRow, String> addrCol = new TableColumn<>("📍 Address");
            addrCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAddress()));
            addrCol.setPrefWidth(120);
            addrCol.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

            TableColumn<MemoryRow, String> bytesCol = new TableColumn<>("📊 Bytes");
            bytesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBytes()));
            bytesCol.setMinWidth(150);
            bytesCol.setStyle("-fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

            TableColumn<MemoryRow, String> instrCol = new TableColumn<>("⚙️ Instruction");
            instrCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInstruction()));
            instrCol.setMinWidth(200);
            instrCol.setStyle("-fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

            table.getColumns().addAll(addrCol, bytesCol, instrCol);

            table.setRowFactory(tv -> {
                TableRow<MemoryRow> row = new TableRow<>();
                row.itemProperty().addListener((obs, oldItem, newItem) -> {
                    if (newItem != null) {
                        int pc = cpu.getProgramCounter();
                        int memAddr = Integer.parseInt(newItem.getAddress().replace("0x", ""), 16);

                        if (memAddr == pc) {
                            row.setStyle(Styles.highlightRow());
                        } else if (memAddr >= pc && memAddr < pc + 16) {
                            row.setStyle(Styles.faintHighlightRow());
                        } else {
                            row.setStyle(Styles.monoRow());
                        }
                    }
                });
                return row;
            });

            container.getChildren().addAll(label, table);
            VBox.setVgrow(table, Priority.ALWAYS);
        }

        public Node get() {
            return container;
        }

        public void refresh() {
            table.setItems(new MemoryRenderer().render());
        }
    }

    private class StatusBar {
        private final Label addressLabel = new Label();
        private final Label memoryLabel = new Label();
        private final Label pcLabel = new Label();
        private final HBox bar;

        public StatusBar() {
            addressLabel.setFont(Fonts.MONO);
            addressLabel.setTextFill(Color.web(Colors.SUCCESS));

            memoryLabel.setFont(Fonts.LABEL);
            memoryLabel.setTextFill(Color.web(Colors.MUTED));

            pcLabel.setFont(Fonts.MONO);
            pcLabel.setTextFill(Color.web(Colors.ACCENT));

            bar = new HBox(15,
                    new Label("📍 Current View:"), addressLabel,
                    createSeparator(),
                    new Label("💾 Memory:"), memoryLabel,
                    createSeparator(),
                    pcLabel
            );
            bar.setAlignment(Pos.CENTER_LEFT);
            bar.setPadding(new Insets(10));
            bar.setStyle(Styles.cardStyle());
        }

        public Node get() {
            return bar;
        }

        public void refresh() {
            addressLabel.setText(String.format("0x%08X - 0x%08X",
                    currentAddress, currentAddress + (ROWS_TO_SHOW * ROW_SIZE) - 1));

            memoryLabel.setText(String.format("Total: %d bytes | Showing: %d rows",
                    memoryMap.getTotalMemorySize(),
                    ROWS_TO_SHOW));

            pcLabel.setText("PC: 0x" + String.format("%08X", cpu.getProgramCounter()));
        }

        private Node createSeparator() {
            Separator sep = new Separator(Orientation.VERTICAL);
            sep.setPrefHeight(20);
            return sep;
        }
    }

    private class ControlPanel {
        private final VBox panel;
        private final ComboBox<String> sectionSelector;
        private final TextField addressInput;

        public ControlPanel() {
            panel = new VBox(8);
            Label label = new Label("Navigation Controls");
            label.setFont(Fonts.SECTION);
            label.setTextFill(Color.web(Colors.ACCENT));

            sectionSelector = new ComboBox<>(FXCollections.observableArrayList(memorySections.keySet()));
            sectionSelector.setPrefWidth(120);
            sectionSelector.getSelectionModel().select(1);
            Styles.styleCombo(sectionSelector);
            sectionSelector.setOnAction(e -> {
                currentAddress = clampAddress(memorySections.get(sectionSelector.getValue()));
                refresh();
            });

            ToggleButton viewToggle = Styles.toggleButton("Hex");
            viewToggle.setOnAction(e -> {
                switch (viewMode) {
                    case HEX -> { viewMode = ViewMode.BITS; viewToggle.setText("Bits"); }
                    case BITS -> { viewMode = ViewMode.NUM; viewToggle.setText("Num"); }
                    case NUM -> { viewMode = ViewMode.HEX; viewToggle.setText("Hex"); }
                }
                refresh();
            });

            Button prev = Styles.button("⬅ Prev");
            Button next = Styles.button("Next ➡");

            prev.setOnAction(e -> {
                currentAddress = clampAddress(currentAddress - ROW_SIZE * ROWS_TO_SHOW);
                refresh();
            });
            next.setOnAction(e -> {
                currentAddress = clampAddress(currentAddress + ROW_SIZE * ROWS_TO_SHOW);
                refresh();
            });

            addressInput = new TextField();
            addressInput.setPromptText("0x00000000");
            addressInput.setPrefWidth(100);
            Styles.styleTextField(addressInput);

            Button go = Styles.button("🎯 Go");
            go.setOnAction(e -> {
                try {
                    int addr = Integer.parseInt(addressInput.getText().trim().replace("0x", ""), 16);
                    currentAddress = clampAddress(addr);
                    refresh();
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.WARNING, "Invalid address");
                    alert.showAndWait();
                }
            });

            HBox line = new HBox(12,
                    new Label("Section:"), sectionSelector,
                    createSeparator(),
                    viewToggle,
                    createSeparator(),
                    prev, next,
                    createSeparator(),
                    new Label("Address:"), addressInput, go
            );
            line.setAlignment(Pos.CENTER_LEFT);
            line.setPadding(new Insets(10));
            line.setStyle(Styles.cardStyle());

            panel.getChildren().addAll(label, line);
        }

        public Node get() {
            return panel;
        }

        private Node createSeparator() {
            Separator sep = new Separator(Orientation.VERTICAL);
            sep.setPrefHeight(20);
            return sep;
        }
    }

    private class MemoryRenderer {
        public ObservableList<MemoryRow> render() {
            ObservableList<MemoryRow> rows = FXCollections.observableArrayList();

            for (int row = 0; row < ROWS_TO_SHOW; row++) {
                final int addr = currentAddress + row * ROW_SIZE;
                if (addr + ROW_SIZE > memoryMap.getTotalMemorySize()) break;

                StringBuilder bytesRep = new StringBuilder();
                for (int i = 0; i < ROW_SIZE; i++) {
                    try {
                        int b = memory.readByte(addr + i) & 0xFF;
                        bytesRep.append(switch (viewMode) {
                            case HEX -> String.format("%02X", b);
                            case BITS -> String.format("%8s", Integer.toBinaryString(b)).replace(' ', '0');
                            case NUM -> String.format("%3d", b);
                        });
                    } catch (IllegalArgumentException ex) {
                        bytesRep.append(switch (viewMode) {
                            case HEX -> "--";
                            case BITS -> "--------";
                            case NUM -> "---";
                        });
                    }
                    if (i < ROW_SIZE - 1) bytesRep.append(" ");
                }

                byte opcode = (byte) (memory.readWord(addr) & 0xFF);
                String instrName = instructionSet.getName(opcode);
                if (instrName == null) instrName = "UNKNOWN";

                rows.add(new MemoryRow(
                        String.format("0x%08X", addr),
                        bytesRep.toString(),
                        instrName + " (0x" + String.format("%02X", opcode) + ")"
                ));
            }
            return rows;
        }
    }
}
