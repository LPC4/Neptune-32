package org.lpc;

import javafx.application.Application;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.lpc.external.Assembler;
import org.lpc.instructions.InstructionSet;
import org.lpc.instructions.NeptuneInstructionSet;
import org.lpc.memory.MemoryMap;
import org.lpc.memory.NeptuneMemoryMap;
import org.lpc.visualization.debug.CpuViewer;
import org.lpc.visualization.debug.MemoryViewer;
import org.lpc.visualization.vram.RGBA32Visualiser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Main extends Application {
    private CPU cpu;

    private static final int VRAM_PIXEL_SCALE = 4;

    @Override
    public void start(Stage primaryStage) {
        MemoryMap memoryMap = new NeptuneMemoryMap();
        InstructionSet instructionSet = new NeptuneInstructionSet();
        cpu = new CPU(instructionSet, memoryMap);

        loadProgram();

        while (true) {
            try {
                cpu.step();
            } catch (Exception e) {
                break;
            }
        }

        visualise();
    }

    private void loadProgram() {
        try (var stream = getClass().getResourceAsStream("/test.asm")) {
            if (stream == null) throw new RuntimeException("Resource not found: test.asm");
            List<String> lines = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().toList();
            new Assembler(cpu).assembleAndLoad(lines);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load assembly program", e);
        }
    }


    private void visualise() {
        var memoryViewer = new MemoryViewer(cpu);
        var cpuViewer = new CpuViewer(cpu);
        var vramViewer = new RGBA32Visualiser(cpu.getMemory(), cpu.getMemoryMap());

        Stage cpuStage = new Stage();
        Stage memoryStage = new Stage();
        Stage vramStage = new Stage();

        cpuViewer.start(cpuStage);
        memoryViewer.start(memoryStage);
        vramViewer.start(vramStage);

        positionStages(memoryStage, cpuStage, vramStage);
    }

    private void positionStages(Stage memStage, Stage cpuStage, Stage vramStage) {
        var screenBounds = Screen.getPrimary().getVisualBounds();

        memStage.setX(screenBounds.getMinX() + 50);
        memStage.setY(screenBounds.getMinY() + 50);

        cpuStage.setX(memStage.getX() + memStage.getWidth() + 20);
        cpuStage.setY(memStage.getY());
        cpuStage.setWidth(700);

        int vramWidth = cpu.getMemoryMap().getVramWidth();
        int vramHeight = cpu.getMemoryMap().getVramHeight();

        vramStage.setWidth(vramWidth * VRAM_PIXEL_SCALE + 16);
        vramStage.setHeight(vramHeight * VRAM_PIXEL_SCALE + 39);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
