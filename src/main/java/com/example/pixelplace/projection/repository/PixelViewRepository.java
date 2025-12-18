package com.example.pixelplace.projection.repository;

import com.example.pixelplace.projection.entity.PixelView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PixelViewRepository extends JpaRepository<PixelView, String> {

    @Modifying
    @Query(value = """
        INSERT INTO pixel_view (pixel_id, canvas_id, x, y, color, user_id, placed_at)
        VALUES (:pixelId, :canvasId, :x, :y, :color, :userId, :placedAt)
        ON CONFLICT (pixel_id)
        DO UPDATE SET
            canvas_id = EXCLUDED.canvas_id,
            x = EXCLUDED.x,
            y = EXCLUDED.y,
            color = EXCLUDED.color,
            user_id = EXCLUDED.user_id,
            placed_at = EXCLUDED.placed_at
        """, nativeQuery = true)
    void upsertPixel(
            @Param("pixelId") String pixelId,
            @Param("canvasId") String canvasId,
            @Param("x") int x,
            @Param("y") int y,
            @Param("color") String color,
            @Param("userId") String userId,
            @Param("placedAt") Instant placedAt
    );

    List<PixelView> findByCanvasId(String canvasId);

    Optional<PixelView> findByCanvasIdAndXAndY(String canvasId, Integer x, Integer y);

    List<PixelView> findByCanvasIdAndXBetweenAndYBetween(
            String canvasId, int xStart, int xEnd, int yStart, int yEnd);

    long countByCanvasId(String canvasId);

    List<PixelView> findByUserId(String userId);

    List<PixelView> findTop100ByCanvasIdOrderByPlacedAtDesc(String canvasId);
}