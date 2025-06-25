package org.lpc.instructions;

import org.lpc.CPU;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;

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
        registerBinaryOp("AND", (a, b) -> a & b, false);
        registerBinaryOp("OR", (a, b) -> a | b, false);
        registerBinaryOp("XOR", (a, b) -> a ^ b, false);

        register("NOT", new Instruction() {
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int result = ~cpu.getRegister(rDest);
                cpu.setRegister(rDest, result);
                cpu.getFlags().update(result);
            }

            public int[] encode(String args) {
                int rDest = parseRegister(args.trim());
                return new int[]{encodeInstruction(rDest, 0, getOpcode("NOT"))};
            }
        });

        register("SHL", createShiftInstruction("SHL", (a, s) -> a << s));
        register("SHR", createShiftInstruction("SHR", (a, s) -> a >>> s));

        register("LOAD", createMemoryInstruction("LOAD", true));
        register("STORE", createMemoryInstruction("STORE", false));

        register("JMP", createJumpInstruction("JMP", cpu -> true));
        register("JZ", createJumpInstruction("JZ", cpu -> cpu.getFlags().isZero()));
        register("JNZ", createJumpInstruction("JNZ", cpu -> !cpu.getFlags().isZero()));

        register("PUSH", new Instruction() {
            public void execute(CPU cpu, int[] words) {
                int rSrc = InstructionUtils.extractRegister(words[0], 16);
                cpu.push(cpu.getRegister(rSrc));
            }

            public int[] encode(String args) {
                int rSrc = parseRegister(args.trim());
                return new int[]{encodeInstruction(rSrc, 0, getOpcode("PUSH"))};
            }
        });

        register("POP", new Instruction() {
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int val = cpu.pop();
                cpu.setRegister(rDest, val);
                cpu.getFlags().update(val);
            }

            public int[] encode(String args) {
                int rDest = parseRegister(args.trim());
                return new int[]{encodeInstruction(rDest, 0, getOpcode("POP"))};
            }
        });

        register("CMP", new Instruction() {
            public void execute(CPU cpu, int[] words) {
                int rA = InstructionUtils.extractRegister(words[0], 16);
                int rB = InstructionUtils.extractRegister(words[0], 8);
                int a = cpu.getRegister(rA);
                int b = cpu.getRegister(rB);
                cpu.getFlags().updateSub(a, b, a - b);
            }

            public int[] encode(String args) {
                String[] parts = args.split(",");
                int rA = parseRegister(parts[0]);
                int rB = parseRegister(parts[1]);
                return new int[]{encodeInstruction(rA, rB, getOpcode("CMP"))};
            }
        });

        register("LOAD_I", new Instruction() {
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                cpu.setRegister(rDest, words[1]);
                cpu.getFlags().update(words[1]);
            }

            public int[] encode(String args) {
                String[] parts = args.split(",");
                int rDest = parseRegister(parts[0].trim());
                int imm = parseImmediate(parts[1].trim());
                return new int[]{
                        encodeInstruction(rDest, 0, getOpcode("LOAD_I")),
                        imm
                };
            }

            public int getWordCount() {
                return 2;
            }
        });
    }

    private Instruction createJumpInstruction(String name, Predicate<CPU> condition) {
        return new Instruction() {
            public void execute(CPU cpu, int[] words) {
                if (condition.test(cpu)) cpu.jump(words[1]);
            }

            public int[] encode(String args) {
                int addr = parseImmediate(args.trim());
                return new int[]{
                        encodeInstruction(0, 0, getOpcode(name)),
                        addr
                };
            }

            public int getWordCount() {
                return 2;
            }
        };
    }

    private Instruction createMemoryInstruction(String name, boolean load) {
        return new Instruction() {
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

            public int[] encode(String args) {
                String[] parts = args.split(",");
                int rA = parseRegister(parts[0]);
                int rB = parseRegister(parts[1]);
                return new int[]{encodeInstruction(rA, rB, getOpcode(name))};
            }
        };
    }

    private Instruction createShiftInstruction(String name, BiFunction<Integer, Integer, Integer> op) {
        return new Instruction() {
            public void execute(CPU cpu, int[] words) {
                int rDest = InstructionUtils.extractRegister(words[0], 16);
                int shift = InstructionUtils.extractRegister(words[0], 8);
                int result = op.apply(cpu.getRegister(rDest), shift);
                cpu.setRegister(rDest, result);
                cpu.getFlags().update(result);
            }

            public int[] encode(String args) {
                String[] parts = args.split(",");
                int rDest = parseRegister(parts[0]);
                int shift = parseImmediate(parts[1]);
                return new int[]{encodeInstruction(rDest, shift, getOpcode(name))};
            }
        };
    }

    private void registerBinaryOp(String name, BiFunction<Integer, Integer, Integer> op, boolean updateAddFlags) {
        register(name, new Instruction() {
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

            public int[] encode(String args) {
                String[] parts = args.split(",");
                int rDest = parseRegister(parts[0]);
                int rSrc = parseRegister(parts[1]);
                return new int[]{encodeInstruction(rDest, rSrc, getOpcode(name))};
            }
        });
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

    private int parseRegister(String token) {
        token = token.trim().toLowerCase();
        if (!token.startsWith("r")) throw new IllegalArgumentException("Invalid register: " + token);
        return Integer.parseInt(token.substring(1));
    }

    private int parseImmediate(String token) {
        token = token.trim().toLowerCase();
        if (token.startsWith("0x")) {
            return Integer.parseInt(token.substring(2), 16);
        }
        return Integer.parseInt(token);
    }

    private void logInstructions() {
        System.out.println("Registered instructions:");
        nameToOpcode.forEach((name, opcode) ->
                System.out.printf("  %s -> 0x%02X%n", name, opcode)
        );
    }
}
