package com.example.pixelplace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estado de un pixel individual
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PixelState {
    private int x;
    private int y;
    private String color;
    private String userId;
    private long timestamp;
}