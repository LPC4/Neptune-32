package org.lpc.visualization.vram;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.*;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.lpc.memory.MemoryBus;
import org.lpc.memory.MemoryMap;

@NoArgsConstructor
public class RGBA32Visualiser extends Application {
    private MemoryBus memoryBus;
    private MemoryMap memoryMap;

    private static final int BYTES_PER_PIXEL = 4;
    private static final int PIXEL_SCALE = 4;

    private WritableImage image;
    private ImageView imageView;

    public RGBA32Visualiser(MemoryBus memoryBus, MemoryMap memoryMap) {
        this.memoryBus = memoryBus;
        this.memoryMap = memoryMap;
    }

    @Override
    public void start(Stage stage) {
        if (memoryBus == null || memoryMap == null) {
            throw new IllegalStateException("MemoryBus and MemoryMap must be set before launching");
        }

        int width = memoryMap.getVramWidth();
        int height = memoryMap.getVramHeight();

        image = new WritableImage(width, height);
        imageView = new ImageView(image);

        // Set scaled size:
        imageView.setFitWidth(width * PIXEL_SCALE);
        imageView.setFitHeight(height * PIXEL_SCALE);

        imageView.setPreserveRatio(true);
        imageView.setSmooth(false);

        updateImage();

        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root);

        stage.setTitle("VRAM Viewer");
        stage.setScene(scene);
        stage.setWidth(width * PIXEL_SCALE + 16);  // + window chrome buffer
        stage.setHeight(height * PIXEL_SCALE + 39); // + window chrome buffer
        stage.show();
    }

    public void updateImage() {
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

                int argb = ((a & 0xFF) << 24) |
                        ((r & 0xFF) << 16) |
                        ((g & 0xFF) << 8) |
                        (b & 0xFF);

                writer.setArgb(x, y, argb);
            }
        }
    }

    public void setMemory(MemoryBus memoryBus, MemoryMap memoryMap) {
        this.memoryBus = memoryBus;
        this.memoryMap = memoryMap;
    }
}
