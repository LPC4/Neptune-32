package org.lpc.memory.io;

import java.util.Map;

/**
 * ConsoleOutputDevice simulates a memory-mapped console output device.
 * Writing a character to the OUTPUT register prints it to standard output.
 *
 * Registers:
 * - OUTPUT_PRINT (offset 0): writing a byte here outputs that character.
 */
public class ConsoleOutputDevice implements IODevice {
    public static final int SIZE = 4;
    public static final int OFFSET_OUTPUT_PRINT = 0;

    private final int baseAddress;
    private final int[] registers = new int[SIZE / 4];

    public ConsoleOutputDevice(int baseAddress) {
        this.baseAddress = baseAddress;
    }

    @Override
    public boolean handles(int address) {
        return address >= baseAddress && address < baseAddress + SIZE;
    }

    @Override
    public int readWord(int address) {
        validateAddress(address);
        int offset = address - baseAddress;
        int registerIndex = offset / 4;

        return switch (offset) {
            case OFFSET_OUTPUT_PRINT -> registers[registerIndex];
            default -> 0;
        };
    }

    @Override
    public void writeWord(int address, int value) {
        validateAddress(address);
        int offset = address - baseAddress;

        if (offset == OFFSET_OUTPUT_PRINT) {
            outputCharacter(value);
            registers[0] = value;
        }
    }

    @Override
    public byte readByte(int address) {
        validateAddress(address);
        int alignedAddress = address & ~3;
        int byteOffset = address & 3;
        int word = readWord(alignedAddress);
        return (byte) ((word >> (byteOffset * 8)) & 0xFF);
    }

    @Override
    public void writeByte(int address, byte value) {
        validateAddress(address);
        outputCharacter(value);
        int alignedAddress = address & ~3;
        int registerIndex = (alignedAddress - baseAddress) / 4;
        registers[registerIndex] = value;
    }

    @Override
    public int getBaseAddress() {
        return baseAddress;
    }

    @Override
    public int getSize() {
        return SIZE;
    }

    @Override
    public String getDescription() {
        return "Console Output Device: prints characters to standard output";
    }

    @Override
    public Map<Integer, String> getOffsetNames() {
        return Map.of(
                OFFSET_OUTPUT_PRINT, "OUTPUT_PRINT"
        );
    }

    private void outputCharacter(int value) {
        System.out.print((char) (value & 0xFF));
        System.out.flush();
    }

    private void validateAddress(int address) {
        if (!handles(address)) {
            throw new IllegalArgumentException(String.format(
                    "Address 0x%08X out of bounds for ConsoleOutputDevice at 0x%08X - 0x%08X",
                    address, baseAddress, baseAddress + SIZE - 1));
        }
    }
}
