package org.lpc.memory;

public interface MemoryMap {
    int getBootRomStart();
    int getBootRomSize();
    int getSyscallTableStart();
    int getSyscallTableSize();
    int getSyscallCodeStart();
    int getSyscallCodeSize();

    int getRamStart();
    int getRamSize();
    int getVramStart();
    int getVramSize();
    VramFormat getVramFormat();
    int getVramWidth();
    int getVramHeight();

    int getIoStart();
    int getIoSize();

    int getStackStart();
    int getProgramStart();
    int getTotalMemorySize();
    int getHeapStart();
    int getHeapSize();

    enum VramFormat {
        RAW_BYTES,
        RGBA32,
        RGB565,
        INDEXED_8BIT,
        TILE_MAP
    }
}
