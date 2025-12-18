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

    @Modifying
    @Query(value = """
        INSERT INTO canvas_view (canvas_id, name, width, height, background_color, created_by, created_at, updated_at)
        VALUES (:canvasId, :name, :width, :height, :backgroundColor, :createdBy, :createdAt, :updatedAt)
        ON CONFLICT (canvas_id)
        DO UPDATE SET
            name = EXCLUDED.name,
            width = EXCLUDED.width,
            height = EXCLUDED.height,
            background_color = EXCLUDED.background_color,
            created_by = EXCLUDED.created_by,
            updated_at = EXCLUDED.updated_at
        """, nativeQuery = true)
    void upsertCanvas(
            @Param("canvasId") String canvasId,
            @Param("name") String name,
            @Param("width") Integer width,
            @Param("height") Integer height,
            @Param("backgroundColor") String backgroundColor,
            @Param("createdBy") String createdBy,
            @Param("createdAt") Instant createdAt,
            @Param("updatedAt") Instant updatedAt
    );

    List<CanvasView> findByNameContainingIgnoreCase(String name);

    List<CanvasView> findAllByOrderByCreatedAtDesc();

    List<CanvasView> findByCreatedBy(String userId);

    boolean existsByName(String name);

    List<CanvasView> findByWidthAndHeight(Integer width, Integer height);

    List<CanvasView> findByCreatedAtAfter(Instant after);
}