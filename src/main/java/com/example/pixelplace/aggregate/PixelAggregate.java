package com.example.pixelplace.aggregate;

import com.example.pixelplace.command.PlacePixelCommand;
import com.example.pixelplace.domain.model.PixelUser;
import com.example.pixelplace.domain.valueobject.UserId;
import com.example.pixelplace.event.PixelPlacedEvent;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.spring.stereotype.Aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Aggregate que representa un Pixel individual en el canvas.
 *
 * ID del aggregate: "{canvasId}_{x}_{y}" (ejemplo: "canvas-123_30_50")
 *
 * Responsabilidades:
 * - Validar posición del pixel
 * - Validar color
 * - Mantener historial de usuarios que modificaron el pixel
 * 
 * CAMBIO: Rate limiting DESHABILITADO para permitir colocación libre de pixeles
 */
@Slf4j
@Aggregate
@NoArgsConstructor
public class PixelAggregate {

    @AggregateIdentifier
    private String pixelId;  // "{canvasId}_{x}_{y}"

    private String canvasId;
    private int x;
    private int y;
    private String color;
    private List<PixelUser> pixelUsers;

    // Configuración de rate limiting (DESHABILITADO - guardado para uso futuro)
    // private static final int MAX_PIXELS_PER_PERIOD = 3;
    // private static final int COOLDOWN_MINUTES = 5;

    @CommandHandler
    @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
    public void handle(PlacePixelCommand command) {
        log.info("🖌️ Colocando pixel en ({}, {}) - Color: {} - Usuario: {}",
                command.getX(), command.getY(),
                command.getColor(), command.getUserId());

        // Validar posición
        if (command.getX() < 0 || command.getY() < 0) {
            throw new IllegalArgumentException("Las coordenadas del pixel deben ser positivas");
        }

        // Validar color (formato hex)
        if (!isValidHexColor(command.getColor())) {
            throw new IllegalArgumentException("Color inválido. Use formato hex (#RRGGBB)");
        }

        // Validar userId
        if (command.getUserId() == null || command.getUserId().isBlank()) {
            throw new IllegalArgumentException("El userId no puede estar vacío");
        }

        // ========================================
        // RATE LIMITING DESHABILITADO
        // ========================================
        // Si quieres reactivarlo en el futuro, descomenta:
        // if (this.pixelUsers != null && !this.pixelUsers.isEmpty()) {
        //     validateUserRateLimit(UserId.of(command.getUserId()));
        // }
        // ========================================

        // Construir lista actualizada de pixelUsers
        List<PixelUser> updatedPixelUsers = new ArrayList<>();
        if (this.pixelUsers != null) {
            updatedPixelUsers.addAll(this.pixelUsers);
        }
        updatedPixelUsers.add(PixelUser.of(UserId.of(command.getUserId()), LocalDateTime.now()));

        // Aplicar evento
        AggregateLifecycle.apply(new PixelPlacedEvent(
                command.getPixelId(),
                command.getCanvasId(),
                command.getX(),
                command.getY(),
                command.getColor(),
                command.getUserId(),
                updatedPixelUsers
        ));
    }

    @EventSourcingHandler
    public void on(PixelPlacedEvent event) {
        log.info("✅ Pixel colocado: ({}, {}) - Color: {}",
                event.getX(), event.getY(), event.getColor());

        this.pixelId = event.getPixelId();
        this.canvasId = event.getCanvasId();
        this.x = event.getX();
        this.y = event.getY();
        this.color = event.getColor();
        this.pixelUsers = new ArrayList<>(event.getPixelUsers());
    }

    /**
     * Valida que el usuario no haya excedido el límite de pixeles.
     * 
     * ⚠️ ACTUALMENTE DESHABILITADO ⚠️
     * 
     * Regla: Un usuario no puede colocar más de MAX_PIXELS_PER_PERIOD pixeles
     * en los últimos COOLDOWN_MINUTES minutos.
     */
    /*
    private void validateUserRateLimit(UserId userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoffTime = now.minusMinutes(COOLDOWN_MINUTES);

        // Contar cuántos pixeles ha colocado el usuario recientemente
        long recentPixelCount = this.pixelUsers.stream()
                .filter(pu -> pu.getUserId().equals(userId))
                .filter(pu -> pu.getTimestamp().isAfter(cutoffTime))
                .count();

        if (recentPixelCount >= MAX_PIXELS_PER_PERIOD) {
            log.warn("⚠️ Usuario {} ha excedido el límite de pixeles ({}/{})",
                    userId, recentPixelCount, MAX_PIXELS_PER_PERIOD);

            throw new IllegalStateException(
                    String.format("Has excedido el límite de %d pixeles en %d minutos. Por favor espera.",
                            MAX_PIXELS_PER_PERIOD, COOLDOWN_MINUTES)
            );
        }
    }
    */

    /**
     * Valida formato de color hex (#RRGGBB o #RGB)
     */
    private boolean isValidHexColor(String color) {
        if (color == null) {
            return false;
        }

        // Formato: #RRGGBB o #RGB
        return color.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");
    }
}