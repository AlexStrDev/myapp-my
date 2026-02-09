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
 * Servicio para generar imágenes de manera incremental.
 * 
 * En lugar de reconstruir toda la imagen cada vez, carga la imagen anterior
 * y pinta solo los nuevos pixeles.
 * 
 * CAMBIOS:
 * - Grid se dibuja DESPUÉS de los pixeles para evitar sobrescritura
 * - Color del grid más visible (alpha 150 en vez de 80)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IncrementalImageService {

    private final CanvasImageFileRepository imageRepository;
    private final CanvasStateProjection canvasProjection;
    private final ImageGenerationProperties properties;

    /**
     * Genera o actualiza la imagen de un canvas de manera incremental.
     * 
     * @param canvasId ID del canvas
     * @param newPixels Lista de nuevos pixeles a pintar
     * @param scale Factor de escala
     * @param grid Si se debe dibujar cuadrícula
     * @return Imagen actualizada
     */
    public BufferedImage updateCanvasImage(String canvasId, List<PixelState> newPixels, 
                                           int scale, boolean grid) throws IOException {
        
        log.info("🎨 Actualizando imagen incremental: canvas={}, pixels={}, scale={}, grid={}", 
                canvasId, newPixels.size(), scale, grid);

        // 1. Obtener metadata del canvas
        CanvasState canvasState = canvasProjection.rebuildCanvasState(canvasId);

        // 2. Intentar cargar imagen anterior
        BufferedImage image = imageRepository.loadImage(canvasId, scale);

        // 3. Si no existe, crear imagen base
        if (image == null) {
            log.info("📄 No existe imagen previa, creando imagen base...");
            image = createBaseImage(canvasState, scale, false); // Sin grid aún
        }

        // 4. Pintar nuevos pixeles sobre la imagen existente
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        for (PixelState pixel : newPixels) {
            Color color = parseColor(pixel.getColor());
            g2d.setColor(color);
            g2d.fillRect(pixel.getX() * scale, pixel.getY() * scale, scale, scale);
            
            log.debug("🖌️ Pixel pintado: ({}, {}) - {}", pixel.getX(), pixel.getY(), pixel.getColor());
        }

        g2d.dispose();

        // 5. IMPORTANTE: Dibujar grid DESPUÉS de los pixeles para que sea visible
        if (grid && scale > 1) {
            image = addGridToImage(image, canvasState.getWidth(), canvasState.getHeight(), scale);
        }

        // 6. Guardar imagen actualizada
        imageRepository.saveImage(canvasId, image, scale);

        log.info("✅ Imagen actualizada: {} pixeles pintados, grid={}", newPixels.size(), grid);

        return image;
    }

    /**
     * Crea una imagen base para un canvas (fondo solamente, sin grid).
     * 
     * @param canvasState Estado del canvas
     * @param scale Factor de escala
     * @param includeGrid Si incluir grid (normalmente false para incremental)
     * @return Imagen base
     */
    private BufferedImage createBaseImage(CanvasState canvasState, int scale, boolean includeGrid) {
        int scaledWidth = canvasState.getWidth() * scale;
        int scaledHeight = canvasState.getHeight() * scale;

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

        log.info("📄 Imagen base creada: {}x{}", scaledWidth, scaledHeight);

        return image;
    }

    /**
     * Agrega grid a una imagen existente.
     * Se usa DESPUÉS de pintar pixeles para evitar sobrescritura.
     * 
     * @param image Imagen sobre la cual dibujar el grid
     * @param widthInPixels Ancho en pixeles del canvas (no escalados)
     * @param heightInPixels Alto en pixeles del canvas (no escalados)
     * @param scale Factor de escala actual
     * @return Imagen con grid aplicado
     */
    private BufferedImage addGridToImage(BufferedImage image, int widthInPixels, 
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

        log.debug("📐 Grid aplicado a imagen: {}x{} pixeles", widthInPixels, heightInPixels);

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
     * Regenera completamente una imagen (útil si hay corrupción).
     * 
     * @param canvasId ID del canvas
     * @param scale Factor de escala
     * @param grid Si se debe dibujar cuadrícula
     */
    public BufferedImage regenerateFullImage(String canvasId, int scale, boolean grid) throws IOException {
        log.info("🔄 Regenerando imagen completa: canvas={}, scale={}, grid={}", canvasId, scale, grid);

        CanvasState canvasState = canvasProjection.rebuildCanvasState(canvasId);

        // Crear imagen base (sin grid)
        BufferedImage image = createBaseImage(canvasState, scale, false);

        // Pintar TODOS los pixeles
        Graphics2D g2d = image.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

        for (PixelState pixel : canvasState.getPixels().values()) {
            Color color = parseColor(pixel.getColor());
            g2d.setColor(color);
            g2d.fillRect(pixel.getX() * scale, pixel.getY() * scale, scale, scale);
        }

        g2d.dispose();

        // IMPORTANTE: Aplicar grid DESPUÉS de pintar todos los pixeles
        if (grid && scale > 1) {
            image = addGridToImage(image, canvasState.getWidth(), canvasState.getHeight(), scale);
        }

        // Guardar imagen
        imageRepository.saveImage(canvasId, image, scale);

        log.info("✅ Imagen regenerada: {} pixeles totales, grid={}", 
                canvasState.getPixelCount(), grid);

        return image;
    }
}