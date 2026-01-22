package com.example.pixelplace.dto;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * Estado completo de un canvas reconstruido desde eventos
 */
@Data
public class CanvasState {
    private String canvasId;
    private String name;
    private Integer width;
    private Integer height;
    private String backgroundColor;
    private Map<String, PixelState> pixels; // Key: "{x}_{y}"
    
    public CanvasState() {
        this.pixels = new HashMap<>();
    }
    
    public CanvasState(String canvasId, String name, Integer width, Integer height, String backgroundColor) {
        this.canvasId = canvasId;
        this.name = name;
        this.width = width;
        this.height = height;
        this.backgroundColor = backgroundColor;
        this.pixels = new HashMap<>();
    }
    
    public void setPixel(int x, int y, String color, String userId, long timestamp) {
        String key = x + "_" + y;
        pixels.put(key, new PixelState(x, y, color, userId, timestamp));
    }
    
    public PixelState getPixel(int x, int y) {
        return pixels.get(x + "_" + y);
    }
    
    public int getPixelCount() {
        return pixels.size();
    }
}