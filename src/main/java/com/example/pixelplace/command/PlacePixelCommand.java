package com.example.pixelplace.command;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.github.axonkafka.annotation.RoutingKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

/**
 * Comando para colocar/actualizar un pixel en el canvas.
 * 
 * ID Format: {canvasId}_{x}_{y}
 * Ejemplo: "canvas-123_50_75"
 * 
 * Routing Strategy: Basado en tiles 100x100
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PlacePixelCommand {

    private static final int TILE_SIZE = 100;

    @TargetAggregateIdentifier
    private String pixelId;  // "{canvasId}_{x}_{y}"

    private String canvasId;
    private int x;
    private int y;
    private String color;
    private String userId;
    
    @RoutingKey
    @JsonIgnore
    public String getRoutingKey() {
        int tileX = this.x / TILE_SIZE;
        int tileY = this.y / TILE_SIZE;
        return "tile_" + tileX + "_" + tileY;
    }
    
    @JsonIgnore
    public static int getTileSize() {
        return TILE_SIZE;
    }
    
    @JsonIgnore
    public static int calculateTileCount(int canvasDimension) {
        return (int) Math.ceil((double) canvasDimension / TILE_SIZE);
    }
    
    @JsonIgnore
    public static int calculateTotalTiles(int canvasWidth, int canvasHeight) {
        int tilesX = calculateTileCount(canvasWidth);
        int tilesY = calculateTileCount(canvasHeight);
        return tilesX * tilesY;
    }
}