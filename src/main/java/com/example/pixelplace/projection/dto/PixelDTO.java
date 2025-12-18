package com.example.pixelplace.projection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuesta de Pixel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixelDTO {
    private String pixelId;
    private String canvasId;
    private Integer x;
    private Integer y;
    private String color;
    private String userId;
    private LocalDateTime placedAt;
}