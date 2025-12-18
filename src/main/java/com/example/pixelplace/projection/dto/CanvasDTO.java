package com.example.pixelplace.projection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO para respuesta de Canvas
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanvasDTO {
    private String canvasId;
    private String name;
    private Integer width;
    private Integer height;
    private String backgroundColor;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long pixelCount;  // Número de pixels colocados
}