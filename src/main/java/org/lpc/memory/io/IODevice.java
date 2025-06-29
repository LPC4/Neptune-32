package org.lpc.memory.io;

import org.lpc.memory.MemoryHandler;

import java.util.Collections;
import java.util.Map;

public interface IODevice extends MemoryHandler {
    boolean handles(int address);
    default Map<Integer, String> getOffsetNames() {
        return Collections.emptyMap();
    }
    default String getDescription() {
        return "Memory-mapped IO device";
    }
}
