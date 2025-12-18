package com.example.pixelplace.projection.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Vista materializada del Canvas.
 *
 * Propósito: Almacenar metadata del canvas para queries eficientes.
 * Se actualiza cuando se emiten eventos de Canvas.
 */
@Entity
@Table(name = "canvas_view")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanvasView {

    @Id
    @Column(name = "canvas_id", length = 255)
    private String canvasId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "width", nullable = false)
    private Integer width;

    @Column(name = "height", nullable = false)
    private Integer height;

    @Column(name = "background_color", nullable = false, length = 7)
    private String backgroundColor;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}