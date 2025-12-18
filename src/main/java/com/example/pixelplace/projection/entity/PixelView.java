package com.example.pixelplace.projection.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Vista materializada de Pixels.
 *
 * Propósito: Almacenar estado actual de cada pixel para queries eficientes.
 * Se actualiza (UPSERT) cuando se emiten PixelPlacedEvent.
 */
@Entity
@Table(
        name = "pixel_view",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_pixel_position",
                        columnNames = {"canvas_id", "x", "y"}
                )
        },
        indexes = {
                @Index(name = "idx_pixel_canvas", columnList = "canvas_id"),
                @Index(name = "idx_pixel_coords", columnList = "canvas_id, x, y"),
                @Index(name = "idx_pixel_user", columnList = "user_id")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PixelView {

    @Id
    @Column(name = "pixel_id", length = 255)
    private String pixelId;  // "{x}_{y}"

    @Column(name = "canvas_id", nullable = false, length = 255)
    private String canvasId;

    @Column(name = "x", nullable = false)
    private Integer x;

    @Column(name = "y", nullable = false)
    private Integer y;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    @Column(name = "placed_at", nullable = false)
    private LocalDateTime placedAt;
}