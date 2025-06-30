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
    private final DataSectionManager dataSectionManager;

    public Assembler(CPU cpu) {
        this.cpu = cpu;
        this.instructionSet = cpu.getInstructionSet();
        this.labelManager = new LabelManager();
        this.syscallManager = new SyscallManager(cpu);
        this.memoryResolver = new MemoryResolver(cpu);
        this.macroManager = new MacroManager();
        this.dataSectionManager = new DataSectionManager(cpu);
    }

    public void assembleAndLoad(List<String> lines, int baseAddress) {
        labelManager.clear();
        syscallManager.clear();
        macroManager.clear();
        dataSectionManager.clear();

        var expanded = macroManager.expandMacros(lines);
        var sections = dataSectionManager.parseSections(expanded);

        // Process data section first to determine final program start address
        int programStartAddress = baseAddress;
        if (sections.dataSection() != null) {
            dataSectionManager.processDataSection(sections.dataSection(), labelManager);
            // Calculate program start after data + 4-word gap
            programStartAddress = dataSectionManager.getNextAvailableAddress() + 16; // 4 words * 4 bytes
        }

        var parsed = parseSourceLines(sections.codeSection(), programStartAddress);

        writeDataToMemory();
        handleProgramStart(programStartAddress);
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

    private void handleProgramStart(int programStartAddress) {
        if (memoryResolver.isRamAddress(programStartAddress)) {
            if (labelManager.containsLabel("main")) {
                cpu.setProgramCounter(labelManager.getAddress("main"));
            } else {
                cpu.setProgramCounter(programStartAddress);
            }
        }
    }

    private void writeDataToMemory() {
        dataSectionManager.writeToMemory(memoryResolver);
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

class DataSectionManager {
    private final CPU cpu;
    private final List<DataItem> dataItems = new ArrayList<>();
    private int currentDataAddress;

    public DataSectionManager(CPU cpu) {
        this.cpu = cpu;
        this.currentDataAddress = cpu.getMemoryMap().getRamStart();
    }

    public void clear() {
        dataItems.clear();
        currentDataAddress = cpu.getMemoryMap().getRamStart();
    }

    public int getNextAvailableAddress() {
        return currentDataAddress;
    }

    public SectionInfo parseSections(List<String> lines) {
        List<String> dataLines = new ArrayList<>();
        List<String> codeLines = new ArrayList<>();
        boolean inDataSection = false;
        boolean hasStartDirective = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.equalsIgnoreCase(".data")) {
                inDataSection = true;
                continue;
            } else if (trimmed.equalsIgnoreCase(".code")) {
                inDataSection = false;
                hasStartDirective = true;
                continue;
            }

            if (inDataSection) {
                if (!trimmed.isEmpty() && !trimmed.startsWith(";") && !trimmed.startsWith("#")) {
                    dataLines.add(line);
                }
            } else {
                codeLines.add(line);
            }
        }

        return new SectionInfo(
                dataLines.isEmpty() ? null : dataLines,
                codeLines,
                hasStartDirective
        );
    }

    public void processDataSection(List<String> dataLines, LabelManager labelManager) {
        for (String line : dataLines) {
            processDataDeclaration(line.trim(), labelManager);
        }
    }

    private void processDataDeclaration(String line, LabelManager labelManager) {
        // Handle different data declaration formats
        if (line.toLowerCase().startsWith("string ")) {
            processStringDeclaration(line, labelManager);
        } else if (line.toLowerCase().startsWith("int ") ||
                line.toLowerCase().startsWith("word ")) {
            processIntDeclaration(line, labelManager);
        } else if (line.toLowerCase().startsWith("byte ")) {
            processByteDeclaration(line, labelManager);
        } else if (line.toLowerCase().startsWith("array ")) {
            processArrayDeclaration(line, labelManager);
        } else if (line.toLowerCase().startsWith("buffer ")) {
            processBufferDeclaration(line, labelManager);
        } else {
            throw new IllegalArgumentException("Unknown data declaration: " + line);
        }
    }

    private void processStringDeclaration(String line, LabelManager labelManager) {
        // Format: String name = "value"
        String[] parts = line.split("=", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid string declaration: " + line);
        }

        String name = parts[0].substring(6).trim(); // Remove "string"
        String valueStr = parts[1].trim();

        if (!valueStr.startsWith("\"") || !valueStr.endsWith("\"")) {
            throw new IllegalArgumentException("String value must be quoted: " + line);
        }

        String value = valueStr.substring(1, valueStr.length() - 1);
        value = unescapeString(value); // Handle escape sequences

        // Add null terminator
        byte[] bytes = (value + "\0").getBytes();

        labelManager.addLabel(name, currentDataAddress);
        dataItems.add(new StringDataItem(currentDataAddress, name, bytes));

        // Align to word boundary
        currentDataAddress += alignToWord(bytes.length);
    }

    private void processIntDeclaration(String line, LabelManager labelManager) {
        // Format: Int name = value  or  Word name = value
        String[] parts = line.split("=", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid integer declaration: " + line);
        }

        String name = parts[0].split("\\s+")[1].trim();
        String valueStr = parts[1].trim();

        int value = parseValue(valueStr, labelManager);

        labelManager.addLabel(name, currentDataAddress);
        dataItems.add(new IntDataItem(currentDataAddress, name, value));
        currentDataAddress += 4; // 32-bit word
    }

    private void processByteDeclaration(String line, LabelManager labelManager) {
        // Format: Byte name = value
        String[] parts = line.split("=", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid byte declaration: " + line);
        }

        String name = parts[0].substring(4).trim();
        String valueStr = parts[1].trim();

        int value = parseValue(valueStr, labelManager);
        if (value < -128 || value > 255) {
            throw new IllegalArgumentException("Byte value out of range: " + value);
        }

        labelManager.addLabel(name, currentDataAddress);
        dataItems.add(new ByteDataItem(currentDataAddress, name, (byte)value));
        currentDataAddress += alignToWord(1); // Align to word boundary
    }

    private void processArrayDeclaration(String line, LabelManager labelManager) {
        // Format: Array name[size] = val1, val2, ... or Array name[size]
        String name;
        int size;
        List<Integer> values = new ArrayList<>();

        if (line.contains("=")) {
            String[] parts = line.split("=", 2);
            String leftPart = parts[0].substring(5).trim(); // Remove "array"
            String valuesPart = parts[1].trim();

            // Parse name[size]
            int bracketStart = leftPart.indexOf('[');
            int bracketEnd = leftPart.indexOf(']');
            if (bracketStart == -1 || bracketEnd == -1) {
                throw new IllegalArgumentException("Invalid array declaration: " + line);
            }

            name = leftPart.substring(0, bracketStart).trim();
            size = Integer.parseInt(leftPart.substring(bracketStart + 1, bracketEnd).trim());

            // Parse values val1, val2, ... (without curly braces)
            if (!valuesPart.trim().isEmpty()) {
                for (String val : valuesPart.split(",")) {
                    values.add(parseValue(val.trim(), labelManager));
                }
            }
        } else {
            // Format: Array name[size] (uninitialized)
            String leftPart = line.substring(5).trim();
            int bracketStart = leftPart.indexOf('[');
            int bracketEnd = leftPart.indexOf(']');

            name = leftPart.substring(0, bracketStart).trim();
            size = Integer.parseInt(leftPart.substring(bracketStart + 1, bracketEnd).trim());
        }

        // Pad with zeros if needed
        while (values.size() < size) {
            values.add(0);
        }

        if (values.size() > size) {
            throw new IllegalArgumentException("Too many initializers for array " + name);
        }

        labelManager.addLabel(name, currentDataAddress);
        dataItems.add(new ArrayDataItem(currentDataAddress, name, values));
        currentDataAddress += size * 4; // Each element is 4 bytes
    }

    private void processBufferDeclaration(String line, LabelManager labelManager) {
        // Format: Buffer name[size] - allocates uninitialized space
        String leftPart = line.substring(6).trim(); // Remove "buffer"
        int bracketStart = leftPart.indexOf('[');
        int bracketEnd = leftPart.indexOf(']');

        if (bracketStart == -1 || bracketEnd == -1) {
            throw new IllegalArgumentException("Invalid buffer declaration: " + line);
        }

        String name = leftPart.substring(0, bracketStart).trim();
        int size = Integer.parseInt(leftPart.substring(bracketStart + 1, bracketEnd).trim());

        labelManager.addLabel(name, currentDataAddress);
        dataItems.add(new BufferDataItem(currentDataAddress, name, size));
        currentDataAddress += alignToWord(size);
    }

    public void writeToMemory(MemoryResolver memoryResolver) {
        for (DataItem item : dataItems) {
            item.writeToMemory(memoryResolver);
        }
    }

    private String unescapeString(String str) {
        return str.replace("\\n", "\n")
                .replace("\\t", "\t")
                .replace("\\r", "\r")
                .replace("\\\\", "\\")
                .replace("\\\"", "\"")
                .replace("\\0", "\0");
    }

    private int parseValue(String valueStr, LabelManager labelManager) {
        valueStr = valueStr.trim();

        // Check if it's a label/constant reference
        if (labelManager.containsLabel(valueStr)) {
            return labelManager.getAddress(valueStr);
        }

        // Parse numeric value
        if (valueStr.startsWith("0x") || valueStr.startsWith("0X")) {
            return Integer.parseUnsignedInt(valueStr.substring(2), 16);
        } else if (valueStr.startsWith("0b") || valueStr.startsWith("0B")) {
            return Integer.parseUnsignedInt(valueStr.substring(2), 2);
        } else {
            return Integer.parseInt(valueStr);
        }
    }

    private int alignToWord(int size) {
        return (size + 3) & ~3; // Round up to multiple of 4
    }

    // Data item classes
    private abstract static class DataItem {
        protected final int address;
        protected final String name;

        public DataItem(int address, String name) {
            this.address = address;
            this.name = name;
        }

        public abstract void writeToMemory(MemoryResolver resolver);
    }

    private static class StringDataItem extends DataItem {
        private final byte[] data;

        public StringDataItem(int address, String name, byte[] data) {
            super(address, name);
            this.data = data;
        }

        @Override
        public void writeToMemory(MemoryResolver resolver) {
            MemoryHandler mem = resolver.resolve(address);
            for (int i = 0; i < data.length; i++) {
                mem.writeByte(address + i, data[i]);
            }
        }
    }

    private static class IntDataItem extends DataItem {
        private final int value;

        public IntDataItem(int address, String name, int value) {
            super(address, name);
            this.value = value;
        }

        @Override
        public void writeToMemory(MemoryResolver resolver) {
            MemoryHandler mem = resolver.resolve(address);
            mem.writeWord(address, value);
        }
    }

    private static class ByteDataItem extends DataItem {
        private final byte value;

        public ByteDataItem(int address, String name, byte value) {
            super(address, name);
            this.value = value;
        }

        @Override
        public void writeToMemory(MemoryResolver resolver) {
            MemoryHandler mem = resolver.resolve(address);
            mem.writeByte(address, value);
        }
    }

    private static class ArrayDataItem extends DataItem {
        private final List<Integer> values;

        public ArrayDataItem(int address, String name, List<Integer> values) {
            super(address, name);
            this.values = values;
        }

        @Override
        public void writeToMemory(MemoryResolver resolver) {
            MemoryHandler mem = resolver.resolve(address);
            for (int i = 0; i < values.size(); i++) {
                mem.writeWord(address + i * 4, values.get(i));
            }
        }
    }

    private static class BufferDataItem extends DataItem {
        private final int size;

        public BufferDataItem(int address, String name, int size) {
            super(address, name);
            this.size = size;
        }

        @Override
        public void writeToMemory(MemoryResolver resolver) {
            MemoryHandler mem = resolver.resolve(address);
            // Initialize buffer to zeros
            for (int i = 0; i < size; i++) {
                mem.writeByte(address + i, (byte)0);
            }
        }
    }

    public record SectionInfo(List<String> dataSection, List<String> codeSection, boolean hasStartDirective) {}
}