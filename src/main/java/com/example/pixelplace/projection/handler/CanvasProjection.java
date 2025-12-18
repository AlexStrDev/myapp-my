package com.example.pixelplace.projection.handler;

import com.example.pixelplace.event.CanvasCreatedEvent;
import com.example.pixelplace.projection.entity.CanvasView;
import com.example.pixelplace.projection.repository.CanvasViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class CanvasProjection {

    private final CanvasViewRepository canvasViewRepository;

    @EventHandler
    @Transactional
    public void on(CanvasCreatedEvent event) {
        log.info("📊 Materializando Canvas: {}", event.getCanvasId());

        try {
            Instant now = Instant.now();

            CanvasView canvasView = CanvasView.builder()
                    .canvasId(event.getCanvasId())
                    .name(event.getName())
                    .width(event.getWidth())
                    .height(event.getHeight())
                    .backgroundColor(event.getBackgroundColor())
                    .createdBy(event.getCreatedBy())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            canvasViewRepository.save(canvasView);

            log.info("Canvas materializado: {} - {} ({}x{})",
                    event.getCanvasId(), event.getName(), event.getWidth(), event.getHeight());

        } catch (Exception e) {
            log.error("Error materializando Canvas: {}", event.getCanvasId(), e);
            throw e;
        }
    }
}