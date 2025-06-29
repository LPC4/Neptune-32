package org.lpc.external;

import lombok.Getter;
import org.lpc.CPU;
import org.lpc.instructions.Instruction;
import org.lpc.instructions.InstructionSet;
import org.lpc.memory.Memory;
import org.lpc.memory.MemoryBus;
import org.lpc.memory.MemoryHandler;

import java.util.*;

public class Assembler {
    private final InstructionSet instructionSet;
    private final CPU cpu;
    private final LabelManager labelManager;
    private final SyscallManager syscallManager;
    private final MemoryResolver memoryResolver;
    private final MacroManager macroManager;

    public Assembler(CPU cpu) {
        this.cpu = cpu;
        this.instructionSet = cpu.getInstructionSet();
        this.labelManager = new LabelManager();
        this.syscallManager = new SyscallManager(cpu);
        this.memoryResolver = new MemoryResolver(cpu);
        this.macroManager = new MacroManager();
    }

    public void assembleAndLoad(List<String> lines, int baseAddress) {
        labelManager.clear();
        syscallManager.clear();
        macroManager.clear();

        var expanded = macroManager.expandMacros(lines);
        var parsed = parseSourceLines(expanded, baseAddress);

        handleProgramStart(baseAddress);
        writeInstructionsToMemory(parsed);
        syscallManager.finalizeSyscallTable(labelManager);
    }

    private List<SourceLine> parseSourceLines(List<String> lines, int baseAddress) {
        List<SourceLine> parsedLines = new ArrayList<>();
        int address = baseAddress;

        for (String rawLine : lines) {
            String line = cleanLine(rawLine);
            if (line.isEmpty()) continue;

            if (syscallManager.processSyscallDeclaration(line, address)) {
                labelManager.addLabel(syscallManager.getLastLabel(), address);
                continue;
            }

            if (labelManager.processLabelDeclaration(line, address)) continue;
            if (labelManager.processConstantDeclaration(line)) continue;

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
            if (!labelManager.containsLabel("main")) {
                throw new IllegalArgumentException("Program must contain a main label");
            }
            cpu.setProgramCounter(labelManager.getAddress("main"));
        }
    }

    private void writeInstructionsToMemory(List<SourceLine> parsedLines) {
        for (SourceLine line : parsedLines) {
            String resolvedArgs = labelManager.resolveArgs(line.args());
            int[] words = line.instruction().encode(resolvedArgs);

            int addr = line.address();
            MemoryHandler targetMem = memoryResolver.resolve(addr);

            for (int word : words) {
                targetMem.writeWord(addr, word);
                addr += 4;
            }
        }
    }

    private record SourceLine(int address, String mnemonic, String args, Instruction instruction) {}
    private record InstructionInfo(String mnemonic, String args, Instruction instruction) {}
}


// Label Management Component
class LabelManager {
    private final Map<String, Integer> labelToAddress = new HashMap<>();
    private final Map<String, Integer> constants = new HashMap<>();

    public void clear() {
        labelToAddress.clear();
        constants.clear();
    }

    public boolean processLabelDeclaration(String line, int address) {
        if (line.endsWith(":")) {
            String label = line.substring(0, line.length() - 1).trim();
            addLabel(label, address);
            return true;
        }
        return false;
    }

    public boolean processConstantDeclaration(String line) {
        if (!line.startsWith(".const ")) return false;

        String[] parts = line.substring(6).trim().split("\\s+");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid const declaration: " + line);
        }

        String name = parts[0];
        String valueStr = parts[1];

        int value = parseImmediate(valueStr);
        if (labelToAddress.containsKey(name) || constants.containsKey(name)) {
            throw new IllegalArgumentException("Duplicate label or constant: " + name);
        }
        constants.put(name, value);
        return true;
    }

    public void addLabel(String label, int address) {
        if (labelToAddress.containsKey(label) || constants.containsKey(label)) {
            throw new IllegalArgumentException("Duplicate label or constant: " + label);
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
            } else if (constants.containsKey(token)) {
                parts[i] = String.valueOf(constants.get(token));
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

    private int parseImmediate(String value) {
        if (value.startsWith("0x") || value.startsWith("0X")) {
            return Integer.parseUnsignedInt(value.substring(2), 16);
        }
        return Integer.parseInt(value);
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

    public MemoryHandler resolve(int addr) {
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

    private boolean inRange(int addr, MemoryHandler mem) {
        return addr >= mem.getBaseAddress() && addr < mem.getBaseAddress() + mem.getSize();
    }
}

// Macro Component
class MacroManager {
    private record Macro(String name, List<String> args, List<String> body) {}
    private final Map<String, Macro> macros = new HashMap<>();

    public void clear() {
        macros.clear();
    }

    public List<String> expandMacros(List<String> lines) {
        List<String> result = new ArrayList<>();
        Iterator<String> iter = lines.iterator();

        while (iter.hasNext()) {
            String raw = iter.next().trim();
            if (raw.startsWith(".macro")) {
                var macro = parseMacro(raw, iter);
                macros.put(macro.name(), macro);
            } else {
                var expanded = expandLine(raw);
                result.addAll(expanded);
            }
        }

        return result;
    }

    private Macro parseMacro(String header, Iterator<String> iter) {
        String[] tokens = header.split("\\s+");
        if (tokens.length < 2) throw new IllegalArgumentException("Invalid macro definition");

        String name = tokens[1];
        List<String> args = List.of(Arrays.copyOfRange(tokens, 2, tokens.length));
        List<String> body = new ArrayList<>();

        while (iter.hasNext()) {
            String line = iter.next().trim();
            if (line.equalsIgnoreCase(".endmacro")) break;
            body.add(line);
        }

        return new Macro(name, args, body);
    }

    private List<String> expandLine(String line) {
        if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) return List.of(line);

        String[] parts = line.split("\\s+", 2);
        String name = parts[0];

        if (!macros.containsKey(name)) return List.of(line);

        Macro macro = macros.get(name);
        String argStr = parts.length > 1 ? parts[1].trim() : "";
        String[] callArgs = argStr.isEmpty() ? new String[0] : argStr.split("\\s*,\\s*");

        if (callArgs.length != macro.args.size()) {
            throw new IllegalArgumentException("Macro " + name + " expects " + macro.args.size() + " arguments");
        }

        Map<String, String> substitutions = new HashMap<>();
        for (int i = 0; i < macro.args.size(); i++) {
            substitutions.put(macro.args.get(i), callArgs[i]);
        }

        List<String> expanded = new ArrayList<>();
        for (String bodyLine : macro.body) {
            expanded.add(substitute(bodyLine, substitutions));
        }

        return expanded;
    }

    private String substitute(String line, Map<String, String> subs) {
        for (var entry : subs.entrySet()) {
            line = line.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }
        return line;
    }
}
