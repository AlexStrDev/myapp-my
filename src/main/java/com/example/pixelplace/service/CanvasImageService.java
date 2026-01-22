package com.example.pixelplace.service;

import com.example.pixelplace.dto.CanvasState;
import com.example.pixelplace.dto.PixelState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Servicio para generar imágenes PNG del canvas
 * 
 * Soporta:
 * - Scale: Ampliar cada pixel por un factor (útil para zoom)
 * - Grid: Dibujar cuadrícula entre pixeles
 */
@Slf4j
@Service
public class CanvasImageService {

    private final CanvasStateProjection canvasProjection;

    public CanvasImageService(CanvasStateProjection canvasProjection) {
        this.canvasProjection = canvasProjection;
    }

    /**
     * Genera imagen completa del canvas
     * 
     * @param canvasId ID del canvas
     * @param scale Factor de escala (1 = sin escala, 10 = cada pixel se dibuja 10x10)
     * @param grid Si se debe dibujar cuadrícula
     */
    public byte[] generateCanvasImage(String canvasId, int scale, boolean grid) throws IOException {
        log.info("🖼️ Generando imagen completa para canvas: {} (scale={}, grid={})", 
                canvasId, scale, grid);
        
        CanvasState state = canvasProjection.rebuildCanvasState(canvasId);
        
        if (state.getWidth() <= 0 || state.getHeight() <= 0) {
            throw new IllegalStateException("Canvas no encontrado o sin dimensiones: " + canvasId);
        }
        
        int scaledWidth = state.getWidth() * scale;
        int scaledHeight = state.getHeight() * scale;
        
        BufferedImage image = new BufferedImage(
                scaledWidth, 
                scaledHeight, 
                BufferedImage.TYPE_INT_RGB
        );
        
        Graphics2D g2d = image.createGraphics();
        
        // Anti-aliasing para grid más suave
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        
        // Fondo
        g2d.setColor(parseColor(state.getBackgroundColor()));
        g2d.fillRect(0, 0, scaledWidth, scaledHeight);
        
        // Pintar pixeles
        for (PixelState pixel : state.getPixels().values()) {
            g2d.setColor(parseColor(pixel.getColor()));
            g2d.fillRect(
                    pixel.getX() * scale, 
                    pixel.getY() * scale, 
                    scale, 
                    scale
            );
        }
        
        // Dibujar grid si está habilitado
        if (grid && scale > 1) {
            drawGrid(g2d, state.getWidth(), state.getHeight(), scale);
        }
        
        g2d.dispose();
        
        log.info("✅ Imagen generada: {}x{} con {} pixeles", 
                scaledWidth, scaledHeight, state.getPixelCount());
        
        return encodeImage(image);
    }
    
    /**
     * Genera imagen de un tile específico
     * 
     * @param canvasId ID del canvas
     * @param tileX Índice X del tile
     * @param tileY Índice Y del tile
     * @param tileSize Tamaño del tile en pixels del canvas
     * @param scale Factor de escala
     * @param grid Si se debe dibujar cuadrícula
     */
    public byte[] generateTileImage(String canvasId, int tileX, int tileY, int tileSize,
                                    int scale, boolean grid) throws IOException {
        
        log.info("🖼️ Generando tile ({},{}) para canvas: {} (scale={}, grid={})", 
                tileX, tileY, canvasId, scale, grid);
        
        CanvasState state = canvasProjection.rebuildPixelsOnly(canvasId);
        
        // Calcular bounds del tile
        int startX = tileX * tileSize;
        int startY = tileY * tileSize;
        int endX = Math.min(startX + tileSize, state.getWidth());
        int endY = Math.min(startY + tileSize, state.getHeight());
        
        int tileWidth = endX - startX;
        int tileHeight = endY - startY;
        
        int scaledWidth = tileWidth * scale;
        int scaledHeight = tileHeight * scale;
        
        BufferedImage image = new BufferedImage(
                scaledWidth, 
                scaledHeight, 
                BufferedImage.TYPE_INT_RGB
        );
        
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        
        // Fondo
        g2d.setColor(parseColor(state.getBackgroundColor()));
        g2d.fillRect(0, 0, scaledWidth, scaledHeight);
        
        // Pintar solo pixeles dentro del tile
        state.getPixels().values().stream()
                .filter(pixel -> pixel.getX() >= startX && pixel.getX() < endX)
                .filter(pixel -> pixel.getY() >= startY && pixel.getY() < endY)
                .forEach(pixel -> {
                    g2d.setColor(parseColor(pixel.getColor()));
                    // Coordenadas relativas al tile y escaladas
                    int relX = (pixel.getX() - startX) * scale;
                    int relY = (pixel.getY() - startY) * scale;
                    g2d.fillRect(relX, relY, scale, scale);
                });
        
        // Dibujar grid si está habilitado
        if (grid && scale > 1) {
            drawGrid(g2d, tileWidth, tileHeight, scale);
        }
        
        g2d.dispose();
        
        log.info("✅ Tile generado: {}x{}", scaledWidth, scaledHeight);
        
        return encodeImage(image);
    }
    
