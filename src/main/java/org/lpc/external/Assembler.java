package org.lpc.external;

import org.lpc.CPU;
import org.lpc.instructions.Instruction;
import org.lpc.instructions.InstructionSet;
import org.lpc.memory.Memory;
import org.lpc.memory.MemoryBus;

import java.util.*;

public class Assembler {
    private final InstructionSet instructionSet;
    private final CPU cpu;

    // List of syscall labels in order of appearance
    private final List<String> syscallLabels = new ArrayList<>();

    public Assembler(CPU cpu) {
        this.cpu = cpu;
        this.instructionSet = cpu.getInstructionSet();
    }

    public void assembleAndLoad(List<String> lines, int baseAddress) {
        Map<String, Integer> labelToAddress = new HashMap<>();
        List<SourceLine> parsedLines = new ArrayList<>();

        int address = baseAddress;

        // Pass 1: Parse lines, collect labels and calculate addresses
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue;

            int commentIdx = Math.min(
                    line.indexOf(';') == -1 ? line.length() : line.indexOf(';'),
                    line.indexOf('#') == -1 ? line.length() : line.indexOf('#')
            );
            line = line.substring(0, commentIdx).trim();

            if (line.isEmpty()) continue;

            // Handle "syscall label:" syntax
            if (line.startsWith("syscall ")) {
                String label = line.substring(8).trim();
                if (!label.endsWith(":")) {
                    throw new IllegalArgumentException("Syscall label must end with ':'");
                }
                label = label.substring(0, label.length() - 1).trim();

                if (labelToAddress.containsKey(label)) {
                    throw new IllegalArgumentException("Duplicate label: " + label);
                }
                labelToAddress.put(label, address);
                syscallLabels.add(label);
                continue;
            }

            // Handle normal labels
            if (line.endsWith(":")) {
                String label = line.substring(0, line.length() - 1).trim();
                if (labelToAddress.containsKey(label)) {
                    throw new IllegalArgumentException("Duplicate label: " + label);
                }
                labelToAddress.put(label, address);
                continue;
            }

            // Parse instruction line
            int spaceIdx = line.indexOf(' ');
            String mnemonic = (spaceIdx == -1) ? line : line.substring(0, spaceIdx).toUpperCase();
            String args = (spaceIdx == -1) ? "" : line.substring(spaceIdx + 1).trim();

            Byte opcode = instructionSet.getOpcode(mnemonic);
            if (opcode == null) {
                throw new IllegalArgumentException("Unknown instruction: '" + line + "'");
            }

            Instruction instr = instructionSet.getInstruction(opcode & 0xFF);
            if (instr == null) {
                throw new IllegalStateException("No implementation for opcode: " + opcode);
            }

            parsedLines.add(new SourceLine(address, mnemonic, args, instr));
            address += instr.getWordCount() * 4;
        }

        // Optional: If assembling a program (RAM), set PC to label START
        if (isRamAddress(baseAddress)) {
            if (!labelToAddress.containsKey("START")) {
                throw new IllegalArgumentException("Program must contain a START label");
            }
            cpu.setProgramCounter(labelToAddress.get("START"));
        }

        // Pass 2: Encode instructions and write to memory
        for (SourceLine line : parsedLines) {
            String resolvedArgs = resolveArgs(line.args(), labelToAddress);

            int[] words;
            try {
                words = line.instruction().encode(resolvedArgs);
            } catch (Exception e) {
                throw new RuntimeException("Failed to encode " + line.mnemonic() + " " + line.args(), e);
            }

            int addr = line.address();
            Memory targetMem = resolveMemoryForAddress(addr);

            for (int word : words) {
                targetMem.writeWord(addr, word);
                addr += 4;
            }
        }

        // Write the syscall table at syscallTableStart in ROM
        if (!syscallLabels.isEmpty()) {
            int syscallTableAddr = cpu.getMemoryMap().getSyscallTableStart();
            Memory rom = cpu.getMemory().getRom();

            for (int i = 0; i < syscallLabels.size(); i++) {
                String label = syscallLabels.get(i);
                Integer target = labelToAddress.get(label);
                if (target == null) {
                    throw new IllegalStateException("Label not found for syscall: " + label);
                }
                rom.writeWord(syscallTableAddr + i * 4, target);
            }
        }

