package org.lpc.memory;

public interface MemoryHandler {
    int readWord(int addr);
    void writeWord(int addr, int val);
    byte readByte(int addr);
    void writeByte(int addr, byte val);
    int getBaseAddress();
    int getSize();
}
