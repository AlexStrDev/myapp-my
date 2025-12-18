package com.example.pixelplace.service;

import com.example.pixelplace.projection.entity.CanvasView;
import com.example.pixelplace.projection.entity.PixelView;
import com.example.pixelplace.projection.repository.CanvasViewRepository;
import com.example.pixelplace.projection.repository.PixelViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Servicio para generar imágenes PNG del canvas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CanvasImageService {

    private final CanvasViewRepository canvasViewRepository;
    private final PixelViewRepository pixelViewRepository;

    /**
     * Genera una imagen PNG del canvas con todos los pixels colocados.
     *
     * @param canvasId ID del canvas
     * @param scale Factor de escala (1 = 1px por pixel, 10 = 10px por pixel)
     * @return Bytes de la imagen PNG
     * @throws IOException Si hay error generando la imagen
     */
    public byte[] generateCanvasImage(String canvasId, int scale) throws IOException {
        log.info("🖼Generando imagen del canvas: {} (escala: {}x)", canvasId, scale);

        if (scale < 1 || scale > 100) {
            throw new IllegalArgumentException("La escala debe estar entre 1 y 100");
        }

        CanvasView canvas = canvasViewRepository.findById(canvasId)
                .orElseThrow(() -> new IllegalArgumentException("Canvas no encontrado: " + canvasId));

        List<PixelView> pixels = pixelViewRepository.findByCanvasId(canvasId);

        int imageWidth = canvas.getWidth() * scale;
        int imageHeight = canvas.getHeight() * scale;

        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            g2d.setColor(hexToColor(canvas.getBackgroundColor()));
            g2d.fillRect(0, 0, imageWidth, imageHeight);

            for (PixelView pixel : pixels) {
                g2d.setColor(hexToColor(pixel.getColor()));

                int x = pixel.getX() * scale;
                int y = pixel.getY() * scale;

                g2d.fillRect(x, y, scale, scale);
            }

            log.info("Imagen generada: {}x{} px ({} pixels colocados)",
                    imageWidth, imageHeight, pixels.size());

        } finally {
            g2d.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    /**
     * Genera una imagen PNG del canvas con tamaño específico (redimensiona proporcionalmente)
     *
     * @param canvasId ID del canvas
     * @param maxWidth Ancho máximo de la imagen
     * @param maxHeight Alto máximo de la imagen
     * @return Bytes de la imagen PNG
     */
    public byte[] generateCanvasImageWithSize(String canvasId, int maxWidth, int maxHeight) throws IOException {
        CanvasView canvas = canvasViewRepository.findById(canvasId)
                .orElseThrow(() -> new IllegalArgumentException("Canvas no encontrado: " + canvasId));

        int scaleX = maxWidth / canvas.getWidth();
        int scaleY = maxHeight / canvas.getHeight();
        int scale = Math.max(1, Math.min(scaleX, scaleY));

        return generateCanvasImage(canvasId, scale);
    }

    /**
     * Genera una imagen PNG del canvas con grid (líneas de cuadrícula)
     *
     * @param canvasId ID del canvas
     * @param scale Factor de escala
     * @param showGrid Si debe mostrar líneas de cuadrícula
     * @return Bytes de la imagen PNG
     */
    public byte[] generateCanvasImageWithGrid(String canvasId, int scale, boolean showGrid) throws IOException {
        byte[] imageBytes = generateCanvasImage(canvasId, scale);

        if (!showGrid || scale < 5) {
            return imageBytes;
        }

        CanvasView canvas = canvasViewRepository.findById(canvasId)
                .orElseThrow(() -> new IllegalArgumentException("Canvas no encontrado: " + canvasId));

        java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageBytes);
        BufferedImage image = ImageIO.read(bais);
        Graphics2D g2d = image.createGraphics();

        try {
            g2d.setColor(new Color(200, 200, 200, 100)); // Gris semi-transparente
            g2d.setStroke(new BasicStroke(1));

            for (int x = 0; x <= canvas.getWidth(); x++) {
                int xPos = x * scale;
                g2d.drawLine(xPos, 0, xPos, canvas.getHeight() * scale);
            }

            for (int y = 0; y <= canvas.getHeight(); y++) {
                int yPos = y * scale;
                g2d.drawLine(0, yPos, canvas.getWidth() * scale, yPos);
            }

        } finally {
            g2d.dispose();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    private Color hexToColor(String hex) {
        if (hex == null || !hex.startsWith("#")) {
            return Color.WHITE;
        }

        try {
            String hexValue = hex.substring(1);

            if (hexValue.length() == 3) {
                hexValue = String.format("%c%c%c%c%c%c",
                        hexValue.charAt(0), hexValue.charAt(0),
                        hexValue.charAt(1), hexValue.charAt(1),
                        hexValue.charAt(2), hexValue.charAt(2));
            }

            int r = Integer.parseInt(hexValue.substring(0, 2), 16);
            int g = Integer.parseInt(hexValue.substring(2, 4), 16);
            int b = Integer.parseInt(hexValue.substring(4, 6), 16);

            return new Color(r, g, b);

        } catch (Exception e) {
            log.warn("Color hex inválido: {}, usando blanco", hex);
            return Color.WHITE;
        }
    }
}