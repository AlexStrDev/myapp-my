package com.example.pixelplace.eventhandler;

import com.example.pixelplace.config.ImageGenerationProperties;
import com.example.pixelplace.dto.PixelState;
import com.example.pixelplace.event.CanvasCreatedEvent;
import com.example.pixelplace.event.PixelPlacedEvent;
import com.example.pixelplace.service.IncrementalImageService;
import com.example.pixelplace.service.TileImageService;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EventHandler que escucha eventos de pixeles y genera imágenes en batch.
 * 
 * Genera dos tipos de imágenes:
 * 1. Canvas completo
 * 2. Tiles individuales (100x100 pixeles por defecto)
 * 
 * Estrategias de batch:
 * - TIME: Genera imagen cada X segundos (vía @Scheduled)
 * - COUNT: Genera imagen cada N eventos
 * - HYBRID: Genera imagen con lo que ocurra primero
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenerationEventHandler {

    private final IncrementalImageService incrementalImageService;
    private final TileImageService tileImageService;
    private final ImageGenerationProperties properties;

    // ========== CANVAS COMPLETO ==========
    
    // Acumulador de pixeles pendientes por canvas
    private final Map<String, List<PixelState>> pendingPixels = new ConcurrentHashMap<>();

    // Contador de eventos por canvas (para batch por cantidad)
    private final Map<String, Integer> eventCounts = new ConcurrentHashMap<>();

    // ========== TILES ==========
    
    // Acumulador de pixeles pendientes por tile: "canvasId_tileX_tileY" -> List<PixelState>
    private final Map<String, List<PixelState>> pendingTilePixels = new ConcurrentHashMap<>();
    
    // Contador de eventos por tile
    private final Map<String, Integer> tileEventCounts = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        if (properties.isEnabled()) {
            log.info("🚀 Generación automática de imágenes HABILITADA");
            log.info("   Modo: {}", properties.getMode());
            log.info("   Intervalo: {} segundos", properties.getBatchIntervalSeconds());
            log.info("   Batch size (canvas): {} eventos", properties.getBatchSize());
            
            if (properties.isTilesEnabled()) {
                log.info("   Tiles HABILITADOS");
                log.info("   Tile size: {}x{}", properties.getTileSize(), properties.getTileSize());
                log.info("   Batch size (tile): {} eventos", properties.getTileBatchSize());
            } else {
                log.info("   Tiles DESHABILITADOS");
            }
            
            log.info("   Storage: {}", properties.getStorageDirectory());
        } else {
            log.info("⏸️ Generación automática de imágenes DESHABILITADA");
        }
    }

    /**
     * Escucha cuando se crea un canvas para inicializar la imagen base.
     */
    @EventHandler
    public void on(CanvasCreatedEvent event) {
        if (!properties.isEnabled()) {
            return;
        }

        log.info("🎨 Canvas creado: {} - Inicializando imagen base...", event.getCanvasId());

        try {
            // Generar imagen base vacía para cada escala configurada
            for (int scale : properties.getScaleVersions()) {
                incrementalImageService.regenerateFullImage(
                        event.getCanvasId(), 
                        scale, 
                        properties.isDefaultGrid()
                );
            }

            log.info("✅ Imagen base inicializada para canvas: {}", event.getCanvasId());

        } catch (IOException e) {
            log.error("❌ Error inicializando imagen base: {}", event.getCanvasId(), e);
        }
    }

    /**
     * Escucha eventos de pixeles colocados y acumula en batch.
     */
    @EventHandler
    public void on(PixelPlacedEvent event) {
        if (!properties.isEnabled()) {
            return;
        }

        String canvasId = event.getCanvasId();

        log.debug("📥 Evento recibido: canvas={}, pixel=({}, {})", 
                canvasId, event.getX(), event.getY());

        PixelState pixelState = new PixelState(
                event.getX(),
                event.getY(),
                event.getColor(),
                event.getUserId(),
                System.currentTimeMillis()
        );

        // ========== PROCESAMIENTO PARA CANVAS COMPLETO ==========
        
        // Agregar pixel a la cola de pendientes del canvas
        pendingPixels.computeIfAbsent(canvasId, k -> new ArrayList<>()).add(pixelState);

        // Incrementar contador del canvas
        int canvasCount = eventCounts.compute(canvasId, (k, v) -> (v == null) ? 1 : v + 1);

        log.debug("📊 Canvas {}: {} eventos acumulados", canvasId, canvasCount);

        // Verificar si debe procesar batch del canvas
        if (properties.getMode() == ImageGenerationProperties.BatchMode.COUNT ||
            properties.getMode() == ImageGenerationProperties.BatchMode.HYBRID) {

            if (canvasCount >= properties.getBatchSize()) {
                log.info("📦 Batch por CANTIDAD alcanzado para canvas {}: {} eventos", 
                        canvasId, canvasCount);
                processCanvasBatch(canvasId);
            }
        }

        // ========== PROCESAMIENTO PARA TILES ==========
        
        if (properties.isTilesEnabled()) {
            // Calcular índice del tile al que pertenece el pixel
            int[] tileIndices = tileImageService.getTileIndices(event.getX(), event.getY());
            int tileX = tileIndices[0];
            int tileY = tileIndices[1];
            
            String tileKey = String.format("%s_%d_%d", canvasId, tileX, tileY);
            
            // Agregar pixel a la cola de pendientes del tile
            pendingTilePixels.computeIfAbsent(tileKey, k -> new ArrayList<>()).add(pixelState);
            
            // Incrementar contador del tile
            int tileCount = tileEventCounts.compute(tileKey, (k, v) -> (v == null) ? 1 : v + 1);
            
            log.debug("📊 Tile {}: {} eventos acumulados", tileKey, tileCount);
            
            // Verificar si debe procesar batch del tile
            if (properties.getMode() == ImageGenerationProperties.BatchMode.COUNT ||
                properties.getMode() == ImageGenerationProperties.BatchMode.HYBRID) {

                if (tileCount >= properties.getTileBatchSize()) {
                    log.info("📦 Batch por CANTIDAD alcanzado para tile {}: {} eventos", 
                            tileKey, tileCount);
                    processTileBatch(canvasId, tileX, tileY);
                }
            }
        }
    }

    /**
     * Tarea programada para procesar batches por tiempo.
     * 
     * Se ejecuta cada X segundos (configurado en properties.batchIntervalSeconds).
     */
    @Scheduled(fixedDelayString = "#{${pixel-place.image.generation.batch-interval-seconds} * 1000}")
    public void processTimeBatches() {
        if (!properties.isEnabled()) {
            return;
        }

        if (properties.getMode() != ImageGenerationProperties.BatchMode.TIME &&
            properties.getMode() != ImageGenerationProperties.BatchMode.HYBRID) {
            return; // Solo procesar si modo es TIME o HYBRID
        }

        // Procesar batches de canvas
        if (!pendingPixels.isEmpty()) {
            log.info("⏰ Procesando batches de CANVAS por TIEMPO...");

            for (String canvasId : new ArrayList<>(pendingPixels.keySet())) {
                processCanvasBatch(canvasId);
            }
        }

        // Procesar batches de tiles
        if (properties.isTilesEnabled() && !pendingTilePixels.isEmpty()) {
            log.info("⏰ Procesando batches de TILES por TIEMPO...");

            for (String tileKey : new ArrayList<>(pendingTilePixels.keySet())) {
                // Parse tileKey: "canvasId_tileX_tileY"
                String[] parts = tileKey.split("_");
                if (parts.length >= 3) {
                    // Reconstruir canvasId (puede contener guiones)
                    int lastUnderscore = tileKey.lastIndexOf("_");
                    int secondLastUnderscore = tileKey.lastIndexOf("_", lastUnderscore - 1);
                    
                    String canvasId = tileKey.substring(0, secondLastUnderscore);
                    int tileX = Integer.parseInt(parts[parts.length - 2]);
                    int tileY = Integer.parseInt(parts[parts.length - 1]);
                    
                    processTileBatch(canvasId, tileX, tileY);
                }
            }
        }
    }

    /**
     * Procesa un batch de pixeles pendientes para un canvas completo.
     * 
     * @param canvasId ID del canvas
     */
    private synchronized void processCanvasBatch(String canvasId) {
        List<PixelState> pixels = pendingPixels.remove(canvasId);
        Integer count = eventCounts.remove(canvasId);

        if (pixels == null || pixels.isEmpty()) {
            return;
        }

        log.info("🎨 Procesando batch de CANVAS: canvasId={}, pixeles={}", canvasId, pixels.size());

        try {
            // Generar imagen para cada escala configurada
            for (int scale : properties.getScaleVersions()) {
                incrementalImageService.updateCanvasImage(
                        canvasId,
                        pixels,
                        scale,
                        properties.isDefaultGrid()
                );
            }

            log.info("✅ Batch de CANVAS procesado: canvasId={}, {} pixeles, {} escalas",
                    canvasId, pixels.size(), properties.getScaleVersions().length);

        } catch (Exception e) {
            log.error("❌ Error procesando batch de canvas {}: {}", canvasId, e.getMessage(), e);

            // Reintentar regeneración completa si falla
            try {
                log.warn("🔄 Intentando regeneración completa del canvas...");
                
                for (int scale : properties.getScaleVersions()) {
                    incrementalImageService.regenerateFullImage(
                            canvasId,
                            scale,
                            properties.isDefaultGrid()
                    );
                }
                
                log.info("✅ Regeneración completa del canvas exitosa");
                
            } catch (Exception e2) {
                log.error("❌ Fallo crítico en regeneración del canvas: {}", e2.getMessage(), e2);
            }
        }
    }

    /**
     * Procesa un batch de pixeles pendientes para un tile específico.
     * 
     * @param canvasId ID del canvas
     * @param tileX Índice X del tile
     * @param tileY Índice Y del tile
     */
    private synchronized void processTileBatch(String canvasId, int tileX, int tileY) {
        String tileKey = String.format("%s_%d_%d", canvasId, tileX, tileY);
        
        List<PixelState> pixels = pendingTilePixels.remove(tileKey);
        Integer count = tileEventCounts.remove(tileKey);

        if (pixels == null || pixels.isEmpty()) {
            return;
        }

        log.info("🎨 Procesando batch de TILE: tile=({},{}), canvasId={}, pixeles={}", 
                tileX, tileY, canvasId, pixels.size());

        try {
            // Generar imagen del tile para cada escala configurada
            for (int scale : properties.getTileScaleVersions()) {
                tileImageService.updateTileImage(
                        canvasId,
                        tileX,
                        tileY,
                        pixels,
                        scale,
                        properties.isDefaultGrid()
                );
            }

            log.info("✅ Batch de TILE procesado: tile=({},{}), {} pixeles, {} escalas",
                    tileX, tileY, pixels.size(), properties.getTileScaleVersions().length);

        } catch (Exception e) {
            log.error("❌ Error procesando batch de tile ({},{}): {}", tileX, tileY, e.getMessage(), e);

            // Reintentar regeneración completa del tile si falla
            try {
                log.warn("🔄 Intentando regeneración completa del tile...");
                
                for (int scale : properties.getTileScaleVersions()) {
                    tileImageService.regenerateFullTileImage(
                            canvasId,
                            tileX,
                            tileY,
                            scale,
                            properties.isDefaultGrid()
                    );
                }
                
                log.info("✅ Regeneración completa del tile exitosa");
                
            } catch (Exception e2) {
                log.error("❌ Fallo crítico en regeneración del tile: {}", e2.getMessage(), e2);
            }
        }
    }

    /**
     * Obtiene estadísticas del procesamiento de batches.
     * 
     * @return Estadísticas en formato String
     */
    public String getStats() {
        int totalPendingCanvas = pendingPixels.values().stream()
                .mapToInt(List::size)
                .sum();

        int totalPendingTiles = pendingTilePixels.values().stream()
                .mapToInt(List::size)
                .sum();

        return String.format("Pendientes - Canvas: %d canvas (%d pixeles), Tiles: %d tiles (%d pixeles)",
                pendingPixels.size(), totalPendingCanvas, 
                pendingTilePixels.size(), totalPendingTiles);
    }
}