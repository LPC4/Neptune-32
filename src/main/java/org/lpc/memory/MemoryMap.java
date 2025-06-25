package org.lpc.memory;

public interface MemoryMap {
    int getBootRomStart();
    int getBootRomSize();
    int getRamStart();
    int getRamSize();
    int getVramStart();
    int getVramSize();
    int getIoStart();
    int getIoSize();


    int getStackStart();
    int getProgramStart();
    int getTotalMemorySize();
    int getHeapStart();
    int getHeapSize();
}
