package org.lpc.memory.io.devices;

import javafx.animation.AnimationTimer;
import org.lpc.memory.io.IODevice;

import java.util.Map;

/**
 * TimerDevice - Memory-mapped hardware timer.
 * Provides time counting and compare-match functionality.
 *
 * Memory Map (16 bytes total):
 * +0x00 CURRENT_TIME   [RO] - Current tick counter (increments continuously)
 * +0x04 COMPARE_VALUE  [RW] - When CURRENT_TIME equals this, STATUS becomes 1
 * +0x08 STATUS         [RO] - 1 if compare match occurred, else 0
 * +0x0C CONTROL        [WO] - Write commands:
 *                              - 1: Clear STATUS
 *                              - 2: Reset CURRENT_TIME
 */
public class TimerDevice implements IODevice {
    public static final int SIZE = 16;

    public static final int OFFSET_CURRENT_TIME = 0;
    public static final int OFFSET_COMPARE_VALUE = 4;
    public static final int OFFSET_STATUS = 8;
    public static final int OFFSET_CONTROL = 12;

    private static final int CTRL_CLEAR_STATUS = 1;
    private static final int CTRL_RESET_TIME = 2;

    private final int baseAddress;
    private final int[] registers = new int[SIZE / 4];

    private long startTimeNanos;

    public TimerDevice(int baseAddress) {
        this.baseAddress = baseAddress;
        this.startTimeNanos = System.nanoTime();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateTimer();
            }
        };
        timer.start();
    }

    private void updateTimer() {
        int currentTime = getCurrentTime();
        registers[OFFSET_CURRENT_TIME / 4] = currentTime;

        if (currentTime == registers[OFFSET_COMPARE_VALUE / 4]) {
            registers[OFFSET_STATUS / 4] = 1;
        }
    }

    private int getCurrentTime() {
        long elapsedNanos = System.nanoTime() - startTimeNanos;
        return (int) (elapsedNanos / 1_000_000); // milliseconds, wraps at 49.7 days
    }

    @Override
    public boolean handles(int address) {
        return address >= baseAddress && address < baseAddress + SIZE;
    }

    @Override
    public int readWord(int address) {
        validateAddress(address);
        int offset = address - baseAddress;
        return offset == OFFSET_CONTROL ? 0 : registers[offset / 4]; // CONTROL is write only

    }

    @Override
    public void writeWord(int address, int value) {
        validateAddress(address);
        int offset = address - baseAddress;

        if (offset == OFFSET_COMPARE_VALUE) {
            registers[OFFSET_COMPARE_VALUE / 4] = value;
        } else if (offset == OFFSET_CONTROL) {
            handleControlCommand(value);
        }
    }

    private void handleControlCommand(int command) {
        if (command == CTRL_CLEAR_STATUS) {
            registers[OFFSET_STATUS / 4] = 0;
        } else if (command == CTRL_RESET_TIME) {
            startTimeNanos = System.nanoTime();
            registers[OFFSET_STATUS / 4] = 0;
            registers[OFFSET_CURRENT_TIME / 4] = 0;
        }
    }

    @Override
    public byte readByte(int address) {
        validateAddress(address);
        int aligned = address & ~3;
        int shift = (address & 3) * 8;
        return (byte) ((readWord(aligned) >> shift) & 0xFF);
    }

    @Override
    public void writeByte(int address, byte value) {
        validateAddress(address);
        int aligned = address & ~3;
        int shift = (address & 3) * 8;
        int mask = 0xFF << shift;
        int current = readWord(aligned);
        int updated = (current & ~mask) | ((value & 0xFF) << shift);
        writeWord(aligned, updated);
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
        return "Timer Device: provides current time and compare functionality";
    }

    @Override
    public Map<Integer, String> getOffsetNames() {
        return Map.of(
                OFFSET_CURRENT_TIME, "CURRENT_TIME",
                OFFSET_COMPARE_VALUE, "COMPARE_VALUE",
                OFFSET_STATUS, "STATUS",
                OFFSET_CONTROL, "CONTROL"
        );
    }

    private void validateAddress(int address) {
        if (!handles(address)) {
            throw new IllegalArgumentException(String.format(
                    "Address 0x%08X out of bounds for TimerDevice at 0x%08X - 0x%08X",
                    address, baseAddress, baseAddress + SIZE - 1));
        }
    }
}
