package org.lpc.visualization.debug;

import javafx.animation.AnimationTimer;
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

import java.util.LinkedHashMap;
import java.util.Map;

public class MemoryViewer {
    private static final int ROW_SIZE = 4;
    private static final int ROWS_TO_SHOW = 16;

    private int currentAddress;

    private final CPU cpu;
    private final InstructionSet instructionSet;
    private final MemoryBus memory;
    private final MemoryMap memoryMap;

    private TableView<MemoryRow> tableView;
    private TextField addressInput;
    private ComboBox<String> sectionSelector;
    private Label currentAddressLabel;
    private Label memoryInfoLabel;

    private final Map<String, Integer> memorySections = new LinkedHashMap<>();

    private long lastUpdate = 0;

    private static final Font TITLE_FONT = Font.font("Segoe UI", FontWeight.BOLD, 20);
    private static final Font SECTION_FONT = Font.font("Segoe UI", FontWeight.SEMI_BOLD, 14);
    private static final Font LABEL_FONT = Font.font("Segoe UI", FontWeight.NORMAL, 12);
    private static final Font MONO_FONT = Font.font("Consolas", FontWeight.NORMAL, 11);

    private static final String BACKGROUND_COLOR = "#f8f9fa";
    private static final String CARD_COLOR = "#ffffff";
    private static final String BORDER_COLOR = "#e9ecef";
    private static final String ACCENT_COLOR = "#007bff";
    private static final String SUCCESS_COLOR = "#28a745";
    private static final String TEXT_COLOR = "#495057";
    private static final String MUTED_COLOR = "#6c757d";

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
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        Label title = new Label("Memory Viewer");
        title.setFont(TITLE_FONT);
        title.setTextFill(Color.web(TEXT_COLOR));
        title.setAlignment(Pos.CENTER);
        BorderPane.setMargin(title, new Insets(15, 20, 10, 20));

        VBox controlPanel = createControlPanel();
        controlPanel.setMaxHeight(120);
        controlPanel.setMinHeight(120);

        VBox tableSection = createTableSection();

        HBox statusBar = createStatusBar();
        statusBar.setMaxHeight(50);
        statusBar.setMinHeight(50);

        root.setTop(new VBox(5, title, controlPanel));
        root.setCenter(tableSection);
        root.setBottom(statusBar);

        BorderPane.setMargin(controlPanel, new Insets(0, 20, 10, 20));
        BorderPane.setMargin(tableSection, new Insets(0, 20, 10, 20));
        BorderPane.setMargin(statusBar, new Insets(0, 20, 15, 20));

        refreshTable();

        Scene scene = new Scene(root, 1000, 700);
        stage.setScene(scene);
        stage.setTitle("Memory Viewer - Debug Monitor");
        stage.setResizable(false);
        stage.show();

