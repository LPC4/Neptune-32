package org.lpc.memory.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.lpc.memory.MemoryHandler;
import org.lpc.visualization.debug.TablePrinter;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

@Getter
public class IODeviceManager implements MemoryHandler {
    private final NavigableMap<Integer, IODevice> addressMap = new TreeMap<>();
    private final int size;
    private final int baseAddress;
    private int currentOffset = 0;

    public IODeviceManager(int baseAddress, int size) {
        this.baseAddress = baseAddress;
        this.size = size;
    }

    public void register(IODevice device) {
        currentOffset += device.getSize();
        for (int addr = baseAddress; addr < baseAddress + size; addr++) {
            if (device.handles(addr)) {
                addressMap.put(addr, device);
            }
        }
    }

    public int readWord(int addr) {
        var device = addressMap.get(addr);
        return device != null ? device.readWord(addr) : 0;
    }

    public void writeWord(int addr, int val) {
        var device = addressMap.get(addr);
        if (device != null) device.writeWord(addr, val);
    }

    public byte readByte(int addr) {
        var device = addressMap.get(addr);
        return device != null ? device.readByte(addr) : 0;
    }

    public void writeByte(int addr, byte val) {
        var device = addressMap.get(addr);
        if (device != null) device.writeByte(addr, val);
    }

    public void printDevices() {
        TablePrinter table = new TablePrinter("IO DEVICE MAP", 22, 10, 6);

        table.printHeader("DEVICE");

        var devices = getRegisteredDevices();
        if (devices.isEmpty()) {
            System.out.println("No IO devices registered.");
        } else {
            for (var device : devices) {
                int start = device.getBaseAddress();
                int end = start + device.getSize() - 1;

                table.printRow(
                        device.getClass().getSimpleName(),
                        formatHex(start),
                        formatHex(end),
                        device.getSize() + "B",
                        device.getDescription()
                );

                printDeviceOffsets(device);
            }
        }

        table.printFooter();
        System.out.printf("Total IO Range:  %s - %s (%s)\n",
                formatHex(baseAddress), formatHex(baseAddress + size - 1), formatSize(size));
        table.printDoubleFooter();
        System.out.println();
    }

    private void printDeviceOffsets(IODevice device) {
        var offsetNames = device.getOffsetNames();
        for (int offset = 0; offset < device.getSize(); offset += 4) {
            String name = offsetNames.getOrDefault(offset, "");
            System.out.printf("   -> +0x%02X (0x%08X)%s\n",
                    offset,
                    device.getBaseAddress() + offset,
                    name.isEmpty() ? "" : " : " + name
            );
        }
    }

    private String formatHex(int value) {
        return String.format("0x%08X", value);
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

    public List<IODevice> getRegisteredDevices() {
        return new ArrayList<>(addressMap.values().stream().distinct().toList());
    }

}


