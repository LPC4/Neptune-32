package org.lpc.memory;

import org.lpc.cpu.CPU;
import org.lpc.instructions.Instruction;
import org.lpc.instructions.InstructionSet;

import java.util.List;
import java.util.stream.Stream;

public class Assembler {
    private final InstructionSet instructionSet;
    private final CPU cpu;

    public Assembler(CPU cpu) {
        this.cpu = cpu;
        this.instructionSet = cpu.getInstructionSet();
    }

    public void assembleAndLoad(List<String> lines) {
        int address = cpu.getMemoryMap().getProgramStart();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue;

            // Split mnemonic and args
            int spaceIdx = line.indexOf(' ');
            String mnemonic = (spaceIdx == -1) ? line : line.substring(0, spaceIdx).toUpperCase();
            String args = (spaceIdx == -1) ? "" : line.substring(spaceIdx + 1).trim();

            Byte opcode = instructionSet.getOpcode(mnemonic);
            if (opcode == null) {
                throw new IllegalArgumentException("Unknown instruction: " + mnemonic);
            }

            Instruction instr = instructionSet.getInstruction((opcode & 0xFF));
            if (instr == null) {
                throw new IllegalStateException("No instruction implementation for opcode " + opcode);
            }

            int encodedWord = instr.encode(args);

            cpu.getMemory().writeWord(address, encodedWord);

            address += 4;
        }
    }
}
