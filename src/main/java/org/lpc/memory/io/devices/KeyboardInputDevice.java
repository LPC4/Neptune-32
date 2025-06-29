package org.lpc.memory.io.devices;

import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import org.lpc.memory.io.IODevice;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

/**
 * KeyboardInputDevice - Memory-mapped keyboard input buffer for CPU interaction.
 * Captures keypresses from a JavaFX scene and exposes them via registers.
 * On buffer overflow (32 bytes) it removes last saved input, buffer is saved externally.
 *
 * Memory Map (16 bytes total):
 * +0x00 FIRST_CHAR    [RO] - ASCII code of the oldest character in the buffer (0 if empty)
 * +0x04 BUFFER_READY  [RO] - 1 if buffer has ≥ 2 characters, 0 otherwise
 * +0x08 CURRENT_CHAR  [RO] - ASCII code of the most recently pressed key (0 if empty)
 * +0x0C CONTROL       [WO] - Write commands to control the buffer:
 *                            - 1: Consume oldest character (remove FIRST_CHAR)
 *                            - 2: Clear the entire buffer
 *                            - 3: Reset buffer and registers
 */
public class KeyboardInputDevice implements IODevice {
    public static final int SIZE = 16;
    public static final int MAX_BUFFER_SIZE = 32;

    // Register offsets
    public static final int OFFSET_FIRST_CHAR = 0;
    public static final int OFFSET_BUFFER_READY = 4;
    public static final int OFFSET_CURRENT_CHAR = 8;
    public static final int OFFSET_CONTROL = 12;

    // Control commands
    private static final int CTRL_CONSUME_CHAR = 1;
    private static final int CTRL_CLEAR_BUFFER = 2;
    private static final int CTRL_RESET_REGISTERS = 3;

    private final int baseAddress;
    private final LinkedList<Character> inputBuffer = new LinkedList<>();
    private final int[] registers = new int[SIZE / 4];

    public KeyboardInputDevice(int baseAddress, Scene scene) {
        this.baseAddress = baseAddress;
        setupKeyEventHandler(scene);
        updateRegisters();
    }

    private void setupKeyEventHandler(Scene scene) {
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            char keyChar = mapKeyEventToChar(event);
            if (keyChar != 0) {
                synchronized (inputBuffer) {
                    handleKeyPress(keyChar);
                }
            }
            event.consume();
        });
    }

    private void handleKeyPress(char keyChar) {
        synchronized (inputBuffer) {
            if (inputBuffer.size() >= MAX_BUFFER_SIZE) {
                inputBuffer.removeFirst();
            }
            inputBuffer.add(keyChar);
            updateRegisters();
        }
    }


    private char mapKeyEventToChar(KeyEvent event) {
        return switch (event.getCode()) {
            case ENTER -> '\n';
            case BACK_SPACE -> '\b';
            case TAB -> '\t';
            case SPACE -> ' ';
            default -> {
                String text = event.getText();
                yield (text != null && !text.isEmpty()) ? text.charAt(0) : 0;
            }
        };
    }

    private void updateRegisters() {
        if (inputBuffer.isEmpty()) {
            registers[OFFSET_FIRST_CHAR / 4] = 0;
            registers[OFFSET_CURRENT_CHAR / 4] = 0;
            registers[OFFSET_BUFFER_READY / 4] = 0;
        } else {
            registers[OFFSET_FIRST_CHAR / 4] = inputBuffer.getFirst();
            registers[OFFSET_CURRENT_CHAR / 4] = inputBuffer.getLast();
            registers[OFFSET_BUFFER_READY / 4] = inputBuffer.size() >= 2 ? 1 : 0;
        }
    }

    @Override
    public boolean handles(int address) {
        return address >= baseAddress && address < baseAddress + SIZE;
    }

    @Override
    public int readWord(int address) {
        validateAddress(address);
        int offset = address - baseAddress;
        return switch (offset) {
            case OFFSET_FIRST_CHAR -> registers[0];
            case OFFSET_BUFFER_READY -> registers[1];
            case OFFSET_CURRENT_CHAR -> registers[2];
            case OFFSET_CONTROL -> 0; // CONTROL is write-only
            default -> 0;
        };
    }

    @Override
    public void writeWord(int address, int value) {
        validateAddress(address);
        int offset = address - baseAddress;

        if (offset == OFFSET_CONTROL) {
            handleControlCommand(value);
        }
    }

    private void handleControlCommand(int command) {
        synchronized (inputBuffer) {
            switch (command) {
                case CTRL_CONSUME_CHAR -> {
                    if (!inputBuffer.isEmpty()) {
                        inputBuffer.removeFirst();
                    }
                }
                case CTRL_CLEAR_BUFFER -> inputBuffer.clear();
                case CTRL_RESET_REGISTERS -> {
                    inputBuffer.clear();
                    Arrays.fill(registers, 0);
                }
            }
            updateRegisters();
        }
    }

    @Override
    public byte readByte(int address) {
        validateAddress(address);
        int alignedAddress = address & ~3;
        int byteOffset = address & 3;
        int word = readWord(alignedAddress);
        return (byte) ((word >> (byteOffset * 8)) & 0xFF);
    }

    @Override
    public void writeByte(int address, byte value) {
        validateAddress(address);
        int alignedAddress = address & ~3;
        int byteOffset = address & 3;
        int shift = byteOffset * 8;
        int mask = 0xFF << shift;

        int registerIndex = (alignedAddress - baseAddress) / 4;
        int currentWord = registers[registerIndex];
        int updatedWord = (currentWord & ~mask) | ((value & 0xFF) << shift);

        if (alignedAddress - baseAddress == OFFSET_CONTROL) {
            handleControlCommand(updatedWord);
        }
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
        return "Keyboard Input Device: buffers keys pressed from JavaFX scene";
    }

    @Override
    public Map<Integer, String> getOffsetNames() {
        return Map.of(
                OFFSET_FIRST_CHAR, "FIRST_CHAR",
                OFFSET_BUFFER_READY, "BUFFER_READY",
                OFFSET_CURRENT_CHAR, "CURRENT_CHAR",
                OFFSET_CONTROL, "CONTROL"
        );
    }

    private void validateAddress(int address) {
        if (!handles(address)) {
            throw new IllegalArgumentException(String.format(
                    "Address 0x%08X out of bounds for KeyboardInputDevice at 0x%08X - 0x%08X",
                    address, baseAddress, baseAddress + SIZE - 1));
        }
    }
}