package com.example.pixelplace.projection.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

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
    private Instant createdAt;
    private Instant updatedAt;
    private Long pixelCount;
}