package com.example.pixelplace.service;

import com.example.pixelplace.dto.CanvasState;
import com.example.pixelplace.dto.PixelState;
import com.example.pixelplace.event.CanvasCreatedEvent;
import com.example.pixelplace.event.PixelPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio que reconstruye el estado del canvas desde el EventStore
 */
@Slf4j
@Service
public class CanvasStateProjection {

    private final EventStore eventStore;
    private final JdbcTemplate jdbcTemplate;
    private final ConcurrentHashMap<String, CanvasState> canvasCache;
    
    public CanvasStateProjection(EventStore eventStore, JdbcTemplate jdbcTemplate) {
        this.eventStore = eventStore;
        this.jdbcTemplate = jdbcTemplate;
        this.canvasCache = new ConcurrentHashMap<>();
    }

    /**
     * Reconstruye el estado completo de un canvas desde eventos
     */
    public CanvasState rebuildCanvasState(String canvasId) {
        log.info("🔄 Reconstruyendo canvas {} desde EventStore", canvasId);
        
        // Verificar cache primero
        CanvasState cached = canvasCache.get(canvasId);
        if (cached != null) {
            log.debug("✅ Canvas {} encontrado en cache con {} pixeles", 
                    canvasId, cached.getPixelCount());
            return cached;
        }
        
        CanvasState state = new CanvasState();
        state.setCanvasId(canvasId);
        
        try {
            eventStore.readEvents(canvasId).asStream()
                    .forEach(domainEventMessage -> processEvent(state, domainEventMessage));
            
            if (state.getWidth() == null || state.getHeight() == null) {
                throw new IllegalStateException("Canvas no encontrado o sin dimensiones: " + canvasId);
            }
            
            log.info("📏 Canvas metadata: {}x{}, fondo={}", 
                    state.getWidth(), state.getHeight(), state.getBackgroundColor());
            
            String sql = "SELECT DISTINCT aggregate_identifier FROM domain_event_entry " +
                         "WHERE aggregate_identifier LIKE ? AND aggregate_identifier != ?";
            
            String prefix = canvasId + "_%";
            
            List<String> pixelAggregateIds = jdbcTemplate.queryForList(
                    sql, 
                    String.class, 
                    prefix,
                    canvasId 
            );
            
            log.info("🔍 Encontrados {} pixeles en EventStore", pixelAggregateIds.size());

            for (String pixelId : pixelAggregateIds) {
                try {
                    eventStore.readEvents(pixelId).asStream()
                            .forEach(domainEventMessage -> processEvent(state, domainEventMessage));
                } catch (Exception e) {
                    log.warn("⚠️ Error leyendo pixel {}: {}", pixelId, e.getMessage());
                }
            }
            
            log.info("✅ Canvas reconstruido: {} pixeles", state.getPixelCount());
            
        } catch (Exception e) {
            log.error("❌ Error reconstruyendo canvas: {}", canvasId, e);
            throw new IllegalStateException("Error reconstruyendo canvas: " + canvasId, e);
        }
        
        // Cachear
        canvasCache.put(canvasId, state);
        
        return state;
    }
    
    /**
     * Reconstruye solo los pixeles de un canvas (sin metadata)
     * Útil cuando solo necesitas los pixeles
     */
    public CanvasState rebuildPixelsOnly(String canvasId) {
        CanvasState state = canvasCache.get(canvasId);
        if (state != null) {
            return state;
        }
        return rebuildCanvasState(canvasId);
    }
    
    /**
     * Procesa un evento individual y actualiza el estado
     */
    private void processEvent(CanvasState state, DomainEventMessage<?> eventMessage) {
        Object payload = eventMessage.getPayload();
        
        if (payload instanceof CanvasCreatedEvent) {
            CanvasCreatedEvent event = (CanvasCreatedEvent) payload;
            state.setName(event.getName());
            state.setWidth(event.getWidth());
            state.setHeight(event.getHeight());
            state.setBackgroundColor(event.getBackgroundColor());
            
            log.debug("📝 Canvas metadata: {}x{} - {}", 
                    event.getWidth(), event.getHeight(), event.getName());
        } 
        else if (payload instanceof PixelPlacedEvent) {
            PixelPlacedEvent event = (PixelPlacedEvent) payload;
            state.setPixel(
                    event.getX(), 
                    event.getY(), 
                    event.getColor(), 
                    event.getUserId(),
                    eventMessage.getTimestamp().toEpochMilli()
            );
            
            log.debug("🎨 Pixel cargado: ({}, {}) - {}", 
                    event.getX(), event.getY(), event.getColor());
        }
    }
    
    /**
     * Invalida el cache de un canvas específico
     */
    public void invalidateCache(String canvasId) {
        canvasCache.remove(canvasId);
        log.info("🗑️ Cache invalidado para canvas: {}", canvasId);
    }
    
    /**
     * Limpia todo el cache
     */
    public void clearCache() {
        canvasCache.clear();
        log.info("🗑️ Cache completo limpiado");
    }
    
    /**
     * Obtiene estadísticas del cache
     */
    public String getCacheStats() {
        return String.format("Cache: %d canvas cacheados", canvasCache.size());
    }
}