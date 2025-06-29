package org.lpc;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.lpc.external.Assembler;
import org.lpc.instructions.InstructionSet;
import org.lpc.instructions.NeptuneInstructionSet;
import org.lpc.memory.MemoryMap;
import org.lpc.memory.NeptuneMemoryMap;
import org.lpc.memory.io.ConsoleOutputDevice;
import org.lpc.memory.io.IODeviceManager;
import org.lpc.memory.io.KeyboardInputDevice;
import org.lpc.visualization.debug.CpuViewer;
import org.lpc.visualization.debug.MemoryViewer;
import org.lpc.visualization.vram.RGBA32Viewer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class Main extends Application {
    private CPU cpu;
    private Scene ioScene;

    @Override
    public void start(Stage primaryStage) {
        initCpu();
        loadBootRom();
        loadUserProgram();
        createAndShowViewers();
        addIoDevices();
        startCpuExecutionThread();
    }

    private void addIoDevices() {
        IODeviceManager io = cpu.getMemory().getIo();
        io.register(new KeyboardInputDevice(io.getBaseAddress() + io.getCurrentOffset(), Objects.requireNonNull(ioScene)));
        io.register(new ConsoleOutputDevice(io.getBaseAddress() + io.getCurrentOffset()));
        io.printDevices();
    }

    private void initCpu() {
        MemoryMap memoryMap = new NeptuneMemoryMap();
        InstructionSet instructionSet = new NeptuneInstructionSet();
        cpu = new CPU(instructionSet, memoryMap, 32);
    }

    private void loadBootRom() {
        assembleAndLoadResource("/boot.rom.asm", cpu.getMemoryMap().getSyscallCodeStart());
    }

    private void loadUserProgram() {
        assembleAndLoadResource("/keyboard_input.asm", cpu.getMemoryMap().getRamStart());
    }

    private void assembleAndLoadResource(String resourcePath, int loadAddress) {
        try (var stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) throw new RuntimeException("Resource not found: " + resourcePath);
            List<String> lines = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().toList();
            new Assembler(cpu).assembleAndLoad(lines, loadAddress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }

    private void createAndShowViewers() {
        var cpuViewer = new CpuViewer(cpu);
        var memoryViewer = new MemoryViewer(cpu);
        var vramViewer = new RGBA32Viewer(cpu.getMemory(), cpu.getMemoryMap());

        Stage cpuStage = new Stage();
        Stage memoryStage = new Stage();
        Stage vramStage = new Stage();

        cpuViewer.start(cpuStage);
        memoryViewer.start(memoryStage);
        vramViewer.start(vramStage);

        ioScene = vramStage.getScene();

        arrangeStages(memoryStage, cpuStage);
    }

    private void arrangeStages(Stage memoryStage, Stage cpuStage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        memoryStage.setX(bounds.getMinX() + 50);
        memoryStage.setY(bounds.getMinY() + 50);

        cpuStage.setX(memoryStage.getX() + memoryStage.getWidth() + 20);
        cpuStage.setY(memoryStage.getY());
        cpuStage.setWidth(700);
    }

    private void startCpuExecutionThread() {
        Thread cpuThread = new Thread(() -> {
            while (!cpu.isHalt()) {
                cpu.step();
            }
            Platform.exit();
        }, "CPU-Execution-Thread");
        cpuThread.setDaemon(true);
        cpuThread.start();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
