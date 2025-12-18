package com.example.pixelplace.domain.valueobject;

import lombok.Value;

import java.io.Serializable;
import java.util.UUID;

/**
 * Value Object para identificar un Canvas.
 */
@Value
public class CanvasId implements Serializable {

    String id;

    public CanvasId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("CanvasId no puede ser nulo o vacío");
        }
        this.id = id;
    }

    public static CanvasId generate() {
        return new CanvasId(UUID.randomUUID().toString());
    }

    public static CanvasId of(String id) {
        return new CanvasId(id);
    }

    @Override
    public String toString() {
        return id;
    }
}