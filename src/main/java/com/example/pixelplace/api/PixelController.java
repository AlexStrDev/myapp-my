package com.example.pixelplace.api;

import com.example.pixelplace.command.PlacePixelCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

/**
 * Controller REST para operaciones de Pixel.
 */
@Slf4j
@RestController
@RequestMapping("/api/pixels")
public class PixelController {

    private final CommandGateway commandGateway;

    public PixelController(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
    }

    /**
     * Coloca/actualiza un pixel en el canvas.
     *
     * POST /api/pixels
     * {
     *   "canvasId": "uuid",
     *   "x": 30,
     *   "y": 50,
     *   "color": "#FF5733",
     *   "userId": "user123"
     * }
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<PixelResponse>> placePixel(
            @Valid @RequestBody PlacePixelRequest request) {

        log.info("📨 Solicitud de colocar pixel: ({}, {}) - Color: {} - Usuario: {}",
                request.getX(), request.getY(), request.getColor(), request.getUserId());

        // Generar pixelId: "{x}_{y}"
        String pixelId = String.format("%d_%d", request.getX(), request.getY());

        PlacePixelCommand command = new PlacePixelCommand(
                pixelId,
                request.getCanvasId(),
                request.getX(),
                request.getY(),
                request.getColor().toUpperCase(),
                request.getUserId()
        );

        return commandGateway.send(command)
                .thenApply(result -> {
                    log.info("✅ Pixel colocado: ({}, {})", request.getX(), request.getY());

                    return ResponseEntity.ok(new PixelResponse(
                            pixelId,
                            "Pixel colocado exitosamente",
                            request.getCanvasId(),
                            request.getX(),
                            request.getY(),
                            request.getColor().toUpperCase()
                    ));
                })
                .exceptionally(throwable -> {
                    log.error("❌ Error colocando pixel", throwable);

                    String errorMessage = throwable.getCause() != null ?
                            throwable.getCause().getMessage() :
                            throwable.getMessage();

                    return ResponseEntity.badRequest().body(new PixelResponse(
                            null,
                            "Error: " + errorMessage,
                            null,
                            0,
                            0,
                            null
                    ));
                });
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlacePixelRequest {

        @NotBlank(message = "El canvasId no puede estar vacío")
        private String canvasId;

        @Min(value = 0, message = "La coordenada X debe ser positiva")
        private int x;

        @Min(value = 0, message = "La coordenada Y debe ser positiva")
        private int y;

        @NotBlank(message = "El color no puede estar vacío")
        @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$",
                message = "El color debe estar en formato hex (#RRGGBB o #RGB)")
        private String color;

        @NotBlank(message = "El userId no puede estar vacío")
        private String userId;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PixelResponse {
        private String pixelId;
        private String message;
        private String canvasId;
        private int x;
        private int y;
        private String color;
    }
}