package com.example.pixelplace.projection.handler;

import com.example.pixelplace.event.CanvasCreatedEvent;
import com.example.pixelplace.event.PixelPlacedEvent;
import com.example.pixelplace.projection.repository.CanvasViewRepository;
import com.example.pixelplace.projection.repository.PixelViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class PixelProjection {

    private final PixelViewRepository pixelViewRepository;
    private final CanvasViewRepository canvasViewRepository;

    @EventHandler
    @Transactional
    public void on(CanvasCreatedEvent event) {
        log.info("Materializando Canvas: {} - Dimensiones: {}x{}",
                event.getCanvasId(), event.getWidth(), event.getHeight());

        try {
            Instant now = Instant.now();
            canvasViewRepository.upsertCanvas(
                    event.getCanvasId(),
                    event.getName(),
                    event.getWidth(),
                    event.getHeight(),
                    event.getBackgroundColor(),
                    event.getCreatedBy(),
                    now,
                    now
            );

            log.info("Canvas materializado: {}", event.getCanvasId());
        } catch (Exception e) {
            log.error("Error materializando Canvas: {}", event.getCanvasId(), e);
            throw e;
        }
    }

    @EventHandler
    @Transactional
    public void on(PixelPlacedEvent event) {
        String pixelId = event.getX() + "_" + event.getY();

        log.info("Materializando Pixel: {} en ({}, {}) - Color: {}",
                pixelId, event.getX(), event.getY(), event.getColor());

        try {
            pixelViewRepository.upsertPixel(
                    pixelId,
                    event.getCanvasId(),
                    event.getX(),
                    event.getY(),
                    event.getColor(),
                    event.getUserId(),
                    Instant.now()
            );

            log.info("Pixel materializado: {}", pixelId);
        } catch (Exception e) {
            log.error("Error materializando Pixel: {}", pixelId, e);
            throw e;
        }
    }
}