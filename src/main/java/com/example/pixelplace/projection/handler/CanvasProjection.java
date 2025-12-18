package com.example.pixelplace.projection.handler;

import com.example.pixelplace.event.CanvasCreatedEvent;
import com.example.pixelplace.projection.entity.CanvasView;
import com.example.pixelplace.projection.repository.CanvasViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Projection Handler para Canvas.
 *
 * Escucha eventos de Canvas y materializa en canvas_view.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasProjection {

    private final CanvasViewRepository canvasViewRepository;

    /**
     * Maneja el evento CanvasCreatedEvent y crea la vista materializada.
     */
    @EventHandler
    @Transactional
    public void on(CanvasCreatedEvent event) {
        log.info("📊 Materializando Canvas: {}", event.getCanvasId());

        try {
            CanvasView canvasView = CanvasView.builder()
                    .canvasId(event.getCanvasId())
                    .name(event.getName())
                    .width(event.getWidth())
                    .height(event.getHeight())
                    .backgroundColor(event.getBackgroundColor())
                    .createdBy(event.getCreatedBy())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            canvasViewRepository.save(canvasView);

            log.info("✅ Canvas materializado: {} - {} ({}x{})",
                    event.getCanvasId(), event.getName(), event.getWidth(), event.getHeight());

        } catch (Exception e) {
            log.error("💥 Error materializando Canvas: {}", event.getCanvasId(), e);
            throw e;
        }
    }
}