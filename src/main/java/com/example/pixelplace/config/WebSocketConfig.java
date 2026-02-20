package com.example.pixelplace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuración de WebSocket para eventos en tiempo real.
 * 
 * Permite a los clientes conectarse vía WebSocket y recibir
 * notificaciones de pixeles colocados.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Habilitar un broker de mensajes simple en memoria
        // Los clientes se suscriben a /topic/*
        config.enableSimpleBroker("/topic");
        
        // Los mensajes enviados al servidor usan prefijo /app
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket en /ws
        registry.addEndpoint("/ws")
                // CORS: Permitir conexiones desde el frontend
                // PRODUCCIÓN: Cambiar a dominio específico
                .setAllowedOriginPatterns(
                    "http://localhost:3000",
                    "http://localhost:5173",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:5173"
                )
                // SockJS fallback para navegadores que no soportan WebSocket
                .withSockJS();
    }
}