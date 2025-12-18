package com.example.pixelplace.projection.repository;

import com.example.pixelplace.projection.entity.CanvasView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface CanvasViewRepository extends JpaRepository<CanvasView, String> {

    /**
     * UPSERT para materializar canvas.
     *
     * ON CONFLICT en PRIMARY KEY (canvas_id) para evitar errores de constraint.
     * Si el canvas_id ya existe, actualiza width, height y created_at.
     * Si no existe, inserta nuevo registro.
     */
    @Modifying
    @Query(value = """
        INSERT INTO canvas_view (canvas_id, width, height, created_at)
        VALUES (:canvasId, :width, :height, :createdAt)
        ON CONFLICT (canvas_id)
        DO UPDATE SET
            width = EXCLUDED.width,
            height = EXCLUDED.height,
            created_at = EXCLUDED.created_at
        """, nativeQuery = true)
    void upsertCanvas(
            @Param("canvasId") String canvasId,
            @Param("width") Integer width,
            @Param("height") Integer height,
            @Param("createdAt") Instant createdAt
    );

    List<CanvasView> findByNameContainingIgnoreCase(String name);

    List<CanvasView> findAllByOrderByCreatedAtDesc();

    List<CanvasView> findByCreatedBy(String userId);

    boolean existsByName(String name);
}