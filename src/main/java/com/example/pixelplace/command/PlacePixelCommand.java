package com.example.pixelplace.command;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Comando para colocar/actualizar un pixel en el canvas.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PlacePixelCommand {

    @TargetAggregateIdentifier
    private String pixelId;  // "{x}_{y}"

    private String canvasId;
    private int x;  // ← Cambiado de xPosition
    private int y;  // ← Cambiado de yPosition
    private String color;
    private String userId;
}