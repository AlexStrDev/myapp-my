package com.example.pixelplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

/**
 * Configuración CORS para permitir peticiones desde el frontend.
 * 
 * IMPORTANTE: Esta configuración es para DESARROLLO.
 * En producción, restringe los orígenes permitidos.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Permitir credenciales (cookies, headers de auth, etc.)
        config.setAllowCredentials(true);

        // Permitir estos orígenes (frontend en desarrollo)
        // PRODUCCIÓN: Cambia esto a tu dominio real
        config.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",
            "http://localhost:5173",  // Vite default port
            "http://127.0.0.1:3000",
            "http://127.0.0.1:5173"
        ));

        // Permitir todos los headers
        config.setAllowedHeaders(Collections.singletonList("*"));

        // Permitir todos los métodos HTTP
        config.setAllowedMethods(Arrays.asList(
            "GET", 
            "POST", 
            "PUT", 
            "DELETE", 
            "PATCH", 
            "OPTIONS"
        ));

        // Exponer headers para que el frontend pueda leerlos
        config.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "X-Requested-With",
            "accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));

        // Tiempo que el navegador puede cachear la respuesta preflight
        config.setMaxAge(3600L);

        // Aplicar configuración a todas las rutas
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}