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
import org.lpc.memory.io.IODeviceManager;
import org.lpc.memory.io.devices.ConsoleOutputDevice;
import org.lpc.memory.io.devices.KeyboardInputDevice;
import org.lpc.memory.io.devices.TimerDevice;
import org.lpc.visualization.debug.CpuViewer;
import org.lpc.visualization.debug.ExternalConsole;
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
    private ExternalConsole externalConsole;

    @Override
    public void start(Stage primaryStage) {
        externalConsole = new ExternalConsole();
        externalConsole.start();

        initCpu();
        loadBootRom();
        loadUserProgram();
        createAndShowViewers();
        registerIoDevices();
        startCpuThread();
    }

    private void initCpu() {
        MemoryMap memoryMap = new NeptuneMemoryMap();
        InstructionSet instructionSet = new NeptuneInstructionSet();
        cpu = new CPU(instructionSet, memoryMap, 32);
    }

    private void loadBootRom() {
        assembleAndLoad("/rom/boot.rom.asm", cpu.getMemoryMap().getSyscallCodeStart());
    }

    private void loadUserProgram() {
        assembleAndLoad("/example_programs/keyboard_input.asm", cpu.getMemoryMap().getRamStart());
    }

    private void assembleAndLoad(String resourcePath, int loadAddress) {
        try (var stream = getClass().getResourceAsStream(resourcePath)) {
            if (stream == null) throw new RuntimeException("Resource not found: " + resourcePath);
            List<String> lines = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .lines()
                    .toList();
            new Assembler(cpu).assembleAndLoad(lines, loadAddress);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + resourcePath, e);
        }
    }

    private void createAndShowViewers() {
        CpuViewer cpuViewer = new CpuViewer(cpu);
        MemoryViewer memoryViewer = new MemoryViewer(cpu);
        RGBA32Viewer vramViewer = new RGBA32Viewer(cpu.getMemory(), cpu.getMemoryMap());

        Stage cpuStage = new Stage();
        Stage memoryStage = new Stage();
        Stage vramStage = new Stage();

        cpuViewer.start(cpuStage);
        memoryViewer.start(memoryStage);
        vramViewer.start(vramStage);

        ioScene = externalConsole.getScene();

        positionStages(memoryStage, cpuStage, vramStage);
    }

    private void positionStages(Stage memoryStage, Stage cpuStage, Stage vramStage) {
        Rectangle2D bounds = Screen.getPrimary().getVisualBounds();

        memoryStage.setX(bounds.getMinX());
        memoryStage.setY(bounds.getMinY() + 50);

        cpuStage.setX(bounds.getMaxX() - cpuStage.getWidth());
        cpuStage.setY(memoryStage.getY());

        vramStage.setY(memoryStage.getY());
    }

    private void registerIoDevices() {
        IODeviceManager io = cpu.getMemory().getIo();

        io.register(new KeyboardInputDevice(io.getBaseAddress() + io.getCurrentOffset(), Objects.requireNonNull(ioScene)));
        io.register(new ConsoleOutputDevice(io.getBaseAddress() + io.getCurrentOffset()));
        io.register(new TimerDevice(io.getBaseAddress() + io.getCurrentOffset()));

        io.printDevices();
    }

    private void startCpuThread() {
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
