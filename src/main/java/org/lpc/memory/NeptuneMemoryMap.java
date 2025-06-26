package org.lpc.memory;

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
    private static final int RAM_SIZE = 128 * 1024;                            // 128 KB

    private static final int VRAM_WIDTH = 128;
    private static final int VRAM_HEIGHT = 128;
    private static final VramFormat VRAM_FORMAT = VramFormat.RGBA32;          // 4 bytes per pixel
    // 128 * 128 * 4 = 64KB
    private static final int VRAM_SIZE = VRAM_WIDTH * VRAM_HEIGHT * bytesPerPixel(VRAM_FORMAT);
    private static final int VRAM_START = RAM_START + RAM_SIZE;

    private static final int IO_START = VRAM_START + VRAM_SIZE;
    private static final int IO_SIZE = 4 * 1024;                              // 4 KB

    private static final int STACK_START = RAM_START + RAM_SIZE - 4;
    private static final int HEAP_START = RAM_START + (8 * 1024);             // 8 KB into RAM
    private static final int HEAP_SIZE = STACK_START - HEAP_START;

    public NeptuneMemoryMap() {
        printMemoryLayout();
    }

    private void printMemoryLayout() {
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println("                        NEPTUNE MEMORY MAP");
        System.out.println("=".repeat(80));

        // Header
        System.out.printf("%-20s | %-12s | %-12s | %-10s | %s\n",
                "REGION", "START", "END", "SIZE", "DESCRIPTION");
        System.out.println("-".repeat(80));

        // ROM Section
        printMemoryRegion("ROM (Total)", BOOT_ROM_START, BOOT_ROM_START + BOOT_ROM_SIZE - 1,
                BOOT_ROM_SIZE, "Boot ROM containing syscalls");
        printMemoryRegion("  Boot Code", BOOT_ROM_START, SYSCALL_TABLE_START - 1,
                SYSCALL_TABLE_START - BOOT_ROM_START, "Boot loader code");
        printMemoryRegion("  Syscall Table", SYSCALL_TABLE_START, SYSCALL_TABLE_START + SYSCALL_TABLE_SIZE - 1,
                SYSCALL_TABLE_SIZE, String.format("64 syscall entries (%d bytes each)", SYSCALL_TABLE_SIZE / 64));
        printMemoryRegion("  Syscall Code", SYSCALL_CODE_START, SYSCALL_CODE_START + SYSCALL_CODE_SIZE - 1,
                SYSCALL_CODE_SIZE, "Syscall implementations");

        int romUnused = BOOT_ROM_SIZE - (SYSCALL_TABLE_START - BOOT_ROM_START) - SYSCALL_TABLE_SIZE - SYSCALL_CODE_SIZE;
        if (romUnused > 0) {
            printMemoryRegion("  ROM Unused", SYSCALL_CODE_START + SYSCALL_CODE_SIZE,
                    BOOT_ROM_START + BOOT_ROM_SIZE - 1, romUnused, "Available ROM space");
        }

        System.out.println("-".repeat(80));

        // RAM Section
        printMemoryRegion("RAM (Total)", RAM_START, RAM_START + RAM_SIZE - 1,
                RAM_SIZE, "Main system RAM");
        printMemoryRegion("  Program Area", RAM_START, HEAP_START - 1,
                HEAP_START - RAM_START, "User program space");
        printMemoryRegion("  Heap", HEAP_START, STACK_START,
                HEAP_SIZE, "Dynamic allocation");
        printMemoryRegion("  Stack", STACK_START + 1, RAM_START + RAM_SIZE - 1,
                RAM_START + RAM_SIZE - STACK_START - 1, "Call stack (grows down)");

        System.out.println("-".repeat(80));

        // VRAM Section
        printMemoryRegion("VRAM", VRAM_START, VRAM_START + VRAM_SIZE - 1,
                VRAM_SIZE, String.format("Video RAM %dx%d %s", VRAM_WIDTH, VRAM_HEIGHT, VRAM_FORMAT));

        System.out.println("-".repeat(80));

        // I/O Section
        printMemoryRegion("I/O", IO_START, IO_START + IO_SIZE - 1,
                IO_SIZE, "Memory-mapped I/O");

        System.out.println("=".repeat(80));

        // Summary
        int totalMemory = BOOT_ROM_SIZE + RAM_SIZE + VRAM_SIZE + IO_SIZE;
        System.out.println("MEMORY SUMMARY:");
        System.out.printf("  Total Address Space: 0x%08X - 0x%08X (%s)\n",
                0, IO_START + IO_SIZE - 1, formatSize(totalMemory));
        System.out.printf("  ROM: %s | RAM: %s | VRAM: %s | I/O: %s\n",
                formatSize(BOOT_ROM_SIZE), formatSize(RAM_SIZE),
                formatSize(VRAM_SIZE), formatSize(IO_SIZE));

        // Special addresses
        System.out.println("\nSPECIAL ADDRESSES:");
        System.out.printf("  Syscall Table:   0x%08X\n", SYSCALL_TABLE_START);
        System.out.printf("  Stack Pointer:   0x%08X (initial)\n", STACK_START);
        System.out.printf("  Heap Start:      0x%08X\n", HEAP_START);

        // VRAM details
        System.out.println("\nVRAM DETAILS:");
        System.out.printf("  Resolution:      %dx%d pixels\n", VRAM_WIDTH, VRAM_HEIGHT);
        System.out.printf("  Format:          %s (%d bytes/pixel)\n",
                VRAM_FORMAT, bytesPerPixel(VRAM_FORMAT));
        System.out.printf("  Total Pixels:    %,d\n", VRAM_WIDTH * VRAM_HEIGHT);

        System.out.println("=".repeat(80));
        System.out.println();
    }

    private void printMemoryRegion(String name, int start, int end, int size, String description) {
        System.out.printf("%-20s | 0x%08X | 0x%08X | %-10s | %s\n",
                name, start, end, formatSize(size), description);
    }

    private String formatSize(int bytes) {
        if (bytes >= 1024 * 1024) {
            return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
        } else if (bytes >= 1024) {
            return String.format("%.1fKB", bytes / 1024.0);
        } else {
            return bytes + "B";
        }
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
            // add other formats here if needed
            default -> throw new IllegalArgumentException("Unsupported VRAM format: " + format);
        };
    }
}
