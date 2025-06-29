package org.lpc.memory;

import lombok.Getter;

public class Memory implements MemoryHandler {
    private final byte[] data;
    @Getter
    private final int baseAddress;

    public Memory(int baseAddress, int size) {
        this.baseAddress = baseAddress;
        this.data = new byte[size];
    }

    private int toOffset(int addr) {
        int offset = addr - baseAddress;
        if (offset < 0 || offset >= data.length) {
            throw new IndexOutOfBoundsException("Address 0x" + Integer.toHexString(addr) + " out of range");
        }
        return offset;
    }

    public int getSize() {
        return data.length;
    }

    public int readWord(int addr) {
        int offset = toOffset(addr);
        return (data[offset] & 0xFF) |
                ((data[offset + 1] & 0xFF) << 8) |
                ((data[offset + 2] & 0xFF) << 16) |
                ((data[offset + 3] & 0xFF) << 24);
    }

    public void writeWord(int addr, int value) {
        int offset = toOffset(addr);
        data[offset] = (byte) value;
        data[offset + 1] = (byte) (value >>> 8);
        data[offset + 2] = (byte) (value >>> 16);
        data[offset + 3] = (byte) (value >>> 24);
    }


    public byte readByte(int addr) {
        return data[toOffset(addr)];
    }

    public void writeByte(int addr, byte val) {
        data[toOffset(addr)] = val;
    }
}
