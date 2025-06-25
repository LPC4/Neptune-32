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

import java.util.List;

public class Main extends Application {
    private CPU cpu;

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
        new Assembler(cpu).assembleAndLoad(List.of(
                "LOAD_I r1, 5",         // r1 = 5 (counter)
                "LOAD_I r2, 1",         // r2 = 1 (decrement value)
                "LOAD_I r3, 0",         // r3 = 0 (comparison value)

                "loop:",
                "SUB r1, r2",           // r1 -= 1
                "CMP r1, r3",           // compare r1 with 0
                "JNZ loop",             // if r1 != 0, jump back

                "LOAD_I r4, 0x1100",    // r4 = memory address 0x1100
                "STORE r1, r4"          // store final r1 (should be 0)
        ));
    }

    private void visualise() {
        var memoryViewer = new MemoryViewer(cpu);
        var cpuViewer = new CpuViewer(cpu);

        Stage cpuStage = new Stage();
        Stage memoryStage = new Stage();
        cpuViewer.start(cpuStage);
        memoryViewer.start(memoryStage);

        positionStages(memoryStage, cpuStage);
    }


    private void positionStages(Stage memStage, Stage cpuStage) {
        var screenBounds = Screen.getPrimary().getVisualBounds();

        memStage.setX(screenBounds.getMinX() + 50);
        memStage.setY(screenBounds.getMinY() + 50);

        cpuStage.setX(memStage.getX() + memStage.getWidth() + 20);
        cpuStage.setY(memStage.getY());
        cpuStage.setWidth(700);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
