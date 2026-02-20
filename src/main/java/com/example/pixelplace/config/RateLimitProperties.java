package com.example.pixelplace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de rate limiting para pixeles.
 * 
 * Actualmente DESHABILITADO pero disponible para uso futuro.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "pixel-place.rate-limit")
public class RateLimitProperties {

    /**
     * Habilitar/deshabilitar rate limiting globalmente
     */
    private boolean enabled = false;

    /**
     * Máximo de pixeles que un usuario puede colocar en el periodo
     */
    private int maxPixelsPerPeriod = 3;

    /**
     * Periodo de cooldown en minutos
     */
    private int cooldownMinutes = 5;

    /**
     * Permitir bypass del rate limit para usuarios específicos (IDs separados por coma)
     */
    private String bypassUsers = "";
}