        startAutoRefresh();
    }

    private void startAutoRefresh() {
        AnimationTimer refreshTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 20_000_000) {
                    refreshTable();
                    lastUpdate = now;
                }
            }
        };
        refreshTimer.start();
    }

    private VBox createTableSection() {
        VBox tableSection = new VBox(8);

        Label sectionTitle = new Label("📋 Memory Contents");
        sectionTitle.setFont(SECTION_FONT);
        sectionTitle.setTextFill(Color.web(ACCENT_COLOR));

        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        tableView.setStyle("-fx-background-color: " + CARD_COLOR + "; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px;");

        TableColumn<MemoryRow, String> addrCol = new TableColumn<>("📍 Address");
        addrCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAddress()));
        addrCol.setMaxWidth(120);
        addrCol.setMinWidth(120);
        addrCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<MemoryRow, String> bytesCol = new TableColumn<>("📊 Hex Bytes");
        bytesCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getBytes()));
        bytesCol.setMinWidth(150);

        TableColumn<MemoryRow, String> instrCol = new TableColumn<>("⚙️ Instruction");
        instrCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getInstruction()));
        instrCol.setMinWidth(200);

        addrCol.setStyle("-fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        bytesCol.setStyle("-fx-font-weight: bold; -fx-font-family: 'Segoe UI';");
        instrCol.setStyle("-fx-font-weight: bold; -fx-font-family: 'Segoe UI';");

        //noinspection unchecked
        tableView.getColumns().addAll(addrCol, bytesCol, instrCol);

        tableView.setRowFactory(tv -> {
            TableRow<MemoryRow> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null) {
                    row.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 11px;");

                    String addr = newItem.getAddress().replace("0x", "");
                    int memAddr = Integer.parseInt(addr, 16);
                    int pc = cpu.getProgramCounter();

                    if (memAddr == pc) { // set yellow
                        row.setStyle("-fx-background-color: #fff3cd; -fx-font-family: 'Consolas'; " +
                                "-fx-font-size: 11px; -fx-font-weight: bold;");
                    } else if (memAddr >= pc && memAddr < pc + 16) {
                        row.setStyle("-fx-background-color: #f8f9fa; -fx-font-family: 'Consolas'; " +
                                "-fx-font-size: 11px;");
                    }
                }
            });
            return row;
        });

        tableSection.getChildren().addAll(sectionTitle, tableView);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        return tableSection;
    }

    private HBox createStatusBar() {
        HBox statusBar = new HBox(15);
        statusBar.setPadding(new Insets(10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.setStyle("-fx-background-color: " + CARD_COLOR + "; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px;");

        currentAddressLabel = new Label();
        currentAddressLabel.setFont(MONO_FONT);
        currentAddressLabel.setTextFill(Color.web(SUCCESS_COLOR));

        memoryInfoLabel = new Label();
        memoryInfoLabel.setFont(LABEL_FONT);
        memoryInfoLabel.setTextFill(Color.web(MUTED_COLOR));

        Label pcLabel = new Label();
        pcLabel.setFont(MONO_FONT);
        pcLabel.setTextFill(Color.web(ACCENT_COLOR));
        pcLabel.setText("PC: 0x" + String.format("%08X", cpu.getProgramCounter()));

        statusBar.getChildren().addAll(
                new Label("📍 Current View:"), currentAddressLabel,
                createSeparator(),
                new Label("💾 Memory:"), memoryInfoLabel,
                createSeparator(),
                pcLabel
        );

        return statusBar;
    }

    private Separator createSeparator() {
        Separator sep = new Separator();
        sep.setOrientation(Orientation.VERTICAL);
        sep.setPrefHeight(20);
        return sep;
    }

    private int clampAddress(final int addr) {
        int maxStart = Math.max(0, memoryMap.getTotalMemorySize() - ROW_SIZE * ROWS_TO_SHOW);
        if (addr < 0) return 0;
        if (addr > maxStart) return maxStart;
        return addr & ~(ROW_SIZE - 1);
    }

    private void refreshTable() {
        ObservableList<MemoryRow> rows = FXCollections.observableArrayList();

        for (int row = 0; row < ROWS_TO_SHOW; row++) {
            final int addr = currentAddress + row * ROW_SIZE;
            if (addr + ROW_SIZE > memoryMap.getTotalMemorySize()) break;

            StringBuilder bytesHex = new StringBuilder();
            for (int i = 0; i < ROW_SIZE; i++) {
                try {
                    int b = memory.readByte(addr + i) & 0xFF;
                    bytesHex.append(String.format("%02X ", b));
                } catch (IllegalArgumentException ex) {
                    bytesHex.append("-- ");
                }
            }

            int word = 0;
            try {
                word = memory.readWord(addr);
            } catch (IllegalArgumentException e) {
                System.out.println("invalid read");
            }
            byte opcode = (byte) (word & 0xFF);
            String instrName = instructionSet.getName(opcode);
            if (instrName == null) instrName = "UNKNOWN";

            String instructionText = instrName + " (0x" + String.format("%02X", opcode & 0xFF) + ")";

            rows.add(new MemoryRow(String.format("0x%08X", addr), bytesHex.toString().trim(), instructionText));
        }

        tableView.setItems(rows);

        currentAddressLabel.setText(String.format("0x%08X - 0x%08X",
                currentAddress,
                currentAddress + (ROWS_TO_SHOW * ROW_SIZE) - 1));

        memoryInfoLabel.setText(String.format("Total: %d bytes | Showing: %d rows",
                memoryMap.getTotalMemorySize(),
                Math.min(ROWS_TO_SHOW, rows.size())));
    }

    private VBox createControlPanel() {
        VBox controlPanel = new VBox(8);

        Label sectionTitle = new Label("Navigation Controls");
        sectionTitle.setFont(SECTION_FONT);
        sectionTitle.setTextFill(Color.web(ACCENT_COLOR));

        // Compact controls container
        HBox controlsContainer = new HBox(12);
        controlsContainer.setPadding(new Insets(10));
        controlsContainer.setAlignment(Pos.CENTER_LEFT);
        controlsContainer.setStyle("-fx-background-color: " + CARD_COLOR + "; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 8px; " +
                "-fx-background-radius: 8px;");

        // Compact section selector
        Label sectionLabel = new Label("Section:");
        sectionLabel.setFont(LABEL_FONT);
        sectionLabel.setTextFill(Color.web(TEXT_COLOR));

        sectionSelector = new ComboBox<>(FXCollections.observableArrayList(memorySections.keySet()));
        sectionSelector.getSelectionModel().selectFirst();
        sectionSelector.setPrefWidth(120);
        styleComboBox(sectionSelector);
        sectionSelector.setOnAction(e -> {
            final String section = sectionSelector.getSelectionModel().getSelectedItem();
            if (section != null && memorySections.containsKey(section)) {
                currentAddress = clampAddress(memorySections.get(section));
                refreshTable();
            }
        });
        sectionSelector.getSelectionModel().select(1); // start in ram section

        // Compact navigation buttons
        Button prevButton = createCompactButton("⬅ Prev");
        Button nextButton = createCompactButton("Next ➡");

        prevButton.setOnAction(e -> {
            currentAddress = clampAddress(currentAddress - ROW_SIZE * ROWS_TO_SHOW);
            refreshTable();
        });

        nextButton.setOnAction(e -> {
            currentAddress = clampAddress(currentAddress + ROW_SIZE * ROWS_TO_SHOW);
            refreshTable();
        });

        // Compact address input
        Label addressLabel = new Label("Address:");
        addressLabel.setFont(LABEL_FONT);
        addressLabel.setTextFill(Color.web(TEXT_COLOR));

        this.addressInput = new TextField();
        this.addressInput.setPromptText("0x00000000");
        this.addressInput.setPrefWidth(100);
        styleTextField(this.addressInput);

        Button goButton = createCompactButton("🎯 Go");
        goButton.setOnAction(e -> {
            final String text = this.addressInput.getText().trim();
            try {
                int addr = Integer.parseInt(text.replace("0x", ""), 16);
                if (addr < 0 || addr >= memoryMap.getTotalMemorySize()) {
                    showStyledAlert("Address out of range", "Please enter an address within memory bounds.");
                    return;
                }
                currentAddress = clampAddress(addr);
                refreshTable();
                this.addressInput.clear();
            } catch (NumberFormatException ex) {
                showStyledAlert("Invalid Address", "Please enter a valid hexadecimal address (e.g., 0x1000).");
            }
        });

        controlsContainer.getChildren().addAll(
                sectionLabel, sectionSelector,
                createCompactSeparator(),
                prevButton, nextButton,
                createCompactSeparator(),
                addressLabel, this.addressInput, goButton
        );

        controlPanel.getChildren().addAll(sectionTitle, controlsContainer);
        return controlPanel;
    }

    private Button createCompactButton(String text) {
        Button button = new Button(text);
        button.setFont(Font.font("Segoe UI", 12));
        button.setStyle("-fx-background-color: " + ACCENT_COLOR + "; " +
                "-fx-text-fill: white; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-padding: 4px 8px; " +
                "-fx-cursor: hand;");

        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: #0069d9; " +
                "-fx-text-fill: white; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-padding: 4px 8px; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 2, 0, 0, 1);"));

        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + ACCENT_COLOR + "; " +
                "-fx-text-fill: white; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-padding: 4px 8px; " +
                "-fx-cursor: hand;"));
        return button;
    }

    private Node createCompactSeparator() {
        Separator sep = new Separator();
        sep.setOrientation(Orientation.VERTICAL);
        sep.setPrefHeight(20);
        sep.setStyle("-fx-padding: 0 4px;");
        return sep;
    }

    private Button createStyledButton(String text, String colorStyle) {
        Button button = new Button(text);
        button.setFont(LABEL_FONT);
        button.setTextFill(Color.WHITE);
        button.setStyle(colorStyle + " " +
                "-fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; " +
                "-fx-padding: 8px 16px; " +
                "-fx-cursor: hand;");

        button.setOnMouseEntered(e -> button.setStyle(colorStyle + " " +
                "-fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; " +
                "-fx-padding: 8px 16px; " +
                "-fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 4, 0, 0, 2);"));

        button.setOnMouseExited(e -> button.setStyle(colorStyle + " " +
                "-fx-border-radius: 6px; " +
                "-fx-background-radius: 6px; " +
                "-fx-padding: 8px 16px; " +
                "-fx-cursor: hand;"));
        return button;
    }

    private void styleComboBox(ComboBox<String> comboBox) {
        comboBox.setStyle("-fx-background-color: white; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px;");
    }

    private void styleTextField(TextField textField) {
        textField.setStyle("-fx-background-color: white; " +
                "-fx-border-color: " + BORDER_COLOR + "; " +
                "-fx-border-width: 1px; " +
                "-fx-border-radius: 4px; " +
                "-fx-background-radius: 4px; " +
                "-fx-padding: 6px;");
        textField.setFont(MONO_FONT);
    }

    private void showStyledAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        // Style the alert
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        alert.showAndWait();
    }

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
}