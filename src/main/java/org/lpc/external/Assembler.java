package org.lpc.external;

import lombok.Getter;
import org.lpc.CPU;
import org.lpc.instructions.Instruction;
import org.lpc.instructions.InstructionSet;
import org.lpc.memory.Memory;
import org.lpc.memory.MemoryBus;

import java.util.*;

public class Assembler {
    private final InstructionSet instructionSet;
    private final CPU cpu;
    private final LabelManager labelManager;
    private final SyscallManager syscallManager;
    private final MemoryResolver memoryResolver;

    public Assembler(CPU cpu) {
        this.cpu = cpu;
        this.instructionSet = cpu.getInstructionSet();
        this.labelManager = new LabelManager();
        this.syscallManager = new SyscallManager(cpu);
        this.memoryResolver = new MemoryResolver(cpu);
    }

    public void assembleAndLoad(List<String> lines, int baseAddress) {
        // Clear previous state
        labelManager.clear();
        syscallManager.clear();

        // First pass - parse and collect labels/syscalls
        List<SourceLine> parsedLines = parseSourceLines(lines, baseAddress);

        // Set program counter if assembling to RAM
        handleProgramStart(baseAddress);

        // Second pass - encode and write to memory
        writeInstructionsToMemory(parsedLines);

        // Finalize syscall table
        syscallManager.finalizeSyscallTable(labelManager);
    }

    private List<SourceLine> parseSourceLines(List<String> lines, int baseAddress) {
        List<SourceLine> parsedLines = new ArrayList<>();
        int address = baseAddress;

        for (String rawLine : lines) {
            String line = cleanLine(rawLine);
            if (line.isEmpty()) continue;

            // Handle syscalls
            if (syscallManager.processSyscallDeclaration(line, address)) {
                labelManager.addLabel(syscallManager.getLastLabel(), address);
                continue;
            }

            // Handle regular labels
            if (labelManager.processLabelDeclaration(line, address)) {
                continue;
            }

            // Parse regular instructions
            InstructionInfo info = parseInstruction(line);
            parsedLines.add(new SourceLine(address, info.mnemonic(), info.args(), info.instruction()));
            address += info.instruction().getWordCount() * 4;
        }

        return parsedLines;
    }

    private String cleanLine(String line) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) return "";

        int commentIdx = Math.min(
                line.indexOf(';') == -1 ? line.length() : line.indexOf(';'),
                line.indexOf('#') == -1 ? line.length() : line.indexOf('#')
        );
        return line.substring(0, commentIdx).trim();
    }

    private InstructionInfo parseInstruction(String line) {
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

        return new InstructionInfo(mnemonic, args, instr);
    }

    private void handleProgramStart(int baseAddress) {
        if (memoryResolver.isRamAddress(baseAddress)) {
            if (!labelManager.containsLabel("START")) {
                throw new IllegalArgumentException("Program must contain a START label");
            }
            cpu.setProgramCounter(labelManager.getAddress("START"));
        }
    }

    private void writeInstructionsToMemory(List<SourceLine> parsedLines) {
        for (SourceLine line : parsedLines) {
            String resolvedArgs = labelManager.resolveArgs(line.args());
            int[] words = line.instruction().encode(resolvedArgs);

            int addr = line.address();
            Memory targetMem = memoryResolver.resolve(addr);

            for (int word : words) {
                targetMem.writeWord(addr, word);
                addr += 4;
            }
        }
    }

    // Helper records
    private record SourceLine(int address, String mnemonic, String args, Instruction instruction) {}
    private record InstructionInfo(String mnemonic, String args, Instruction instruction) {}
}

// Label Management Component
class LabelManager {
    private final Map<String, Integer> labelToAddress = new HashMap<>();

    public void clear() {
        labelToAddress.clear();
    }

    public boolean processLabelDeclaration(String line, int address) {
        if (line.endsWith(":")) {
            String label = line.substring(0, line.length() - 1).trim();
            addLabel(label, address);
            return true;
        }
        return false;
    }

    public void addLabel(String label, int address) {
        if (labelToAddress.containsKey(label)) {
            throw new IllegalArgumentException("Duplicate label: " + label);
        }
        labelToAddress.put(label, address);
    }

    public String resolveArgs(String args) {
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

    public boolean containsLabel(String label) {
        return labelToAddress.containsKey(label);
    }

    public int getAddress(String label) {
        return labelToAddress.get(label);
    }
}

// Syscall Management Component
class SyscallManager {
    private final CPU cpu;
    private final Map<Integer, String> syscallMap = new HashMap<>();
    @Getter
    private String lastLabel;

    public SyscallManager(CPU cpu) {
        this.cpu = cpu;
    }

    public void clear() {
        syscallMap.clear();
        lastLabel = null;
    }

    public boolean processSyscallDeclaration(String line, int address) {
        if (!line.startsWith("syscall ")) return false;

        String rest = line.substring(8).trim();
        if (!rest.endsWith(":")) {
            throw new IllegalArgumentException("Syscall label must end with ':'");
        }

        String[] parts = rest.substring(0, rest.length() - 1).trim().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Syscall syntax must be: syscall number label:");
        }

        try {
            int syscallNum = Integer.parseInt(parts[0]);
            String label = parts[1];

            if (syscallMap.containsKey(syscallNum)) {
                throw new IllegalArgumentException("Duplicate syscall number: " + syscallNum);
            }

            syscallMap.put(syscallNum, label);
            lastLabel = label;
            return true;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid syscall number: " + parts[0]);
        }
    }

    public void finalizeSyscallTable(LabelManager labelManager) {
        if (syscallMap.isEmpty()) return;

        int syscallTableAddr = cpu.getMemoryMap().getSyscallTableStart();
        Memory rom = cpu.getMemory().getRom();

        // Find the highest syscall number to determine table size
        int maxSyscallNum = syscallMap.keySet().stream().max(Integer::compare).orElse(0);

        // Initialize all entries to 0 first
        for (int i = 0; i <= maxSyscallNum; i++) {
            rom.writeWord(syscallTableAddr + i * 4, 0);
        }

        // Write the defined syscalls
        for (Map.Entry<Integer, String> entry : syscallMap.entrySet()) {
            int syscallNum = entry.getKey();
            String label = entry.getValue();
            Integer target = labelManager.getAddress(label);
            if (target == null) {
                throw new IllegalStateException("Label not found for syscall: " + label);
            }
            rom.writeWord(syscallTableAddr + syscallNum * 4, target);
        }
    }
}

// Memory Resolution Component
class MemoryResolver {
    private final CPU cpu;

    public MemoryResolver(CPU cpu) {
        this.cpu = cpu;
    }

    public Memory resolve(int addr) {
        MemoryBus mem = cpu.getMemory();
        if (inRange(addr, mem.getRom())) return mem.getRom();
        if (inRange(addr, mem.getRam())) return mem.getRam();
        if (inRange(addr, mem.getVram())) return mem.getVram();
        if (inRange(addr, mem.getIo())) return mem.getIo();
        throw new IllegalArgumentException(String.format("Address 0x%08X does not map to any memory region", addr));
    }

    public boolean isRamAddress(int addr) {
        return inRange(addr, cpu.getMemory().getRam());
    }

    private boolean inRange(int addr, Memory mem) {
        return addr >= mem.getBaseAddress() && addr < mem.getBaseAddress() + mem.getSize();
    }
}