package com.example.pixelplace.eventhandler;

import com.example.pixelplace.event.PixelPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * EventHandler que transmite eventos de pixeles via WebSocket.
 * 
 * Cuando se coloca un pixel, envía el evento a todos los clientes
 * suscritos al topic del canvas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventHandler {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Escucha eventos de pixeles colocados y los transmite via WebSocket.
     * 
     * Topic: /topic/canvas/{canvasId}
     */
    @EventHandler
    public void on(PixelPlacedEvent event) {
        log.info("📡 Broadcasting pixel event: canvas={}, pixel=({}, {}), color={}", 
                event.getCanvasId(), event.getX(), event.getY(), event.getColor());

        // Crear payload para WebSocket
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "PIXEL_PLACED");
        payload.put("canvasId", event.getCanvasId());
        payload.put("x", event.getX());
        payload.put("y", event.getY());
        payload.put("color", event.getColor());
        payload.put("userId", event.getUserId());
        payload.put("timestamp", System.currentTimeMillis());

        // Calcular tile al que pertenece el pixel
        int tileSize = 100;
        int tileX = event.getX() / tileSize;
        int tileY = event.getY() / tileSize;
        payload.put("tileX", tileX);
        payload.put("tileY", tileY);

        // Enviar a todos los clientes suscritos al canvas
        String destination = "/topic/canvas/" + event.getCanvasId();
        messagingTemplate.convertAndSend(destination, payload);

        log.debug("✅ Event broadcasted to {}", destination);
    }
}