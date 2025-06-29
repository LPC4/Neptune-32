package org.lpc.instructions;

public final class InstructionUtils {
    private InstructionUtils() {} // prevent instantiation

    public static int extractBits(int word, int startBit, int length) {
        int mask = (1 << length) - 1;
        return (word >>> startBit) & mask;
    }

    public static byte decodeOpcode(int instructionWord) {
        return (byte) (instructionWord & 0xFF);
    }

    public static int encodeInstruction(int rDest, int rSrc, int opcode) {
        return (rDest << 16) | (rSrc << 8) | (opcode & 0xFF);
    }

    public static int extractRegister(int word, int bitOffset) {
        return extractBits(word, bitOffset, 8);
    }
}
