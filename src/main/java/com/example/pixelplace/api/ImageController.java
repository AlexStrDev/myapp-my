package com.example.pixelplace.api;

import com.example.pixelplace.repository.CanvasImageFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller para servir imágenes del canvas pre-generadas.
 * 
 * Las imágenes se generan automáticamente en background mediante EventHandlers.
 * Este controller solo las sirve.
 */
@Slf4j
@RestController
@RequestMapping("/api/canvas/{canvasId}/image")
@RequiredArgsConstructor
public class ImageController {

    private final CanvasImageFileRepository imageRepository;

    /**
     * GET /api/canvas/{canvasId}/image
     * GET /api/canvas/{canvasId}/image?scale=10
     * 
     * Sirve la imagen pre-generada del canvas.
     * 
     * NOTA: Las imágenes se generan automáticamente en background.
     * Si no existe imagen, retorna 404.
     */
    @GetMapping
    public ResponseEntity<Resource> getCanvasImage(
            @PathVariable("canvasId") String canvasId,
            @RequestParam(name = "scale", defaultValue = "1") int scale) {
        
        log.info("🖼️ Request imagen - Canvas: {}, scale={}", canvasId, scale);
        
        // Validaciones
        if (scale < 1 || scale > 100) {
            log.warn("⚠️ Scale inválido: {}", scale);
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // Verificar que existe la imagen
            if (!imageRepository.imageExists(canvasId, scale)) {
                log.warn("⚠️ Imagen no encontrada: canvas={}, scale={}", canvasId, scale);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(null);
            }

            // Obtener path de la imagen
            Path imagePath = imageRepository.getImagePath(canvasId, scale);

            if (!Files.exists(imagePath)) {
                log.error("❌ Archivo no existe: {}", imagePath);
                return ResponseEntity.notFound().build();
            }

            // Servir imagen como recurso
            Resource imageResource = new FileSystemResource(imagePath);
            long fileSize = Files.size(imagePath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(fileSize);
            headers.setCacheControl("public, max-age=5"); // Cache 5 segundos
            
            log.info("✅ Imagen servida: {} KB", fileSize / 1024);
            
            return new ResponseEntity<>(imageResource, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("❌ Error sirviendo imagen", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/canvas/{canvasId}/image/stats
     * 
     * Obtiene información sobre las imágenes disponibles.
     */
    @GetMapping("/stats")
    public ResponseEntity<ImageStats> getImageStats(
            @PathVariable("canvasId") String canvasId) {
        
        ImageStats stats = new ImageStats();
        stats.canvasId = canvasId;

        // Verificar qué escalas están disponibles
        int[] commonScales = {1, 5, 10, 20};
        for (int scale : commonScales) {
            if (imageRepository.imageExists(canvasId, scale)) {
                stats.availableScales.add(scale);
                
                try {
                    Path imagePath = imageRepository.getImagePath(canvasId, scale);
                    if (Files.exists(imagePath)) {
                        stats.totalSize += Files.size(imagePath);
                    }
                } catch (Exception e) {
                    log.warn("Error leyendo tamaño de imagen: {}", e.getMessage());
                }
            }
        }

        return ResponseEntity.ok(stats);
    }

    /**
     * GET /api/canvas/{canvasId}/image/tile/{tileX}/{tileY}
     * GET /api/canvas/{canvasId}/image/tile/{tileX}/{tileY}?scale=10
     * 
     * Sirve la imagen pre-generada de un tile específico.
     */
    @GetMapping("/tile/{tileX}/{tileY}")
    public ResponseEntity<Resource> getTileImage(
            @PathVariable("canvasId") String canvasId,
            @PathVariable("tileX") int tileX,
            @PathVariable("tileY") int tileY,
            @RequestParam(name = "scale", defaultValue = "10") int scale) {
        
        log.info("🖼️ Request tile - Canvas: {}, tile=({},{}), scale={}", 
                canvasId, tileX, tileY, scale);
        
        // Validaciones
        if (scale < 1 || scale > 100) {
            log.warn("⚠️ Scale inválido: {}", scale);
            return ResponseEntity.badRequest().build();
        }
        
        if (tileX < 0 || tileY < 0) {
            log.warn("⚠️ Índices de tile inválidos: ({}, {})", tileX, tileY);
            return ResponseEntity.badRequest().build();
        }
        
        try {
            // Verificar que existe la imagen del tile
            if (!imageRepository.tileImageExists(canvasId, tileX, tileY, scale)) {
                log.warn("⚠️ Tile no encontrado: canvas={}, tile=({},{}), scale={}", 
                        canvasId, tileX, tileY, scale);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Obtener path de la imagen del tile
            Path imagePath = imageRepository.getTileImagePath(canvasId, tileX, tileY, scale);

            if (!Files.exists(imagePath)) {
                log.error("❌ Archivo no existe: {}", imagePath);
                return ResponseEntity.notFound().build();
            }

            // Servir imagen como recurso
            Resource imageResource = new FileSystemResource(imagePath);
            long fileSize = Files.size(imagePath);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(fileSize);
            headers.setCacheControl("public, max-age=5"); // Cache 5 segundos
            
            log.info("✅ Tile servido: {} KB", fileSize / 1024);
            
            return new ResponseEntity<>(imageResource, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("❌ Error sirviendo tile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // DTO para stats
    @lombok.Data
    public static class ImageStats {
        private String canvasId;
        private java.util.List<Integer> availableScales = new java.util.ArrayList<>();
        private long totalSize;
    }
}