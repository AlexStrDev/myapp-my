package com.example.pixelplace.aggregate;

import com.example.pixelplace.command.CreateCanvasCommand;
import com.example.pixelplace.event.CanvasCreatedEvent;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;

/**
 * Aggregate que representa un Canvas.
 *
 * Responsabilidades:
 * - Crear canvas con validación
 * - Validar dimensiones y nombre
 * - Mantener metadata del canvas
 */
@Slf4j
@Aggregate
@NoArgsConstructor
public class CanvasAggregate {

    @AggregateIdentifier
    private String canvasId;

    private String name;
    private Integer width;
    private Integer height;
    private String backgroundColor;
    private String createdBy;

    // Límites de validación
    private static final int MIN_WIDTH = 10;
    private static final int MAX_WIDTH = 1000;
    private static final int MIN_HEIGHT = 10;
    private static final int MAX_HEIGHT = 1000;

    @CommandHandler
    public CanvasAggregate(CreateCanvasCommand command) {
        log.info("Creando canvas: {} ({}x{}) - Color: {}",
                command.getName(), command.getWidth(), command.getHeight(), command.getBackgroundColor());

        // Validar nombre
        if (command.getName() == null || command.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del canvas no puede estar vacío");
        }

        if (command.getName().length() > 255) {
            throw new IllegalArgumentException("El nombre del canvas no puede exceder 255 caracteres");
        }

        // Validar dimensiones
        if (command.getWidth() == null || command.getHeight() == null) {
            throw new IllegalArgumentException("Las dimensiones del canvas no pueden ser nulas");
        }

        if (command.getWidth() < MIN_WIDTH || command.getWidth() > MAX_WIDTH) {
            throw new IllegalArgumentException(
                    String.format("El ancho debe estar entre %d y %d pixels", MIN_WIDTH, MAX_WIDTH));
        }

        if (command.getHeight() < MIN_HEIGHT || command.getHeight() > MAX_HEIGHT) {
            throw new IllegalArgumentException(
                    String.format("La altura debe estar entre %d y %d pixels", MIN_HEIGHT, MAX_HEIGHT));
        }

        // Validar color de fondo
        if (command.getBackgroundColor() == null || !isValidHexColor(command.getBackgroundColor())) {
            throw new IllegalArgumentException("Color de fondo inválido. Use formato hex (#RRGGBB)");
        }

        // Emitir evento
        AggregateLifecycle.apply(new CanvasCreatedEvent(
                command.getCanvasId(),
                command.getName(),
                command.getWidth(),
                command.getHeight(),
                command.getBackgroundColor(),
                command.getCreatedBy()
        ));
    }

    @EventSourcingHandler
    public void on(CanvasCreatedEvent event) {
        log.info("Canvas creado: {} - {} ({}x{})",
                event.getCanvasId(), event.getName(), event.getWidth(), event.getHeight());

        this.canvasId = event.getCanvasId();
        this.name = event.getName();
        this.width = event.getWidth();
        this.height = event.getHeight();
        this.backgroundColor = event.getBackgroundColor();
        this.createdBy = event.getCreatedBy();
    }

    /**
     * Valida formato de color hex (#RRGGBB o #RGB)
     */
    private boolean isValidHexColor(String color) {
        if (color == null) {
            return false;
        }
        return color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }
}