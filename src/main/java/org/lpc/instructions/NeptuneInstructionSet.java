package org.lpc.instructions;

import org.lpc.util.InstructionUtils;

import java.util.HashMap;
import java.util.Map;

public class InstructionSet {
    public static final byte ADD = 1;
    public static final byte SUB = 2;
    public static final byte LOAD_IMMEDIATE = 3;
    public static final byte JMP = 4;
    public static final byte MUL = 5;
    public static final byte DIV = 6;
    public static final byte MOD = 7;
    public static final byte AND = 8;
    public static final byte OR  = 9;
    public static final byte XOR = 10;
    public static final byte NOT = 11;
    public static final byte SHL = 12;
    public static final byte SHR = 13;
    public static final byte LOAD = 14;
    public static final byte STORE = 15;
    public static final byte JZ = 16;
    public static final byte JNZ = 17;
    public static final byte PUSH = 18;
    public static final byte POP = 19;
    public static final byte CMP = 20;

    private final Map<Byte, Instruction> instructionMap = new HashMap<>();

    public InstructionSet() {
        // Arithmetic
        register(ADD, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int rSrc = InstructionUtils.extractRegister(word, 8);
            int a = cpu.getRegister(rDest);
            int b = cpu.getRegister(rSrc);
            int result = a + b;
            cpu.setRegister(rDest, result);
            cpu.getFlags().updateAdd(a, b, result);
        });

        register(SUB, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int rSrc = InstructionUtils.extractRegister(word, 8);
            int a = cpu.getRegister(rDest);
            int b = cpu.getRegister(rSrc);
            int result = a - b;
            cpu.setRegister(rDest, result);
            cpu.getFlags().updateSub(a, b, result);
        });

        register(MUL, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int rSrc = InstructionUtils.extractRegister(word, 8);
            int result = cpu.getRegister(rDest) * cpu.getRegister(rSrc);
            cpu.setRegister(rDest, result);
            cpu.getFlags().update(result);
        });

        register(DIV, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int rSrc = InstructionUtils.extractRegister(word, 8);
            int divisor = cpu.getRegister(rSrc);
            if (divisor != 0) {
                int result = cpu.getRegister(rDest) / divisor;
                cpu.setRegister(rDest, result);
                cpu.getFlags().update(result);
            } else {
                throw new ArithmeticException("Division by zero");
            }
        });

        register(MOD, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int rSrc = InstructionUtils.extractRegister(word, 8);
            int divisor = cpu.getRegister(rSrc);
            if (divisor != 0) {
                int result = cpu.getRegister(rDest) % divisor;
                cpu.setRegister(rDest, result);
                cpu.getFlags().update(result);
            } else {
                throw new ArithmeticException("Modulo by zero");
            }
        });

        // Bitwise
        register(AND, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int rSrc = InstructionUtils.extractRegister(word, 8);
            int result = cpu.getRegister(rDest) & cpu.getRegister(rSrc);
            cpu.setRegister(rDest, result);
            cpu.getFlags().update(result);
        });

        register(OR, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int rSrc = InstructionUtils.extractRegister(word, 8);
            int result = cpu.getRegister(rDest) | cpu.getRegister(rSrc);
            cpu.setRegister(rDest, result);
            cpu.getFlags().update(result);
        });

        register(XOR, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int rSrc = InstructionUtils.extractRegister(word, 8);
            int result = cpu.getRegister(rDest) ^ cpu.getRegister(rSrc);
            cpu.setRegister(rDest, result);
            cpu.getFlags().update(result);
        });

        register(NOT, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int result = ~cpu.getRegister(rDest);
            cpu.setRegister(rDest, result);
            cpu.getFlags().update(result);
        });

        register(SHL, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int shift = InstructionUtils.extractRegister(word, 8);
            int result = cpu.getRegister(rDest) << shift;
            cpu.setRegister(rDest, result);
            cpu.getFlags().update(result);
        });

        register(SHR, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int shift = InstructionUtils.extractRegister(word, 8);
            int result = cpu.getRegister(rDest) >>> shift;
            cpu.setRegister(rDest, result);
            cpu.getFlags().update(result);
        });

        // Memory
        register(LOAD, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int rAddr = InstructionUtils.extractRegister(word, 8);
            int value = cpu.getMemory().readWord(cpu.getRegister(rAddr));
            cpu.setRegister(rDest, value);
            cpu.getFlags().update(value);
        });

        register(STORE, (cpu, word) -> {
            int rSrc = InstructionUtils.extractRegister(word, 16);
            int rAddr = InstructionUtils.extractRegister(word, 8);
            cpu.getMemory().writeWord(cpu.getRegister(rAddr), cpu.getRegister(rSrc));
        });

        // Control Flow
        register(JMP, (cpu, word) -> {
            int address = InstructionUtils.extractBits(word, 0, 24);
            cpu.jump(address);
        });

        register(JZ, (cpu, word) -> {
            int address = InstructionUtils.extractBits(word, 0, 24);
            if (cpu.getFlags().isZero()) {
                cpu.jump(address);
            }
        });

        register(JNZ, (cpu, word) -> {
            int address = InstructionUtils.extractBits(word, 0, 24);
            if (!cpu.getFlags().isZero()) {
                cpu.jump(address);
            }
        });

        // Stack
        register(PUSH, (cpu, word) -> {
            int rSrc = InstructionUtils.extractRegister(word, 16);
            cpu.push(cpu.getRegister(rSrc));
        });

        register(POP, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int val = cpu.pop();
            cpu.setRegister(rDest, val);
            cpu.getFlags().update(val);
        });

        // Compare
        register(CMP, (cpu, word) -> {
            int rA = InstructionUtils.extractRegister(word, 16);
            int rB = InstructionUtils.extractRegister(word, 8);
            int result = cpu.getRegister(rA) - cpu.getRegister(rB);
            cpu.getFlags().updateSub(cpu.getRegister(rA), cpu.getRegister(rB), result);
        });

        // Immediate value loader
        register(LOAD_IMMEDIATE, (cpu, word) -> {
            int rDest = InstructionUtils.extractRegister(word, 16);
            int imm = InstructionUtils.extractRegister(word, 0);
            cpu.setRegister(rDest, imm);
            cpu.getFlags().update(imm);
        });
    }

    public InstructionSet register(byte opcode, Instruction instruction) {
        instructionMap.put(opcode, instruction);
        return this;
    }

    public Instruction getInstruction(int instructionWord) {
        byte opcode = decodeOpcode(instructionWord);
        return instructionMap.get(opcode);
    }

    private byte decodeOpcode(int instructionWord) {
        return (byte) (instructionWord & 0xFF);
    }
}
