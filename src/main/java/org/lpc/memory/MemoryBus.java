package org.lpc.memory;

import lombok.Getter;

@Getter
public class MemoryBus {
    private final Memory rom;
    private final Memory ram;
    private final Memory vram;
    private final Memory io;

    public MemoryBus(MemoryMap map) {
        rom = new Memory(map.getBootRomStart(), map.getBootRomSize());
        ram = new Memory(map.getRamStart(), map.getRamSize());
        vram = new Memory(map.getVramStart(), map.getVramSize());
        io = new Memory(map.getIoStart(), map.getIoSize());
    }

    public byte readByte(int addr) {
        if (inRange(addr, rom)) return rom.readByte(addr);
        if (inRange(addr, ram)) return ram.readByte(addr);
        if (inRange(addr, vram)) return vram.readByte(addr);
        if (inRange(addr, io)) return io.readByte(addr);
        throw new IllegalArgumentException(String.format("Invalid memory read at 0x%08X", addr));
    }

    public void writeByte(int addr, byte val) {
        if (inRange(addr, rom)) throw new UnsupportedOperationException(
                String.format("Cannot write to ROM at 0x%08X", addr)
        );
        if (inRange(addr, ram)) {
            ram.writeByte(addr, val);
            return;
        }
        if (inRange(addr, vram)) {
            vram.writeByte(addr, val);
            return;
        }
        if (inRange(addr, io)) {
            io.writeByte(addr, val);
            return;
        }
        throw new IllegalArgumentException(String.format("Invalid memory write at 0x%08X", addr));
    }

    public int readWord(int addr) {
        if (inRange(addr, rom)) return rom.readWord(addr);
        if (inRange(addr, ram)) return ram.readWord(addr);
        if (inRange(addr, vram)) return vram.readWord(addr);
        if (inRange(addr, io)) return io.readWord(addr);
        throw new IllegalArgumentException(String.format("Invalid memory readWord at 0x%08X", addr));
    }

    public void writeWord(int addr, int val) {
        if (inRange(addr, rom)) throw new UnsupportedOperationException(
                String.format("Cannot write to ROM at 0x%08X", addr)
        );
        if (inRange(addr, ram)) {
            ram.writeWord(addr, val);
            return;
        }
        if (inRange(addr, vram)) {
            vram.writeWord(addr, val);
            return;
        }
        if (inRange(addr, io)) {
            io.writeWord(addr, val);
            return;
        }
        throw new IllegalArgumentException(String.format("Invalid memory writeWord at 0x%08X", addr));
    }

    private boolean inRange(int addr, Memory mem) {
        return addr >= mem.getBaseAddress() && addr < mem.getBaseAddress() + mem.getSize();
    }

}
