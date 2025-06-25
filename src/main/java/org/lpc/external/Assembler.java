package org.lpc.external;

import org.lpc.CPU;
import org.lpc.instructions.Instruction;
import org.lpc.instructions.InstructionSet;

import java.util.*;

public class Assembler {
    private final InstructionSet instructionSet;
    private final CPU cpu;

    public Assembler(CPU cpu) {
        this.cpu = cpu;
        this.instructionSet = cpu.getInstructionSet();
    }

    public void assembleAndLoad(List<String> lines) {
        Map<String, Integer> labelToAddress = new HashMap<>();
        List<SourceLine> parsedLines = new ArrayList<>();

        int programStart = cpu.getMemoryMap().getProgramStart();
        int address = programStart;

        // Pass 1: Parse lines, collect labels and calculate addresses
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) continue;

            if (line.endsWith(":")) {
                String label = line.substring(0, line.length() - 1).trim();
                if (labelToAddress.containsKey(label)) {
                    throw new IllegalArgumentException("Duplicate label: " + label);
                }
                labelToAddress.put(label, address);
            } else {
                int spaceIdx = line.indexOf(' ');
                String mnemonic = (spaceIdx == -1) ? line : line.substring(0, spaceIdx).toUpperCase();
                String args = (spaceIdx == -1) ? "" : line.substring(spaceIdx + 1).trim();

                Byte opcode = instructionSet.getOpcode(mnemonic);
                if (opcode == null) {
                    throw new IllegalArgumentException("Unknown instruction: " + mnemonic);
                }

                Instruction instr = instructionSet.getInstruction(opcode & 0xFF);
                if (instr == null) {
                    throw new IllegalStateException("No implementation for opcode: " + opcode);
                }

                parsedLines.add(new SourceLine(address, mnemonic, args, instr));
                address += instr.getWordCount() * 4;
            }
        }

        // Pass 2: Encode instructions, resolve labels
        for (SourceLine line : parsedLines) {
            String resolvedArgs = resolveArgs(line.args(), labelToAddress);
            int[] words = line.instruction().encode(resolvedArgs);

            int addr = line.address();
            for (int word : words) {
                cpu.getMemory().writeWord(addr, word);
                addr += 4;
            }
        }
    }

    private String resolveArgs(String args, Map<String, Integer> labelToAddress) {
        if (args.isEmpty()) return "";

        String[] parts = args.split(",");
        for (int i = 0; i < parts.length; i++) {
            String token = parts[i].trim();
            if (labelToAddress.containsKey(token)) {
                parts[i] = String.valueOf(labelToAddress.get(token));
            }
        }
        return String.join(",", parts);
    }

    private record SourceLine(int address, String mnemonic, String args, Instruction instruction) {}
}
