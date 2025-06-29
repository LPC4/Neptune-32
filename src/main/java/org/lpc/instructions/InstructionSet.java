package org.lpc.instructions;

import java.util.Set;

public interface InstructionSet {
    Instruction getInstruction(int instructionWord);

    void register(String name, Instruction instruction);

    Byte getOpcode(String name);

    String getName(Byte opcode);
}
