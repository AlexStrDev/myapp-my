package com.example.pixelplace.api;

import com.example.pixelplace.command.CreateCanvasCommand;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Controller para comandos de Canvas (write operations).
 */
@Slf4j
@RestController
@RequestMapping("/api/canvas")
@RequiredArgsConstructor
public class CanvasController {

    private final CommandGateway commandGateway;

    /**
     * POST /api/canvas
     * Crear un nuevo canvas
     */
    @PostMapping
    public CompletableFuture<ResponseEntity<CreateCanvasResponse>> createCanvas(
            @RequestBody CreateCanvasRequest request) {

        log.info("🎨 Request para crear canvas: {}", request.getName());

        String canvasId = UUID.randomUUID().toString();

        CreateCanvasCommand command = new CreateCanvasCommand(
                canvasId,
                request.getName(),
                request.getWidth(),
                request.getHeight(),
                request.getBackgroundColor(),
                request.getCreatedBy()
        );

        return commandGateway.send(command)
                .thenApply(result -> {
                    log.info("✅ Canvas creado: {}", canvasId);

                    CreateCanvasResponse response = new CreateCanvasResponse(
                            canvasId,
                            request.getName(),
                            request.getWidth(),
                            request.getHeight(),
                            request.getBackgroundColor()
                    );

                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                })
                .exceptionally(ex -> {
                    log.error("❌ Error creando canvas", ex);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                });
    }

    // DTOs

    @Data
    public static class CreateCanvasRequest {
        private String name;
        private Integer width;
        private Integer height;
        private String backgroundColor;
        private String createdBy;
    }

    @Data
    @RequiredArgsConstructor
    public static class CreateCanvasResponse {
        private final String canvasId;
        private final String name;
        private final Integer width;
        private final Integer height;
        private final String backgroundColor;
    }
}