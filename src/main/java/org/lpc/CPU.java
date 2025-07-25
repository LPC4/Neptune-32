package org.lpc;

import lombok.Getter;
import lombok.Setter;
import org.lpc.instructions.Instruction;
import org.lpc.instructions.InstructionSet;
import org.lpc.memory.Flags;
import org.lpc.memory.MemoryBus;
import org.lpc.memory.MemoryMap;

@Getter
@Setter
public class CPU {
    private final int[] registers;

    private int programCounter;
    private int stackPointer;
    private int heapPointer;
    private boolean halt;

    private final Flags flags;
    private final MemoryBus memory;
    private final MemoryMap memoryMap;
    private final InstructionSet instructionSet;

    public CPU(InstructionSet instructionSet, MemoryMap memoryMap, int registers) {
        this.instructionSet = instructionSet;
        this.memoryMap = memoryMap;
        this.flags = new Flags();
        this.memory = new MemoryBus(memoryMap);
        this.halt = false;
        this.registers = new int[registers];

        this.programCounter = memoryMap.getProgramStart();
        this.stackPointer = memoryMap.getStackStart();
        this.heapPointer = memoryMap.getHeapStart();
    }

    // -------- Registers --------
    public int getRegister(int index) {
        validateRegisterIndex(index);
        return switch (index) {
            case 252 -> programCounter;
            case 253 -> stackPointer;
            case 254 -> heapPointer;
            default -> {
                if (index >= 0 && index < registers.length) {
                    yield registers[index];
                }
                throw new IllegalArgumentException("Wrong register index: " + index);
            }
        };
    }

    public void setRegister(int index, int value) {
        validateRegisterIndex(index);
        switch (index) {
            case 252 -> programCounter = value;
            case 253 -> stackPointer = value;
            case 254 -> heapPointer = value;
            default -> {
                if (index >= 0 && index < registers.length) {
                    registers[index] = value;
                } else {
                    throw new IllegalArgumentException("Wrong register index: " + index);
                }
            }
        };
    }

    private void validateRegisterIndex(int index) {
        if (index < 0 || index >= registers.length && index < 252 || index > 254) {
            throw new IllegalArgumentException("Invalid register index: " + index);
        }
    }

    // -------- Stack (grows downward) --------
    public void push(int value) {
        stackPointer -= 4;
        ensureHeapStackNoCollision();
        memory.writeWord(stackPointer, value);
    }

    public int pop() {
        int value = memory.readWord(stackPointer);
        stackPointer += 4;
        return value;
    }

    // -------- Heap (simple bump allocator) --------
    public int allocateHeap(int size) {
        int alignedSize = (size + 3) & ~3;
        int nextHeap = heapPointer + alignedSize;
        if (nextHeap >= stackPointer) throw new IllegalStateException("Heap/stack collision");
        int allocAddr = heapPointer;
        heapPointer = nextHeap;
        return allocAddr;
    }

    public void setHeapPointer(int ptr) {
        if (ptr < memoryMap.getHeapStart() || ptr >= stackPointer) {
            throw new IllegalArgumentException("Invalid heap pointer");
        }
        heapPointer = ptr;
    }

    // -------- Program Counter --------
    public void jump(int address) {
        programCounter = address;
    }

    public void advancePC(int bytes) {
        programCounter += bytes;
    }

    public int fetchWord() {
        int word = memory.readWord(programCounter);
        advancePC(4);
        return word;
    }

    public int peekWord() {
        return memory.readWord(programCounter);
    }

    // -------- Instruction Execution --------
    public void step() {
        int firstWord = fetchWord();
        Instruction instr = instructionSet.getInstruction(firstWord);
        int[] words = new int[instr.getWordCount()];
        words[0] = firstWord;
        for (int i = 1; i < words.length; i++) {
            words[i] = fetchWord();
        }
        instr.execute(this, words);
    }

    // -------- Safety Check --------
    private void ensureHeapStackNoCollision() {
        if (heapPointer >= stackPointer) {
            throw new IllegalStateException("Heap and stack collided");
        }
    }
}
