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
 * Routing Strategy: Los comandos se enrutan a particiones basadas en tiles.
 * 
 * Configuración actual:
 * - Tile size: 100x100 pixeles
 * - Canvas: 1000x1000 pixeles
 * - Total tiles: 100 (10x10)
 * 
 * Para cambiar el tamaño de tiles, modifica TILE_SIZE:
 * - TILE_SIZE = 50  → Canvas 100x100 = 4 tiles (2x2)
 * - TILE_SIZE = 100 → Canvas 1000x1000 = 100 tiles (10x10) ✅
 * - TILE_SIZE = 200 → Canvas 2000x2000 = 100 tiles (10x10)
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PlacePixelCommand {

    /**
     * Tamaño de cada tile en pixeles.
     * 
     * IMPORTANTE: Al cambiar este valor, debes también ajustar:
     * 1. axon.kafka.topic.partitions en application.properties
     * 2. pixel-place.canvas.width y height
     * 3. Recrear los topics de Kafka
     * 
     * Fórmula: partitions = (canvas_width / TILE_SIZE) * (canvas_height / TILE_SIZE)
     * 
     * Ejemplos:
     * - Canvas 100x100, TILE_SIZE=50  → 4 partitions
     * - Canvas 500x500, TILE_SIZE=100 → 25 partitions
     * - Canvas 1000x1000, TILE_SIZE=100 → 100 partitions ✅
     * - Canvas 2000x2000, TILE_SIZE=100 → 400 partitions
     */
    private static final int TILE_SIZE = 100;

    @TargetAggregateIdentifier
    private String pixelId;  // "{x}_{y}"

    private String canvasId;
    private int x;
    private int y;
    private String color;
    private String userId;
    
    /**
     * Calcula el routing key basado en el tile al que pertenece el pixel.
     * 
     * El routing key determina a qué partición de Kafka se envía el comando,
     * permitiendo procesamiento paralelo eficiente por región del canvas.
     * 
     * Ejemplos (TILE_SIZE=100):
     * - Pixel (0,0)     → tile_0_0
     * - Pixel (50,75)   → tile_0_0
     * - Pixel (150,250) → tile_1_2
     * - Pixel (999,999) → tile_9_9
     * 
     * IMPORTANTE: @JsonIgnore previene que este método se serialice en el payload JSON.
     * El routing key solo se usa como key de Kafka, no como campo del comando.
     * 
     * @return Routing key en formato "tile_{tileX}_{tileY}"
     */
    @RoutingKey
    @JsonIgnore
    public String getRoutingKey() {
        int tileX = this.x / TILE_SIZE;
        int tileY = this.y / TILE_SIZE;
        return "tile_" + tileX + "_" + tileY;
    }
    
    /**
     * Método estático para obtener el tamaño de tile configurado.
     * Útil para validaciones y cálculos externos.
     * 
     * @return Tamaño del tile en pixeles
     */
    @JsonIgnore
    public static int getTileSize() {
        return TILE_SIZE;
    }
    
    /**
     * Calcula cuántos tiles hay en una dimensión del canvas.
     * 
     * @param canvasDimension Ancho o alto del canvas
     * @return Número de tiles en esa dimensión
     */
    @JsonIgnore
    public static int calculateTileCount(int canvasDimension) {
        return (int) Math.ceil((double) canvasDimension / TILE_SIZE);
    }
    
    /**
     * Calcula el número total de tiles para un canvas dado.
     * 
     * @param canvasWidth Ancho del canvas
     * @param canvasHeight Alto del canvas
     * @return Número total de tiles
     */
    @JsonIgnore
    public static int calculateTotalTiles(int canvasWidth, int canvasHeight) {
        int tilesX = calculateTileCount(canvasWidth);
        int tilesY = calculateTileCount(canvasHeight);
        return tilesX * tilesY;
    }
}