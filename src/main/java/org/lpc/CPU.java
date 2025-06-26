package org.lpc;

import lombok.Getter;
import lombok.Setter;
import org.lpc.instructions.Instruction;
import org.lpc.instructions.InstructionSet;
import org.lpc.instructions.InstructionUtils;
import org.lpc.memory.Flags;
import org.lpc.memory.MemoryBus;
import org.lpc.memory.MemoryMap;

@Getter
@Setter
public class CPU {
    private final int[] registers = new int[16];

    private int programCounter;
    private int stackPointer;
    private int heapPointer;
    private boolean halt;

    private final Flags flags;
    private final MemoryBus memory;
    private final MemoryMap memoryMap;
    private final InstructionSet instructionSet;

    public CPU(InstructionSet instructionSet, MemoryMap memoryMap) {
        this.instructionSet = instructionSet;
        this.memoryMap = memoryMap;
        this.flags = new Flags();
        this.memory = new MemoryBus(memoryMap);
        this.halt = false;

        this.programCounter = memoryMap.getProgramStart();
        this.stackPointer = memoryMap.getStackStart();
        this.heapPointer = memoryMap.getHeapStart();
    }

    // -------- Registers --------
    public int getRegister(int index) {
        validateRegisterIndex(index);
        return registers[index];
    }

    public void setRegister(int index, int value) {
        validateRegisterIndex(index);
        registers[index] = value;
    }

    private void validateRegisterIndex(int index) {
        if (index < 0 || index >= registers.length) {
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
        System.out.println(instructionSet.getName(InstructionUtils.extractOpcode(firstWord)));
    }

    // -------- Safety Check --------
    private void ensureHeapStackNoCollision() {
        if (heapPointer >= stackPointer) {
            throw new IllegalStateException("Heap and stack collided");
        }
    }
}
