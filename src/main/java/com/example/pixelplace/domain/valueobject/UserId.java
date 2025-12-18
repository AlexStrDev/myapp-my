package com.example.pixelplace.domain.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.io.Serializable;

/**
 * Value Object que representa un identificador de usuario
 */
@Getter
@EqualsAndHashCode
@ToString
public class UserId implements Serializable {

    private final String id;

    /**
     * Constructor con anotación @JsonCreator para que Jackson pueda deserializar
     * @param id El identificador del usuario
     */
    @JsonCreator
    public UserId(@JsonProperty("id") String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID de usuario no puede estar vacío");
        }
        this.id = id;
    }

    /**
     * Método estático para crear una instancia de UserId
     * @param id El identificador del usuario
     * @return Una nueva instancia de UserId
     */
    public static UserId of(String id) {
        return new UserId(id);
    }
}