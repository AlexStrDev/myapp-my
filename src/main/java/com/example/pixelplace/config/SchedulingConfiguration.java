package com.example.pixelplace.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuración para habilitar tareas programadas (@Scheduled).
 * 
 * Necesario para el procesamiento de batches por tiempo.
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {
}