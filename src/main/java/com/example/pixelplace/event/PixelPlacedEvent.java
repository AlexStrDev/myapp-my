package com.example.pixelplace.event;

import com.example.pixelplace.domain.model.PixelUser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Evento emitido cuando se coloca/actualiza un pixel.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PixelPlacedEvent {

    private String pixelId;      // "{x}_{y}"
    private String canvasId;
    private int x;  // ← Cambiado de xPosition
    private int y;  // ← Cambiado de yPosition
    private String color;
    private String userId;
    private List<PixelUser> pixelUsers;  // Historial de usuarios que modificaron este pixel
}