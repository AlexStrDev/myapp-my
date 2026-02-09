package com.example.pixelplace.repository;

import com.example.pixelplace.config.ImageGenerationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Repositorio para guardar y cargar imágenes del canvas desde el filesystem.
 * 
 * CAMBIOS:
 * - Compresión PNG optimizada (nivel 3 en vez de 6) para mejor calidad
 * - Escritura de imagen con parámetros de calidad configurables
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CanvasImageFileRepository {

    private final ImageGenerationProperties properties;

    /**
     * Guarda una imagen en el filesystem con compresión optimizada.
     * 
     * @param canvasId ID del canvas
     * @param image Imagen a guardar
     * @param scale Factor de escala (para nombrar el archivo)
     * @return Path del archivo guardado
     */
    public Path saveImage(String canvasId, BufferedImage image, int scale) throws IOException {
        Path canvasDir = getCanvasDirectory(canvasId);
        ensureDirectoryExists(canvasDir);

        String filename = scale == 1 ? "latest.png" : String.format("latest_%dx.png", scale);
        Path imagePath = canvasDir.resolve(filename);

        // Guardar con compresión optimizada
        saveImageWithCompression(image, imagePath.toFile());

        log.info("💾 Imagen guardada: {} ({}x{}, {}KB)", 
                imagePath, image.getWidth(), image.getHeight(), 
                imagePath.toFile().length() / 1024);

        return imagePath;
    }

    /**
     * Guarda una imagen con parámetros de compresión optimizados.
     * Compresión nivel 3 (en vez de 6) para mejor calidad.
     * 
     * @param image Imagen a guardar
     * @param outputFile Archivo de salida
     */
    private void saveImageWithCompression(BufferedImage image, File outputFile) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
        
        if (!writers.hasNext()) {
            // Fallback a guardado simple si no hay writer disponible
            ImageIO.write(image, "PNG", outputFile);
            return;
        }
        
        ImageWriter writer = writers.next();
        ImageWriteParam writeParam = writer.getDefaultWriteParam();
        
        // Configurar compresión PNG
        if (writeParam.canWriteCompressed()) {
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            // Nivel 3 de compresión (0=sin compresión, 9=máxima compresión)
            // Nivel 3 balancea tamaño y calidad de imagen
            writeParam.setCompressionQuality(0.7f); // Equivalente a nivel ~3
        }
        
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(outputFile)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }
    }

    /**
     * Carga la última imagen generada para un canvas.
     * 
     * @param canvasId ID del canvas
     * @param scale Factor de escala
     * @return Imagen cargada o null si no existe
     */
    public BufferedImage loadImage(String canvasId, int scale) throws IOException {
        Path canvasDir = getCanvasDirectory(canvasId);
        String filename = scale == 1 ? "latest.png" : String.format("latest_%dx.png", scale);
        Path imagePath = canvasDir.resolve(filename);

        if (!Files.exists(imagePath)) {
            log.debug("📂 No existe imagen previa: {}", imagePath);
            return null;
        }

        BufferedImage image = ImageIO.read(imagePath.toFile());
        log.debug("📂 Imagen cargada: {} ({}x{})", imagePath, image.getWidth(), image.getHeight());
        
        return image;
    }

    /**
     * Verifica si existe una imagen previa para un canvas.
     * 
     * @param canvasId ID del canvas
     * @param scale Factor de escala
     * @return true si existe
     */
    public boolean imageExists(String canvasId, int scale) {
        Path canvasDir = getCanvasDirectory(canvasId);
        String filename = scale == 1 ? "latest.png" : String.format("latest_%dx.png", scale);
        Path imagePath = canvasDir.resolve(filename);
        
        return Files.exists(imagePath);
    }

    /**
     * Elimina todas las imágenes de un canvas.
     * 
     * @param canvasId ID del canvas
     */
    public void deleteAllImages(String canvasId) throws IOException {
        Path canvasDir = getCanvasDirectory(canvasId);
        
        if (Files.exists(canvasDir)) {
            Files.walk(canvasDir)
                    .sorted((a, b) -> b.compareTo(a)) // Borrar archivos antes que directorios
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            log.warn("⚠️ Error borrando {}: {}", path, e.getMessage());
                        }
                    });
            
            log.info("🗑️ Imágenes eliminadas: {}", canvasDir);
        }
    }

    /**
     * Obtiene el directorio del canvas.
     * 
     * @param canvasId ID del canvas
     * @return Path del directorio
     */
    private Path getCanvasDirectory(String canvasId) {
        return Paths.get(properties.getStorageDirectory(), canvasId);
    }

    /**
     * Asegura que un directorio existe.
     * 
     * @param directory Directorio a crear
     */
    private void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
            log.debug("📁 Directorio creado: {}", directory);
        }
    }

    /**
     * Obtiene la ruta de la imagen para servir via HTTP.
     * 
     * @param canvasId ID del canvas
     * @param scale Factor de escala
     * @return Path del archivo
     */
    public Path getImagePath(String canvasId, int scale) {
        Path canvasDir = getCanvasDirectory(canvasId);
        String filename = scale == 1 ? "latest.png" : String.format("latest_%dx.png", scale);
        return canvasDir.resolve(filename);
    }

    // ========== MÉTODOS PARA TILES ==========

    /**
     * Guarda una imagen de tile en el filesystem con compresión optimizada.
     * 
     * @param canvasId ID del canvas
     * @param tileX Índice X del tile
     * @param tileY Índice Y del tile
     * @param image Imagen del tile
     * @param scale Factor de escala
     * @return Path del archivo guardado
     */
    public Path saveTileImage(String canvasId, int tileX, int tileY, 
                              BufferedImage image, int scale) throws IOException {
        Path tilesDir = getTilesDirectory(canvasId);
        ensureDirectoryExists(tilesDir);

        String filename = scale == 1 ? 
                String.format("tile_%d_%d.png", tileX, tileY) : 
                String.format("tile_%d_%d_%dx.png", tileX, tileY, scale);
        
        Path imagePath = tilesDir.resolve(filename);

        // Guardar con compresión optimizada
        saveImageWithCompression(image, imagePath.toFile());

        log.info("💾 Tile guardado: {} ({}x{}, {}KB)", 
                imagePath, image.getWidth(), image.getHeight(), 
                imagePath.toFile().length() / 1024);

        return imagePath;
    }

    /**
     * Carga la imagen de un tile.
     * 
     * @param canvasId ID del canvas
     * @param tileX Índice X del tile
     * @param tileY Índice Y del tile
     * @param scale Factor de escala
     * @return Imagen del tile o null si no existe
     */
    public BufferedImage loadTileImage(String canvasId, int tileX, int tileY, 
                                       int scale) throws IOException {
        Path tilesDir = getTilesDirectory(canvasId);
        
        String filename = scale == 1 ? 
                String.format("tile_%d_%d.png", tileX, tileY) : 
                String.format("tile_%d_%d_%dx.png", tileX, tileY, scale);
        
        Path imagePath = tilesDir.resolve(filename);

        if (!Files.exists(imagePath)) {
            log.debug("📂 No existe tile previo: {}", imagePath);
            return null;
        }

        BufferedImage image = ImageIO.read(imagePath.toFile());
        log.debug("📂 Tile cargado: {} ({}x{})", imagePath, image.getWidth(), image.getHeight());
        
        return image;
    }

    /**
     * Verifica si existe una imagen de tile.
     * 
     * @param canvasId ID del canvas
     * @param tileX Índice X del tile
     * @param tileY Índice Y del tile
     * @param scale Factor de escala
     * @return true si existe
     */
    public boolean tileImageExists(String canvasId, int tileX, int tileY, int scale) {
        Path tilesDir = getTilesDirectory(canvasId);
        
        String filename = scale == 1 ? 
                String.format("tile_%d_%d.png", tileX, tileY) : 
                String.format("tile_%d_%d_%dx.png", tileX, tileY, scale);
        
        Path imagePath = tilesDir.resolve(filename);
        
        return Files.exists(imagePath);
    }

    /**
     * Obtiene la ruta de la imagen de un tile.
     * 
     * @param canvasId ID del canvas
     * @param tileX Índice X del tile
     * @param tileY Índice Y del tile
     * @param scale Factor de escala
     * @return Path del archivo
     */
    public Path getTileImagePath(String canvasId, int tileX, int tileY, int scale) {
        Path tilesDir = getTilesDirectory(canvasId);
        
        String filename = scale == 1 ? 
                String.format("tile_%d_%d.png", tileX, tileY) : 
                String.format("tile_%d_%d_%dx.png", tileX, tileY, scale);
        
        return tilesDir.resolve(filename);
    }

    /**
     * Obtiene el directorio de tiles del canvas.
     * 
     * @param canvasId ID del canvas
     * @return Path del directorio de tiles
     */
    private Path getTilesDirectory(String canvasId) {
        return getCanvasDirectory(canvasId).resolve("tiles");
    }
}