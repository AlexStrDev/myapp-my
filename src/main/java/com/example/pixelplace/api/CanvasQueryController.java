package com.example.pixelplace.api.query;

import com.example.pixelplace.projection.dto.CanvasDTO;
import com.example.pixelplace.projection.dto.CanvasRenderDTO;
import com.example.pixelplace.projection.dto.PixelDTO;
import com.example.pixelplace.projection.entity.CanvasView;
import com.example.pixelplace.projection.entity.PixelView;
import com.example.pixelplace.projection.repository.CanvasViewRepository;
import com.example.pixelplace.projection.repository.PixelViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller para queries de Canvas y Pixels (read-only).
 *
 * Endpoints de consulta que leen desde las vistas materializadas.
 */
@Slf4j
@RestController
@RequestMapping("/api/canvas")
@RequiredArgsConstructor
public class CanvasQueryController {

    private final CanvasViewRepository canvasViewRepository;
    private final PixelViewRepository pixelViewRepository;

    /**
     * GET /api/canvas
     * Listar todos los canvas
     */
    @GetMapping
    public ResponseEntity<List<CanvasDTO>> getAllCanvas() {
        log.info("📋 Listando todos los canvas");

        List<CanvasView> canvasViews = canvasViewRepository.findAllByOrderByCreatedAtDesc();

        List<CanvasDTO> canvasDTOs = canvasViews.stream()
                .map(this::toCanvasDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(canvasDTOs);
    }

    /**
     * GET /api/canvas/{canvasId}
     * Obtener metadata de un canvas específico
     */
    @GetMapping("/{canvasId}")
    public ResponseEntity<CanvasDTO> getCanvas(@PathVariable String canvasId) {
        log.info("🔍 Obteniendo canvas: {}", canvasId);

        CanvasView canvasView = canvasViewRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas no encontrado: " + canvasId));

        return ResponseEntity.ok(toCanvasDTO(canvasView));
    }

    /**
     * GET /api/canvas/{canvasId}/pixels
     * Obtener todos los pixels de un canvas
     */
    @GetMapping("/{canvasId}/pixels")
    public ResponseEntity<List<PixelDTO>> getCanvasPixels(@PathVariable String canvasId) {
        log.info("🎨 Obteniendo pixels del canvas: {}", canvasId);

        // Verificar que el canvas existe
        if (!canvasViewRepository.existsById(canvasId)) {
            throw new RuntimeException("Canvas no encontrado: " + canvasId);
        }

        List<PixelView> pixelViews = pixelViewRepository.findByCanvasId(canvasId);

        List<PixelDTO> pixelDTOs = pixelViews.stream()
                .map(this::toPixelDTO)
                .collect(Collectors.toList());

        log.info("✅ {} pixels encontrados para canvas: {}", pixelDTOs.size(), canvasId);

        return ResponseEntity.ok(pixelDTOs);
    }

    /**
     * GET /api/canvas/{canvasId}/pixel?x={x}&y={y}
     * Obtener un pixel específico por coordenadas
     */
    @GetMapping("/{canvasId}/pixel")
    public ResponseEntity<PixelDTO> getPixel(
            @PathVariable String canvasId,
            @RequestParam Integer x,
            @RequestParam Integer y) {

        log.info("🔍 Obteniendo pixel: canvas={}, x={}, y={}", canvasId, x, y);

        PixelView pixelView = pixelViewRepository.findByCanvasIdAndXAndY(canvasId, x, y)
                .orElseThrow(() -> new RuntimeException(
                        String.format("Pixel no encontrado: canvas=%s, x=%d, y=%d", canvasId, x, y)));

        return ResponseEntity.ok(toPixelDTO(pixelView));
    }

    /**
     * GET /api/canvas/{canvasId}/render
     * Renderizar canvas completo con matriz de colores
     */
    @GetMapping("/{canvasId}/render")
    public ResponseEntity<CanvasRenderDTO> renderCanvas(@PathVariable String canvasId) {
        log.info("🖼️ Renderizando canvas: {}", canvasId);

        // Obtener canvas
        CanvasView canvasView = canvasViewRepository.findById(canvasId)
                .orElseThrow(() -> new RuntimeException("Canvas no encontrado: " + canvasId));

        // Obtener pixels
        List<PixelView> pixelViews = pixelViewRepository.findByCanvasId(canvasId);

        // Crear matriz de colores
        String[][] grid = new String[canvasView.getHeight()][canvasView.getWidth()];

        // Inicializar con color de fondo
        for (int y = 0; y < canvasView.getHeight(); y++) {
            for (int x = 0; x < canvasView.getWidth(); x++) {
                grid[y][x] = canvasView.getBackgroundColor();
            }
        }

        // Aplicar pixels colocados
        for (PixelView pixel : pixelViews) {
            grid[pixel.getY()][pixel.getX()] = pixel.getColor();
        }

        // Calcular metadata
        int totalPixels = canvasView.getWidth() * canvasView.getHeight();
        int placedPixels = pixelViews.size();
        int emptyPixels = totalPixels - placedPixels;
        double coverage = (placedPixels * 100.0) / totalPixels;

        CanvasRenderDTO.RenderMetadata metadata = CanvasRenderDTO.RenderMetadata.builder()
                .totalPixelsPlaced(placedPixels)
                .emptyPixels(emptyPixels)
                .coveragePercentage(coverage)
                .build();

        CanvasRenderDTO renderDTO = CanvasRenderDTO.builder()
                .canvas(toCanvasDTO(canvasView))
                .grid(grid)
                .metadata(metadata)
                .build();

        log.info("✅ Canvas renderizado: {} - {}% cobertura", canvasId, String.format("%.2f", coverage));

        return ResponseEntity.ok(renderDTO);
    }

    /**
     * GET /api/canvas/search?name={name}
     * Buscar canvas por nombre
     */
    @GetMapping("/search")
    public ResponseEntity<List<CanvasDTO>> searchCanvas(@RequestParam String name) {
        log.info("🔍 Buscando canvas con nombre: {}", name);

        List<CanvasView> canvasViews = canvasViewRepository.findByNameContainingIgnoreCase(name);

        List<CanvasDTO> canvasDTOs = canvasViews.stream()
                .map(this::toCanvasDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(canvasDTOs);
    }

    // Helper methods

    private CanvasDTO toCanvasDTO(CanvasView canvasView) {
        long pixelCount = pixelViewRepository.countByCanvasId(canvasView.getCanvasId());

        return CanvasDTO.builder()
                .canvasId(canvasView.getCanvasId())
                .name(canvasView.getName())
                .width(canvasView.getWidth())
                .height(canvasView.getHeight())
                .backgroundColor(canvasView.getBackgroundColor())
                .createdBy(canvasView.getCreatedBy())
                .createdAt(canvasView.getCreatedAt())
                .updatedAt(canvasView.getUpdatedAt())
                .pixelCount(pixelCount)
                .build();
    }

    private PixelDTO toPixelDTO(PixelView pixelView) {
        return PixelDTO.builder()
                .pixelId(pixelView.getPixelId())
                .canvasId(pixelView.getCanvasId())
                .x(pixelView.getX())
                .y(pixelView.getY())
                .color(pixelView.getColor())
                .userId(pixelView.getUserId())
                .placedAt(pixelView.getPlacedAt())
                .build();
    }
}