    /**
     * Genera imagen de una región específica del canvas
     * 
     * @param canvasId ID del canvas
     * @param x Coordenada X inicial
     * @param y Coordenada Y inicial
     * @param width Ancho de la región
     * @param height Alto de la región
     * @param scale Factor de escala
     * @param grid Si se debe dibujar cuadrícula
     */
    public byte[] generateRegionImage(String canvasId, int x, int y, int width, int height,
                                      int scale, boolean grid) throws IOException {
        
        log.info("🖼️ Generando región ({},{}) {}x{} para canvas: {} (scale={}, grid={})", 
                x, y, width, height, canvasId, scale, grid);
        
        CanvasState state = canvasProjection.rebuildPixelsOnly(canvasId);
        
        // Validar bounds
        int endX = Math.min(x + width, state.getWidth());
        int endY = Math.min(y + height, state.getHeight());
        width = endX - x;
        height = endY - y;
        
        int scaledWidth = width * scale;
        int scaledHeight = height * scale;
        
        BufferedImage image = new BufferedImage(
                scaledWidth, 
                scaledHeight, 
                BufferedImage.TYPE_INT_RGB
        );
        
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        
        // Fondo
        g2d.setColor(parseColor(state.getBackgroundColor()));
        g2d.fillRect(0, 0, scaledWidth, scaledHeight);
        
        // Pintar pixeles en la región
        state.getPixels().values().stream()
                .filter(pixel -> pixel.getX() >= x && pixel.getX() < endX)
                .filter(pixel -> pixel.getY() >= y && pixel.getY() < endY)
                .forEach(pixel -> {
                    g2d.setColor(parseColor(pixel.getColor()));
                    int relX = (pixel.getX() - x) * scale;
                    int relY = (pixel.getY() - y) * scale;
                    g2d.fillRect(relX, relY, scale, scale);
                });
        
        // Dibujar grid si está habilitado
        if (grid && scale > 1) {
            drawGrid(g2d, width, height, scale);
        }
        
        g2d.dispose();
        
        return encodeImage(image);
    }
    
    /**
     * Dibuja una cuadrícula sobre la imagen
     * 
     * @param g2d Graphics2D context
     * @param widthInPixels Ancho en pixeles del canvas (no escalados)
     * @param heightInPixels Alto en pixeles del canvas (no escalados)
     * @param scale Factor de escala actual
     */
    private void drawGrid(Graphics2D g2d, int widthInPixels, int heightInPixels, int scale) {
        // Color del grid: gris claro semi-transparente
        g2d.setColor(new Color(128, 128, 128, 80));
        
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
    }
    
    /**
     * Codifica BufferedImage a bytes PNG
     */
    private byte[] encodeImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
    
    /**
     * Parsea color hex a Color de AWT
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
}