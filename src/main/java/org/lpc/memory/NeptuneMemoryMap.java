package org.lpc.memory;

public class NeptuneMemoryMap implements MemoryMap {
    private static final int BOOT_ROM_START = 0x0000;
    private static final int BOOT_ROM_SIZE = 0x1000;    // 4KB

    private static final int RAM_START = BOOT_ROM_START + BOOT_ROM_SIZE;  // 0x1000
    private static final int RAM_SIZE = 0x8000;         // 32KB total RAM region

    // Hardcoded VRAM properties:
    private static final int VRAM_WIDTH = 128;          // width in pixels
    private static final int VRAM_HEIGHT = 128;          // height in pixels
    private static final VramFormat VRAM_FORMAT = VramFormat.RGBA32; // 4 bytes per pixel

    private static final int VRAM_SIZE = VRAM_WIDTH * VRAM_HEIGHT * bytesPerPixel(VRAM_FORMAT);

    private static final int VRAM_START = RAM_START + RAM_SIZE;           // right after RAM

    private static final int IO_START = VRAM_START + VRAM_SIZE;           // right after VRAM
    private static final int IO_SIZE = 0x1000;          // 4KB

    // Stack and heap inside RAM
    private static final int STACK_START = RAM_START + RAM_SIZE - 4;       // stack grows down from end of RAM
    private static final int HEAP_START = RAM_START + 0x2000;              // heap starts 8KB into RAM (adjust as needed)
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
        return RAM_START; // program loaded at start of RAM
    }

    @Override
    public int getTotalMemorySize() {
        return IO_START + IO_SIZE; // total size up to end of IO
    }

    @Override
    public VramFormat getVramFormat() {
        return VRAM_FORMAT;
    }

    @Override
    public int getVramWidth() {
        return VRAM_WIDTH;
    }

    @Override
    public int getVramHeight() {
        return VRAM_HEIGHT;
    }

    private static int bytesPerPixel(VramFormat format) {
        return switch (format) {
            case RGBA32 -> 4;
            // add other formats here if needed
            default -> throw new IllegalArgumentException("Unsupported VRAM format: " + format);
        };
    }
}
