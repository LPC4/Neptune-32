package org.lpc.instructions;

import org.lpc.CPU;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/*
 * NeptuneInstructionSet - Complete Instruction Reference
 *
 * Format: [instruction] [arguments] - Description
 *
 * =====================================================
 * Arithmetic Instructions:
 * =====================================================
 * ADD   rDest, rSrc      - Add rSrc to rDest, store in rDest, update flags
 * ADDI  rDest, imm       - Add immediate to rDest, store in rDest, update flags
 * SUB   rDest, rSrc      - Subtract rSrc from rDest, store in rDest, update flags
 * SUBI  rDest, imm       - Subtract immediate from rDest, store in rDest, update flags
 * MUL   rDest, rSrc      - Multiply rDest by rSrc, store in rDest, update flags
 * MULI  rDest, imm       - Multiply rDest by immediate, store in rDest, update flags
 * DIV   rDest, rSrc      - Divide rDest by rSrc, store in rDest, update flags (throws if divide by zero)
 * DIVI  rDest, imm       - Divide rDest by immediate, store in rDest, update flags (throws if divide by zero)
 * MOD   rDest, rSrc      - Modulo rDest by rSrc, store in rDest, update flags (throws if modulo by zero)
 * MODI  rDest, imm       - Modulo rDest by immediate, store in rDest, update flags (throws if modulo by zero)
 * INC   rDest            - Increment rDest by 1, update flags
 * DEC   rDest            - Decrement rDest by 1, update flags
 *
 * =====================================================
 * Logical Instructions:
 * =====================================================
 * AND   rDest, rSrc      - Bitwise AND rDest with rSrc, store in rDest, update flags
 * ANDI  rDest, imm       - Bitwise AND rDest with immediate, store in rDest, update flags
 * OR    rDest, rSrc      - Bitwise OR rDest with rSrc, store in rDest, update flags
 * ORI   rDest, imm       - Bitwise OR rDest with immediate, store in rDest, update flags
 * XOR   rDest, rSrc      - Bitwise XOR rDest with rSrc, store in rDest, update flags
 * XORI  rDest, imm       - Bitwise XOR rDest with immediate, store in rDest, update flags
 * NOT   rDest            - Bitwise NOT of rDest, store in rDest, update flags
 *
 * =====================================================
 * Shift Instructions:
 * =====================================================
 * SHL   rDest, shift     - Shift rDest left by shift bits, store in rDest, update flags
 * SHR   rDest, shift     - Logical shift rDest right by shift bits, store in rDest, update flags
 *
 * =====================================================
 * Memory Instructions:
 * =====================================================
 * LOAD  rDest, rAddr     - Load word from memory at rAddr into rDest, update flags
 * STORE rSrc, rAddr      - Store word from rSrc into memory at rAddr
 *
 * =====================================================
 * Control Flow Instructions:
 * =====================================================
 * JMP   address          - Jump to address unconditionally
 * JZ    address          - Jump to address if zero flag set
 * JNZ   address          - Jump to address if zero flag not set
 * CALL  address          - Push PC, jump to address (2-word instruction)
 * RET                   - Pop PC from stack and jump
 *
 * =====================================================
 * Stack Instructions:
 * =====================================================
 * PUSH  rSrc             - Push register rSrc onto stack
 * POP   rDest            - Pop from stack into rDest, update flags
 *
 * =====================================================
 * Data Movement Instructions:
 * =====================================================
 * MOV   rDest, rSrc      - Copy value from rSrc into rDest, update flags
 * LOADI rDest, imm      - Load immediate imm into rDest, update flags (2-word instruction)
 * CLR   rDest            - Clear rDest (set to 0), update flags
 *
 * =====================================================
 * Comparison Instructions:
 * =====================================================
 * CMP   rA, rB           - Compare rA with rB (rA - rB), update flags (no register change)
 * CMPI  rA, imm          - Compare rA with immediate, update flags (no register change)
 * TEST  rA, rB           - Bitwise AND of rA and rB, update flags (no register change)
 * TESTI rA, imm          - Bitwise AND of rA and immediate, update flags (no register change)
 *
 * =====================================================
 * System Instructions:
 * =====================================================
 * SYSCALL               - Executes system call specified by r0:
 *        1) Get VRAM info: r1 = start, r2 = size
 * NOP                   - No operation
 *
 * =====================================================
 * Register Convention:
 * r0-r15 - General purpose registers
 *
 * =====================================================
 * Immediate Values:
 * Can be specified as:
 * - Decimal: 42
 * - Hexadecimal: 0x2A
 *
 * =====================================================
 * Flag Behavior:
 * - Zero flag (Z): Set when result is zero
 * - Negative flag (N): Set when result is negative
 * - Overflow flag (V): Set when arithmetic overflow occurs
 * - Carry flag (C): Set when carry occurs
 *
 * =====================================================
 * Memory Access:
 * - All memory operations use word (32-bit) addressing
 * - Memory is byte-addressable but accessed in words
 */

