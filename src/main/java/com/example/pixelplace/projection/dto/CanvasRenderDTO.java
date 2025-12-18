package com.example.pixelplace.projection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO para renderizar un canvas completo con todos sus pixels
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanvasRenderDTO {
    private CanvasDTO canvas;
    private String[][] grid;  // Matriz de colores [y][x]
    private List<PixelDTO> pixels;  // Lista de pixels colocados (opcional)

    /**
     * Metadata de renderizado
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RenderMetadata {
        private Integer totalPixelsPlaced;
        private Integer emptyPixels;
        private Double coveragePercentage;
    }

    private RenderMetadata metadata;
}