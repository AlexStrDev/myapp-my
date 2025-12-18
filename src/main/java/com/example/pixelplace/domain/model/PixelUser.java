package com.example.pixelplace.domain.model;

import com.example.pixelplace.domain.valueobject.UserId;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Modelo que representa un usuario que colocó un pixel y cuándo lo hizo
 */
@Getter
@EqualsAndHashCode
@ToString
public class PixelUser implements Serializable {

    private final UserId userId;
    private final LocalDateTime timestamp;

    /**
     * Constructor con anotaciones Jackson para deserialización
     * @param userId El ID del usuario
     * @param timestamp La fecha/hora en que se colocó el pixel
     */
    @JsonCreator
    public PixelUser(
            @JsonProperty("userId") UserId userId,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.userId = userId;
        this.timestamp = timestamp;
    }

    /**
     * Método estático para crear una instancia
     * @param userId El ID del usuario
     * @param timestamp La fecha/hora
     * @return Una nueva instancia de PixelUser
     */
    public static PixelUser of(UserId userId, LocalDateTime timestamp) {
        return new PixelUser(userId, timestamp);
    }
}