public class NeptuneInstructionSet implements InstructionSet {
    private final Map<Byte, Instruction> instructionMap = new HashMap<>();
    private final Map<String, Byte> nameToOpcode = new HashMap<>();
    private final Map<Byte, String> opcodeToName = new HashMap<>();
    private byte nextOpcode = 1;

    public NeptuneInstructionSet() {
        registerOpcodes();
        logInstructions();
    }

    private void registerOpcodes() {
        registerArithmeticInstructions();
        registerLogicalInstructions();
        registerShiftInstructions();
        registerMemoryInstructions();
        registerControlFlowInstructions();
        registerStackInstructions();
        registerDataMovementInstructions();
        registerComparisonInstructions();
        registerSystemInstructions();
    }

    // Helper method for splitting arguments
    private String[] splitArgs(String args, int expectedCount) {
        String[] parts = args.split(",");
        if (parts.length != expectedCount) {
            throw new IllegalArgumentException("Invalid arguments: " + args);
        }
        for (int i = 0; i < parts.length; i++) {
            parts[i] = parts[i].trim();
        }
        return parts;
    }

    private void registerArithmeticInstructions() {
        // Register-register operations
        registerBinaryOp("ADD", Integer::sum, true);
        registerBinaryOp("SUB", (a, b) -> a - b, true);
        registerBinaryOp("MUL", (a, b) -> a * b, false);
        registerBinaryOp("DIV", (a, b) -> {
            if (b == 0) throw new ArithmeticException("Division by zero");
            return a / b;
        }, false);
        registerBinaryOp("MOD", (a, b) -> {
            if (b == 0) throw new ArithmeticException("Modulo by zero");
            return a % b;
        }, false);

        // Register-immediate operations
        registerBinaryImmediateOp("ADDI", Integer::sum, true);
        registerBinaryImmediateOp("SUBI", (a, b) -> a - b, true);
        registerBinaryImmediateOp("MULI", (a, b) -> a * b, false);
        registerBinaryImmediateOp("DIVI", (a, b) -> {
            if (b == 0) throw new ArithmeticException("Division by zero");
            return a / b;
        }, false);
        registerBinaryImmediateOp("MODI", (a, b) -> {
            if (b == 0) throw new ArithmeticException("Modulo by zero");
            return a % b;
        }, false);

        register("INC", new UnaryRegisterInstruction("INC") {
            @Override
            protected int calculate(int value) {
                return value + 1;
            }
        });

        register("DEC", new UnaryRegisterInstruction("DEC") {
            @Override
            protected int calculate(int value) {
                return value - 1;
            }
        });
    }

    private void registerLogicalInstructions() {
        // Register-register operations
        registerBinaryOp("AND", (a, b) -> a & b, false);
        registerBinaryOp("OR", (a, b) -> a | b, false);
        registerBinaryOp("XOR", (a, b) -> a ^ b, false);

        // Register-immediate operations
        registerBinaryImmediateOp("ANDI", (a, b) -> a & b, false);
        registerBinaryImmediateOp("ORI", (a, b) -> a | b, false);
        registerBinaryImmediateOp("XORI", (a, b) -> a ^ b, false);

        register("NOT", new UnaryRegisterInstruction("NOT") {
            @Override
            protected int calculate(int value) {
                return ~value;
            }
        });
    }

