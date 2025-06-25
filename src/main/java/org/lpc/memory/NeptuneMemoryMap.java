package org.lpc.memory;

public class NeptuneMemoryMap implements MemoryMap {
    private static final int BOOT_ROM_START = 0x0000;
    private static final int BOOT_ROM_SIZE = 0x1000;    // 4KB

    private static final int RAM_START = 0x1000;
    private static final int RAM_SIZE = 0x8000;         // 32KB total RAM region

    private static final int VRAM_START = 0x9000;
    private static final int VRAM_SIZE = 0x2000;        // 8KB

    private static final int IO_START = 0xB000;
    private static final int IO_SIZE = 0x1000;          // 4KB

    private static final int HEAP_START = 0x3000;       // Deeper in RAM

    private static final int STACK_START = RAM_START + RAM_SIZE - 4;
    private static final int HEAP_SIZE = STACK_START - HEAP_START;

    @Override
    public int getBootRomStart() {
        return BOOT_ROM_START;
    }

    @Override
    public int getBootRomSize() {
        return BOOT_ROM_SIZE;
    }

    @Override
    public int getRamStart() {
        return RAM_START;
    }

    @Override
    public int getRamSize() {
        return RAM_SIZE;
    }

    @Override
    public int getVramStart() {
        return VRAM_START;
    }

    @Override
    public int getVramSize() {
        return VRAM_SIZE;
    }

    @Override
    public int getIoStart() {
        return IO_START;
    }

    @Override
    public int getIoSize() {
        return IO_SIZE;
    }

    @Override
    public int getHeapStart() {
        return HEAP_START;
    }

    @Override
    public int getHeapSize() {
        return HEAP_SIZE;
    }

    @Override
    public int getStackStart() {
        return STACK_START;
    }

    @Override
    public int getProgramStart() {
        return RAM_START; // Typically, program code loaded at start of RAM
    }

    @Override
    public int getTotalMemorySize() {
        // Total addressable memory up to end of IO region
        return IO_START + IO_SIZE;
    }
}
