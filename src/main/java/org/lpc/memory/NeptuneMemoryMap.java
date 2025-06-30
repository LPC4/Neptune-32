package org.lpc.memory;

import org.lpc.visualization.debug.TablePrinter;

public class NeptuneMemoryMap implements MemoryMap {
    // ROM
    private static final int BOOT_ROM_START = 0;
    private static final int BOOT_ROM_SIZE = 8 * 1024; // 8 KB

    // Syscall Table & Syscall Code (inside ROM)
    private static final int SYSCALL_TABLE_START = BOOT_ROM_START + 0x10;
    private static final int SYSCALL_TABLE_SIZE = 64 * 4;                     // 64 syscalls, 4 bytes each

    private static final int SYSCALL_CODE_START = SYSCALL_TABLE_START + SYSCALL_TABLE_SIZE;
    private static final int SYSCALL_CODE_SIZE = 2 * 1024;                        // 2 KB for syscall implementations

    private static final int RAM_START = BOOT_ROM_START + BOOT_ROM_SIZE;
    private static final int RAM_SIZE = 1024 * 1024;                            // 1 MB

    private static final int VRAM_WIDTH = 128;
    private static final int VRAM_HEIGHT = 128;
    private static final VramFormat VRAM_FORMAT = VramFormat.RGBA32;          // 4 bytes per pixel
    // 128 * 128 * 4 = 64KB
    private static final int VRAM_SIZE = VRAM_WIDTH * VRAM_HEIGHT * bytesPerPixel(VRAM_FORMAT);
    private static final int VRAM_START = RAM_START + RAM_SIZE;

    private static final int IO_START = VRAM_START + VRAM_SIZE;
    private static final int IO_SIZE = 4 * 1024;                              // 4 KB

    private static final int STACK_START = RAM_START + RAM_SIZE - 4;
    private static final int HEAP_START = RAM_START + (512 * 1024);             // 512 KB into RAM
    private static final int HEAP_SIZE = STACK_START - HEAP_START;

    public NeptuneMemoryMap() {
        printMemoryLayout();
    }

    public void printMemoryLayout() {
        TablePrinter table = new TablePrinter("NEPTUNE MEMORY MAP", 20, 10, 8);

        table.printHeader("REGION");

        table.printRow("ROM (Total)", BOOT_ROM_START, BOOT_ROM_START + BOOT_ROM_SIZE - 1,
                BOOT_ROM_SIZE, "Boot ROM containing syscalls");

        table.printRow("  Boot Code", BOOT_ROM_START, SYSCALL_TABLE_START - 1,
                SYSCALL_TABLE_START - BOOT_ROM_START, "Boot loader code");

        table.printRow("  Syscall Table", SYSCALL_TABLE_START, SYSCALL_TABLE_START + SYSCALL_TABLE_SIZE - 1,
                SYSCALL_TABLE_SIZE, "Syscall number to address map");

        table.printRow("  Syscall Code", SYSCALL_CODE_START, SYSCALL_CODE_START + SYSCALL_CODE_SIZE - 1,
                SYSCALL_CODE_SIZE, "Syscall implementations");

        int romUnused = BOOT_ROM_SIZE - (SYSCALL_TABLE_START - BOOT_ROM_START) - SYSCALL_TABLE_SIZE - SYSCALL_CODE_SIZE;
        table.printRow("  ROM Unused", SYSCALL_CODE_START + SYSCALL_CODE_SIZE,
                BOOT_ROM_START + BOOT_ROM_SIZE - 1, romUnused, "Available ROM space");

        table.printFooter();

        table.printRow("RAM (Total)", RAM_START, RAM_START + RAM_SIZE - 1,
                RAM_SIZE, "Main system RAM");

        table.printRow("  Program Area", RAM_START, HEAP_START - 1,
                HEAP_START - RAM_START, "User program space");

        table.printRow("  Heap", HEAP_START, STACK_START,
                HEAP_SIZE, "Dynamic allocation");

        table.printRow("  Stack", STACK_START + 1, RAM_START + RAM_SIZE - 1,
                RAM_START + RAM_SIZE - STACK_START - 1, "Call stack (grows down)");

        table.printFooter();

        table.printRow("VRAM", VRAM_START, VRAM_START + VRAM_SIZE - 1,
                VRAM_SIZE, String.format("Video RAM %dx%d %s", VRAM_WIDTH, VRAM_HEIGHT, VRAM_FORMAT));

        table.printFooter();

        table.printRow("I/O", IO_START, IO_START + IO_SIZE - 1,
                IO_SIZE, "Memory-mapped I/O");

        table.printDoubleFooter();
    }


    @Override
    public int getBootRomStart() {
        return BOOT_ROM_START;
    }

    @Override
    public int getBootRomSize() {
        return BOOT_ROM_SIZE;
    }

    @Override
    public int getSyscallTableStart() {
        return SYSCALL_TABLE_START;
    }

    @Override
    public int getSyscallTableSize() {
        return SYSCALL_TABLE_SIZE;
    }

    @Override
    public int getSyscallCodeStart() {
        return SYSCALL_CODE_START;
    }

    @Override
    public int getSyscallCodeSize() {
        return SYSCALL_CODE_SIZE;
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
            default -> throw new IllegalArgumentException("Unsupported VRAM format: " + format);
        };
    }
}
