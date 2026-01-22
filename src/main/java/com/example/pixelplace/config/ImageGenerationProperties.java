package com.example.pixelplace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración para generación automática de imágenes en batch.
 * 
 * Modos de batch:
 * - TIME: Genera imagen cada X segundos
 * - COUNT: Genera imagen cada N eventos
 * - HYBRID: Genera imagen con lo que ocurra primero (tiempo O cantidad)
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "pixel-place.image.generation")
public class ImageGenerationProperties {

    /**
     * Habilitar generación automática de imágenes
     */
    private boolean enabled = true;

    /**
     * Modo de batch: TIME, COUNT, HYBRID
     */
    private BatchMode mode = BatchMode.HYBRID;

    /**
     * Intervalo en segundos para batch por tiempo
     */
    private int batchIntervalSeconds = 5;

    /**
     * Cantidad de eventos para batch por cantidad
     */
    private int batchSize = 10;

    /**
     * Directorio donde guardar imágenes
     */
    private String storageDirectory = "/var/canvas-images";

    /**
     * Scale por defecto para las imágenes generadas
     */
    private int defaultScale = 1;

    /**
     * Grid por defecto
     */
    private boolean defaultGrid = false;

    /**
     * Generar versiones con diferentes scales
     * Ejemplo: [1, 5, 10] genera 3 imágenes: latest.png, latest_5x.png, latest_10x.png
     */
    private int[] scaleVersions = {1};

    /**
     * ========== CONFIGURACIÓN DE TILES ==========
     */
    
    /**
     * Habilitar generación de imágenes por tiles
     */
    private boolean tilesEnabled = true;
    
    /**
     * Tamaño de cada tile en pixeles
     * Debe coincidir con PlacePixelCommand.TILE_SIZE
     */
    private int tileSize = 100;
    
    /**
     * Scales para tiles (separados del canvas completo)
     * Generalmente se usan scales mayores para tiles
     */
    private int[] tileScaleVersions = {10};
    
    /**
     * Batch size específico para tiles
     * Puede ser menor que el batch del canvas completo
     */
    private int tileBatchSize = 5;

    public enum BatchMode {
        /**
         * Batch solo por tiempo (cada N segundos)
         */
        TIME,
        
        /**
         * Batch solo por cantidad de eventos (cada N eventos)
         */
        COUNT,
        
        /**
         * Batch híbrido: lo que ocurra primero entre tiempo y cantidad
         */
        HYBRID
    }
}