        // Output syscall information after assembly
        outputSyscallInfo(labelToAddress);
    }

    /**
     * Outputs information about the syscalls that were assembled
     */
    private void outputSyscallInfo(Map<String, Integer> labelToAddress) {
        if (syscallLabels.isEmpty()) {
            System.out.println("No syscalls found in assembly.");
            return;
        }

        System.out.println("\n=== SYSCALL TABLE ===");
        int syscallTableAddr = cpu.getMemoryMap().getSyscallTableStart();

        System.out.printf("Syscall table base address: 0x%08X\n", syscallTableAddr);
        System.out.printf("Number of syscalls: %d\n\n", syscallLabels.size());

        System.out.println("Syscall ID | Label           | Target Address | Table Entry Address");
        System.out.println("-----------|-----------------|----------------|-------------------");

        for (int i = 0; i < syscallLabels.size(); i++) {
            String label = syscallLabels.get(i);
            Integer targetAddr = labelToAddress.get(label);
            int tableEntryAddr = syscallTableAddr + i * 4;

            System.out.printf("%-10d | %-15s | 0x%08X     | 0x%08X\n",
                    i, label, targetAddr, tableEntryAddr);
        }

        System.out.println("\n=== SYSCALL DETAILS ===");
        for (int i = 0; i < syscallLabels.size(); i++) {
            String label = syscallLabels.get(i);
            Integer targetAddr = labelToAddress.get(label);
            Memory targetMem = resolveMemoryForAddress(targetAddr);
            String memoryType = getMemoryTypeName(targetAddr);

            System.out.printf("Syscall %d (%s):\n", i, label);
            System.out.printf("  - Target address: 0x%08X\n", targetAddr);
            System.out.printf("  - Memory region: %s\n", memoryType);
            System.out.printf("  - Table entry: [0x%08X] = 0x%08X\n\n",
                    syscallTableAddr + i * 4, targetAddr);
        }
    }

    /**
     * Returns a human-readable name for the memory region containing the given address
     */
    private String getMemoryTypeName(int addr) {
        MemoryBus mem = cpu.getMemory();
        if (inRange(addr, mem.getRom())) return "ROM";
        if (inRange(addr, mem.getRam())) return "RAM";
        if (inRange(addr, mem.getVram())) return "VRAM";
        if (inRange(addr, mem.getIo())) return "I/O";
        return "UNKNOWN";
    }

    /**
     * Public method to get syscall information after assembly
     */
    public List<SyscallInfo> getSyscallInfo() {
        List<SyscallInfo> info = new ArrayList<>();
        int syscallTableAddr = cpu.getMemoryMap().getSyscallTableStart();

        for (int i = 0; i < syscallLabels.size(); i++) {
            String label = syscallLabels.get(i);
            // Note: This would need labelToAddress to be instance variable to work properly
            // For now, this is a structure to show what could be returned
            info.add(new SyscallInfo(i, label, 0, syscallTableAddr + i * 4));
        }
        return info;
    }

    /**
     * Record to hold syscall information
     */
    public record SyscallInfo(int id, String label, int targetAddress, int tableEntryAddress) {}

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

    private Memory resolveMemoryForAddress(int addr) {
        MemoryBus mem = cpu.getMemory();
        if (inRange(addr, mem.getRom())) return mem.getRom();
        if (inRange(addr, mem.getRam())) return mem.getRam();
        if (inRange(addr, mem.getVram())) return mem.getVram();
        if (inRange(addr, mem.getIo())) return mem.getIo();
        throw new IllegalArgumentException(String.format("Address 0x%08X does not map to any memory region", addr));
    }

    private boolean inRange(int addr, Memory mem) {
        return addr >= mem.getBaseAddress() && addr < mem.getBaseAddress() + mem.getSize();
    }

    private boolean isRamAddress(int addr) {
        return inRange(addr, cpu.getMemory().getRam());
    }

    private record SourceLine(int address, String mnemonic, String args, Instruction instruction) {}
}