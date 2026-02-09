package com.example.pixelplace.service;

import com.example.pixelplace.config.ImageGenerationProperties;
import com.example.pixelplace.dto.CanvasState;
import com.example.pixelplace.dto.PixelState;
import com.example.pixelplace.repository.CanvasImageFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * Servicio para generar imágenes de tiles de manera incremental.
 * 
 * Cada tile es una región del canvas (ej: 100x100 pixeles).
 * Genera y actualiza tiles independientemente.
 * 
 * CAMBIOS:
 * - Grid se dibuja DESPUÉS de los pixeles para evitar sobrescritura
 * - Color del grid más visible (alpha 150 en vez de 80)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TileImageService {

    private final CanvasImageFileRepository imageRepository;
    private final CanvasStateProjection canvasProjection;
    private final ImageGenerationProperties properties;

    /**
     * Actualiza la imagen de un tile específico de manera incremental.
     * 
     * @param canvasId ID del canvas
     * @param tileX Índice X del tile
     * @param tileY Índice Y del tile
     * @param newPixels Nuevos pixeles a pintar en este tile
     * @param scale Factor de escala
     * @param grid Si se debe dibujar cuadrícula
     * @return Imagen del tile actualizada
     */
    public BufferedImage updateTileImage(String canvasId, int tileX, int tileY,
                                         List<PixelState> newPixels, 
                                         int scale, boolean grid) throws IOException {
        
        log.info("🎨 Actualizando tile incremental: canvas={}, tile=({},{}), pixels={}, scale={}, grid={}", 
                canvasId, tileX, tileY, newPixels.size(), scale, grid);

        // 1. Obtener metadata del canvas
        CanvasState canvasState = canvasProjection.rebuildCanvasState(canvasId);

        // 2. Intentar cargar imagen anterior del tile
        BufferedImage image = imageRepository.loadTileImage(canvasId, tileX, tileY, scale);

        // 3. Si no existe, crear imagen base del tile (sin grid)
        if (image == null) {
            log.info("📄 No existe tile previo, creando tile base...");
            image = createBaseTileImage(canvasState, tileX, tileY, scale, false);
        }

        // 4. Calcular offsets del tile
        int tileSize = properties.getTileSize();
        int tileStartX = tileX * tileSize;
        int tileStartY = tileY * tileSize;

        // 5. Pintar nuevos pixeles sobre la imagen existente
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        for (PixelState pixel : newPixels) {
            // Verificar que el pixel pertenece a este tile
            if (pixel.getX() >= tileStartX && pixel.getX() < tileStartX + tileSize &&
                pixel.getY() >= tileStartY && pixel.getY() < tileStartY + tileSize) {
                
                Color color = parseColor(pixel.getColor());
                g2d.setColor(color);
                
                // Coordenadas relativas al tile y escaladas
                int relX = (pixel.getX() - tileStartX) * scale;
                int relY = (pixel.getY() - tileStartY) * scale;
                
                g2d.fillRect(relX, relY, scale, scale);
                
                log.debug("🖌️ Pixel pintado en tile: ({}, {}) - {}", 
                        pixel.getX(), pixel.getY(), pixel.getColor());
            }
        }

        g2d.dispose();

        // 6. IMPORTANTE: Dibujar grid DESPUÉS de los pixeles
        if (grid && scale > 1) {
            int tileEndX = Math.min(tileStartX + tileSize, canvasState.getWidth());
            int tileEndY = Math.min(tileStartY + tileSize, canvasState.getHeight());
            int actualTileWidth = tileEndX - tileStartX;
            int actualTileHeight = tileEndY - tileStartY;
            
            image = addGridToTileImage(image, actualTileWidth, actualTileHeight, scale);
        }

        // 7. Guardar tile actualizado
        imageRepository.saveTileImage(canvasId, tileX, tileY, image, scale);

        log.info("✅ Tile actualizado: ({}, {}) - {} pixeles pintados, grid={}", 
                tileX, tileY, newPixels.size(), grid);

        return image;
    }

    /**
     * Crea una imagen base para un tile (fondo solamente, sin grid).
     * 
     * @param canvasState Estado del canvas
     * @param tileX Índice X del tile
     * @param tileY Índice Y del tile
     * @param scale Factor de escala
     * @param includeGrid Si incluir grid (normalmente false para incremental)
     * @return Imagen base del tile
     */
    private BufferedImage createBaseTileImage(CanvasState canvasState, int tileX, int tileY, 
                                              int scale, boolean includeGrid) {
        int tileSize = properties.getTileSize();
        
        // Calcular bounds del tile (puede ser menor en los bordes)
        int tileStartX = tileX * tileSize;
        int tileStartY = tileY * tileSize;
        int tileEndX = Math.min(tileStartX + tileSize, canvasState.getWidth());
        int tileEndY = Math.min(tileStartY + tileSize, canvasState.getHeight());
        
        int actualTileWidth = tileEndX - tileStartX;
        int actualTileHeight = tileEndY - tileStartY;
        
        int scaledWidth = actualTileWidth * scale;
        int scaledHeight = actualTileHeight * scale;

        BufferedImage image = new BufferedImage(
                scaledWidth,
                scaledHeight,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        // Pintar fondo
        Color bgColor = parseColor(canvasState.getBackgroundColor());
        g2d.setColor(bgColor);
        g2d.fillRect(0, 0, scaledWidth, scaledHeight);

        g2d.dispose();

        log.info("📄 Tile base creado: ({}, {}) - {}x{}", 
                tileX, tileY, scaledWidth, scaledHeight);

        return image;
    }

    /**
     * Agrega grid a una imagen de tile existente.
     * Se usa DESPUÉS de pintar pixeles para evitar sobrescritura.
     * 
     * @param image Imagen del tile sobre la cual dibujar el grid
     * @param widthInPixels Ancho en pixeles del tile (no escalados)
     * @param heightInPixels Alto en pixeles del tile (no escalados)
     * @param scale Factor de escala actual
     * @return Imagen del tile con grid aplicado
     */
    private BufferedImage addGridToTileImage(BufferedImage image, int widthInPixels, 
                                             int heightInPixels, int scale) {
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Color del grid: gris claro con alpha 150 (más visible que 80)
        g2d.setColor(new Color(128, 128, 128, 150));

        int scaledWidth = widthInPixels * scale;
        int scaledHeight = heightInPixels * scale;

        // Líneas verticales
        for (int x = 0; x <= widthInPixels; x++) {
            int scaledX = x * scale;
            g2d.drawLine(scaledX, 0, scaledX, scaledHeight);
        }

        // Líneas horizontales
        for (int y = 0; y <= heightInPixels; y++) {
            int scaledY = y * scale;
            g2d.drawLine(0, scaledY, scaledWidth, scaledY);
        }

        g2d.dispose();

        log.debug("📐 Grid aplicado a tile: {}x{} pixeles", widthInPixels, heightInPixels);

        return image;
    }

    /**
     * Parsea color hex a Color de AWT.
     * 
     * @param hexColor Color en formato hex (#RRGGBB)
     * @return Color de AWT
     */
    private Color parseColor(String hexColor) {
        if (hexColor == null || !hexColor.startsWith("#")) {
            return Color.WHITE;
        }

        try {
            if (hexColor.length() == 4) {
                // #RGB → #RRGGBB
                String r = hexColor.substring(1, 2);
                String g = hexColor.substring(2, 3);
                String b = hexColor.substring(3, 4);
                hexColor = "#" + r + r + g + g + b + b;
            }

            return Color.decode(hexColor);
        } catch (Exception e) {
            log.warn("⚠️ Color inválido: {} - usando blanco", hexColor);
            return Color.WHITE;
        }
    }

    /**
     * Regenera completamente la imagen de un tile (útil si hay corrupción).
     * 
     * @param canvasId ID del canvas
     * @param tileX Índice X del tile
     * @param tileY Índice Y del tile
     * @param scale Factor de escala
     * @param grid Si se debe dibujar cuadrícula
     */
    public BufferedImage regenerateFullTileImage(String canvasId, int tileX, int tileY, 
                                                 int scale, boolean grid) throws IOException {
        log.info("🔄 Regenerando tile completo: canvas={}, tile=({},{}), scale={}, grid={}", 
                canvasId, tileX, tileY, scale, grid);

        CanvasState canvasState = canvasProjection.rebuildCanvasState(canvasId);

        // Crear imagen base del tile (sin grid)
        BufferedImage image = createBaseTileImage(canvasState, tileX, tileY, scale, false);

        // Pintar TODOS los pixeles del tile
        int tileSize = properties.getTileSize();
        int tileStartX = tileX * tileSize;
        int tileStartY = tileY * tileSize;
        int tileEndX = Math.min(tileStartX + tileSize, canvasState.getWidth());
        int tileEndY = Math.min(tileStartY + tileSize, canvasState.getHeight());

        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        int pixelCount = 0;
        for (PixelState pixel : canvasState.getPixels().values()) {
            if (pixel.getX() >= tileStartX && pixel.getX() < tileEndX &&
                pixel.getY() >= tileStartY && pixel.getY() < tileEndY) {
                
                Color color = parseColor(pixel.getColor());
                g2d.setColor(color);
                
                int relX = (pixel.getX() - tileStartX) * scale;
                int relY = (pixel.getY() - tileStartY) * scale;
                
                g2d.fillRect(relX, relY, scale, scale);
                pixelCount++;
            }
        }

        g2d.dispose();

        // IMPORTANTE: Aplicar grid DESPUÉS de pintar todos los pixeles
        if (grid && scale > 1) {
            int actualWidth = tileEndX - tileStartX;
            int actualHeight = tileEndY - tileStartY;
            image = addGridToTileImage(image, actualWidth, actualHeight, scale);
        }

        // Guardar tile
        imageRepository.saveTileImage(canvasId, tileX, tileY, image, scale);

        log.info("✅ Tile regenerado: ({}, {}) - {} pixeles totales, grid={}", 
                tileX, tileY, pixelCount, grid);

        return image;
    }

    /**
     * Calcula el índice de tile para unas coordenadas.
     * 
     * @param x Coordenada X del pixel
     * @param y Coordenada Y del pixel
     * @return Array [tileX, tileY]
     */
    public int[] getTileIndices(int x, int y) {
        int tileSize = properties.getTileSize();
        int tileX = x / tileSize;
        int tileY = y / tileSize;
        return new int[]{tileX, tileY};
    }
}