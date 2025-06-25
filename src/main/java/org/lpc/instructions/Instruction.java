package org.lpc.instructions;

import org.lpc.CPU;

public interface Instruction {
    void execute(CPU cpu, int[] words);
    int[] encode(String args);
    default int getWordCount() {
        return 1;
    }
}

