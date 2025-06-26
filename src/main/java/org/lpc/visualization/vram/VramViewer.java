package org.lpc.visualization.vram;

import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import org.lpc.memory.MemoryBus;
import org.lpc.memory.MemoryMap;

@NoArgsConstructor
public abstract class VramViewer {
    protected MemoryBus memoryBus;
    protected MemoryMap memoryMap;

    protected WritableImage image;

    protected static final int PIXEL_SCALE = 4;

    public VramViewer(MemoryBus memoryBus, MemoryMap memoryMap) {
        this.memoryBus = memoryBus;
        this.memoryMap = memoryMap;
    }

    public void start(Stage stage) {
        if (memoryBus == null || memoryMap == null) {
            throw new IllegalStateException("MemoryBus and MemoryMap must be set before launching");
        }

        int width = memoryMap.getVramWidth();
        int height = memoryMap.getVramHeight();

        image = new WritableImage(width, height);
        ImageView imageView = new ImageView(image);

        imageView.setFitWidth(width * PIXEL_SCALE);
        imageView.setFitHeight(height * PIXEL_SCALE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(false);

        updateImage();

        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root);

        stage.setTitle("VRAM Viewer");
        stage.setScene(scene);
        stage.setWidth(width * PIXEL_SCALE + 16);
        stage.setHeight(height * PIXEL_SCALE + 39);
        stage.show();
    }

    public void setMemory(MemoryBus memoryBus, MemoryMap memoryMap) {
        this.memoryBus = memoryBus;
        this.memoryMap = memoryMap;
    }

    public abstract void updateImage();
}