    private void registerShiftInstructions() {
        register("SHL", createShiftInstruction("SHL", (a, s) -> a << s));
        register("SHR", createShiftInstruction("SHR", (a, s) -> a >>> s));
    }

    private void registerMemoryInstructions() {
        register("LOAD", createMemoryInstruction("LOAD", true));
        register("STORE", createMemoryInstruction("STORE", false));
    }

    private void registerControlFlowInstructions() {
        register("JMP", createJumpInstruction("JMP", cpu -> true));
        register("JZ", createJumpInstruction("JZ", cpu -> cpu.getFlags().isZero()));
        register("JNZ", createJumpInstruction("JNZ", cpu -> !cpu.getFlags().isZero()));

        register("CALL", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int addr = words[1];
                cpu.push(cpu.getProgramCounter());
                cpu.jump(addr);
            }

            @Override
            public int[] encode(String args) {
                int addr = parseImmediate(args);
                return new int[]{encodeInstruction(0, 0, getOpcode("CALL")), addr};
            }

            @Override
            public int getWordCount() { return 2; }
        });

        register("RET", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int retAddr = cpu.pop();
                cpu.jump(retAddr);
            }

            @Override
            public int[] encode(String args) {
                return new int[]{encodeInstruction(0, 0, getOpcode("RET"))};
            }
        });
    }

    private void registerStackInstructions() {
        register("PUSH", new StackInstruction("PUSH") {
            @Override
            public void executeOperation(CPU cpu, int value) {
                cpu.push(value);
            }
        });

        register("POP", new StackInstruction("POP") {
            @Override
            public void executeOperation(CPU cpu, int value) {
                cpu.setRegister(register, value);
                cpu.getFlags().update(value);
            }
        });
    }

    private void registerDataMovementInstructions() {
        register("LOADI", new ImmediateInstruction("LOADI") {
            @Override
            protected void executeImmediate(CPU cpu, int rDest, int immediate) {
                cpu.setRegister(rDest, immediate);
                cpu.getFlags().update(immediate);
            }
        });

        register("MOV", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int rSrc = InstructionUtils.extractRegister(words[0], 8);
                int val = cpu.getRegister(rSrc);
                cpu.setRegister(rDest, val);
                cpu.getFlags().update(val);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rDest = parseRegister(parts[0]);
                int rSrc = parseRegister(parts[1]);
                return new int[]{encodeInstruction(rDest, rSrc, getOpcode("MOV"))};
            }
        });

        register("CLR", new UnaryRegisterInstruction("CLR") {
            @Override
            protected int calculate(int value) {
                return 0;
            }
        });
    }

    private void registerComparisonInstructions() {
        // Register-register comparisons
        register("CMP", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rA = InstructionUtils.extractRegister(words[0], 16);
                int rB = InstructionUtils.extractRegister(words[0], 8);
                int a = cpu.getRegister(rA);
                int b = cpu.getRegister(rB);
                cpu.getFlags().updateSub(a, b, a - b);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rA = parseRegister(parts[0]);
                int rB = parseRegister(parts[1]);
                return new int[]{encodeInstruction(rA, rB, getOpcode("CMP"))};
            }
        });

        // Register-immediate comparison
        register("CMPI", new ImmediateInstruction("CMPI") {
            @Override
            protected void executeImmediate(CPU cpu, int rA, int immediate) {
                int a = cpu.getRegister(rA);
                cpu.getFlags().updateSub(a, immediate, a - immediate);
            }
        });

        // Register-register test
        register("TEST", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rA = InstructionUtils.extractRegister(words[0], 16);
                int rB = InstructionUtils.extractRegister(words[0], 8);
                int val = cpu.getRegister(rA) & cpu.getRegister(rB);
                cpu.getFlags().update(val);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rA = parseRegister(parts[0]);
                int rB = parseRegister(parts[1]);
                return new int[]{encodeInstruction(rA, rB, getOpcode("TEST"))};
            }
        });

        // Register-immediate test
        register("TESTI", new ImmediateInstruction("TESTI") {
            @Override
            protected void executeImmediate(CPU cpu, int rA, int immediate) {
                int val = cpu.getRegister(rA) & immediate;
                cpu.getFlags().update(val);
            }
        });
    }

    private void registerSystemInstructions() {
        register("SYSCALL", new Instruction() {
            public void execute(CPU cpu, int[] words) {
                int syscallNumber = cpu.getRegister(0);
                switch (syscallNumber) {
                    case 1:  // get VRAM info
                        cpu.setRegister(1, cpu.getMemoryMap().getVramStart());
                        cpu.setRegister(2, cpu.getMemoryMap().getVramSize());
                        cpu.getFlags().update(cpu.getRegister(1));
                        break;
                    default:
                        throw new IllegalStateException("Unknown syscall number: " + syscallNumber);
                }
            }

            public int[] encode(String args) {
                return new int[] { encodeInstruction(0, 0, getOpcode("SYSCALL")) };
            }
        });

        register("NOP", new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) { /* no op */ }

            @Override
            public int[] encode(String args) {
                return new int[]{encodeInstruction(0, 0, getOpcode("NOP"))};
            }
        });
    }

    // Helper classes for common instruction patterns
    private abstract class UnaryRegisterInstruction implements Instruction {
        private final String name;

        UnaryRegisterInstruction(String name) {
            this.name = name;
        }

        @Override
        public void execute(CPU cpu, int[] words) {
            int rDest = InstructionUtils.extractRegister(words[0], 16);
            int value = cpu.getRegister(rDest);
            int result = calculate(value);
            cpu.setRegister(rDest, result);
            cpu.getFlags().update(result);
        }

        @Override
        public int[] encode(String args) {
            int rDest = parseRegister(args);
            return new int[]{encodeInstruction(rDest, 0, getOpcode(name))};
        }

        protected abstract int calculate(int value);
    }

    private abstract class ImmediateInstruction implements Instruction {
        private final String name;

        ImmediateInstruction(String name) {
            this.name = name;
        }

        @Override
        public void execute(CPU cpu, int[] words) {
            int rDest = InstructionUtils.extractRegister(words[0], 16);
            executeImmediate(cpu, rDest, words[1]);
        }

        @Override
        public int[] encode(String args) {
            String[] parts = splitArgs(args, 2);
            int rDest = parseRegister(parts[0]);
            int imm = parseImmediate(parts[1]);
            return new int[]{
                    encodeInstruction(rDest, 0, getOpcode(name)),
                    imm
            };
        }

        @Override
        public int getWordCount() {
            return 2;
        }

        protected abstract void executeImmediate(CPU cpu, int rDest, int immediate);
    }

    private abstract class StackInstruction implements Instruction {
        protected int register;
        private final String name;

        StackInstruction(String name) {
            this.name = name;
        }

        @Override
        public void execute(CPU cpu, int[] words) {
            register = InstructionUtils.extractRegister(words[0], 16);
            int value = name.equals("PUSH") ?
                    cpu.getRegister(register) : cpu.pop();
            executeOperation(cpu, value);
        }

        @Override
        public int[] encode(String args) {
            int reg = parseRegister(args);
            return new int[]{encodeInstruction(reg, 0, getOpcode(name))};
        }

        public abstract void executeOperation(CPU cpu, int value);
    }

    // Helper methods for instruction creation
    private Instruction createJumpInstruction(String name, Predicate<CPU> condition) {
        return new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                if (condition.test(cpu)) cpu.jump(words[1]);
            }

            @Override
            public int[] encode(String args) {
                int addr = parseImmediate(args);
                return new int[]{
                        encodeInstruction(0, 0, getOpcode(name)),
                        addr
                };
            }

            @Override
            public int getWordCount() {
                return 2;
            }
        };
    }

    private Instruction createMemoryInstruction(String name, boolean load) {
        return new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rA = InstructionUtils.extractRegister(words[0], 16);
                int rB = InstructionUtils.extractRegister(words[0], 8);
                if (load) {
                    int value = cpu.getMemory().readWord(cpu.getRegister(rB));
                    cpu.setRegister(rA, value);
                    cpu.getFlags().update(value);
                } else {
                    cpu.getMemory().writeWord(cpu.getRegister(rB), cpu.getRegister(rA));
                }
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rA = parseRegister(parts[0]);
                int rB = parseRegister(parts[1]);
                return new int[]{encodeInstruction(rA, rB, getOpcode(name))};
            }
        };
    }

    private Instruction createShiftInstruction(String name, BiFunction<Integer, Integer, Integer> op) {
        return new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int shift = InstructionUtils.extractRegister(words[0], 8);
                int result = op.apply(cpu.getRegister(rDest), shift);
                cpu.setRegister(rDest, result);
                cpu.getFlags().update(result);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rDest = parseRegister(parts[0]);
                int shift = parseImmediate(parts[1]);
                return new int[]{encodeInstruction(rDest, shift, getOpcode(name))};
            }
        };
    }

    private void registerBinaryOp(String name, BiFunction<Integer, Integer, Integer> op, boolean updateAddFlags) {
        register(name, new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int rSrc = InstructionUtils.extractRegister(words[0], 8);
                int a = cpu.getRegister(rDest);
                int b = cpu.getRegister(rSrc);
                int result = op.apply(a, b);
                cpu.setRegister(rDest, result);
                if (updateAddFlags) cpu.getFlags().updateAdd(a, b, result);
                else cpu.getFlags().update(result);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rDest = parseRegister(parts[0]);
                int rSrc = parseRegister(parts[1]);
                return new int[]{encodeInstruction(rDest, rSrc, getOpcode(name))};
            }
        });
    }

    private void registerBinaryImmediateOp(String name, BiFunction<Integer, Integer, Integer> op, boolean updateAddFlags) {
        register(name, new Instruction() {
            @Override
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int a = cpu.getRegister(rDest);
                int b = words[1];
                int result = op.apply(a, b);
                cpu.setRegister(rDest, result);
                if (updateAddFlags) cpu.getFlags().updateAdd(a, b, result);
                else cpu.getFlags().update(result);
            }

            @Override
            public int[] encode(String args) {
                String[] parts = splitArgs(args, 2);
                int rDest = parseRegister(parts[0]);
                int imm = parseImmediate(parts[1]);
                return new int[]{
                        encodeInstruction(rDest, 0, getOpcode(name)),
                        imm
                };
            }

            @Override
            public int getWordCount() {
                return 2;
            }
        });
    }

    // Utility methods
    private int parseImmediate(String token) {
        token = token.trim().toLowerCase();
        if (token.startsWith("0x")) {
            return (int) Long.parseLong(token.substring(2), 16);
        }
        return Integer.parseInt(token);
    }

    private int parseRegister(String token) {
        token = token.trim().toLowerCase();
        if (!token.startsWith("r")) {
            throw new IllegalArgumentException("Invalid register: " + token);
        }
        return Integer.parseInt(token.substring(1));
    }

    @Override
    public Instruction getInstruction(int instructionWord) {
        byte opcode = decodeOpcode(instructionWord);
        return instructionMap.get(opcode);
    }

    @Override
    public InstructionSet register(String name, Instruction instruction) {
        byte opcode = nextOpcode++;
        instructionMap.put(opcode, instruction);
        nameToOpcode.put(name, opcode);
        opcodeToName.put(opcode, name);
        return this;
    }

    @Override
    public Byte getOpcode(String name) {
        return nameToOpcode.get(name);
    }

    @Override
    public String getName(Byte opcode) {
        return opcodeToName.get(opcode);
    }

    @Override
    public Set<String> getInstructionNames() {
        return Collections.unmodifiableSet(nameToOpcode.keySet());
    }

    private byte decodeOpcode(int instructionWord) {
        return (byte) (instructionWord & 0xFF);
    }

    private int encodeInstruction(int rDest, int rSrc, int opcode) {
        return (rDest << 16) | (rSrc << 8) | (opcode & 0xFF);
    }

    private void logInstructions() {
        System.out.println("Registered instructions:");
        nameToOpcode.forEach((name, opcode) ->
                System.out.printf("  %s -> 0x%02X%n", name, opcode)
        );
    }
}