package org.lpc.visualization.vram;

import javafx.animation.AnimationTimer;
import javafx.scene.image.PixelWriter;
import org.lpc.memory.MemoryBus;
import org.lpc.memory.MemoryMap;

public class RGBA32Viewer extends VramViewer {
    private static final int BYTES_PER_PIXEL = 4;

    public RGBA32Viewer() {
        super();
        startRenderTimer();
    }

    public RGBA32Viewer(MemoryBus memoryBus, MemoryMap memoryMap) {
        super(memoryBus, memoryMap);
        startRenderTimer();
    }

    @Override
    public void updateImage() {
        if (memoryMap.getVramFormat() != MemoryMap.VramFormat.RGBA32) {
            throw new IllegalStateException("RGBA32 Viewer can only visualise VRAM in RGBA32 format");
        }

        int width = memoryMap.getVramWidth();
        int height = memoryMap.getVramHeight();
        int baseAddr = memoryMap.getVramStart();

        PixelWriter writer = image.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int addr = baseAddr + (y * width + x) * BYTES_PER_PIXEL;

                int r = Byte.toUnsignedInt(memoryBus.readByte(addr));
                int g = Byte.toUnsignedInt(memoryBus.readByte(addr + 1));
                int b = Byte.toUnsignedInt(memoryBus.readByte(addr + 2));
                int a = Byte.toUnsignedInt(memoryBus.readByte(addr + 3));

                int argb = (a << 24) | (r << 16) | (g << 8) | b;
                writer.setArgb(x, y, argb);
            }
        }
    }

    private void startRenderTimer() {
        new AnimationTimer() {
            private static final long interval = 50_000_000; // 50 ms in nanoseconds -> 20 FPS
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate >= interval) {
                    updateImage();
                    lastUpdate = now;
                }
            }
        }.start();
    }
}
