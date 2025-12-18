-- ===============================================
-- Script SQL para Vistas Materializadas
-- Pixel Place - Canvas + Pixels
-- ===============================================

-- -----------------------------------------------
-- Tabla: canvas_view
-- Propósito: Almacenar metadata de canvas
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS canvas_view (
    canvas_id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    background_color VARCHAR(7) NOT NULL,
    created_by VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Índices para canvas_view
CREATE INDEX IF NOT EXISTS idx_canvas_name ON canvas_view(name);
CREATE INDEX IF NOT EXISTS idx_canvas_created_at ON canvas_view(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_canvas_created_by ON canvas_view(created_by);

-- -----------------------------------------------
-- Tabla: pixel_view
-- Propósito: Almacenar estado actual de cada pixel
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS pixel_view (
    pixel_id VARCHAR(255) PRIMARY KEY,
    canvas_id VARCHAR(255) NOT NULL,
    x INT NOT NULL,
    y INT NOT NULL,
    color VARCHAR(7) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    placed_at TIMESTAMP WITH TIME ZONE NOT NULL,

    -- Foreign key con CASCADE DELETE
    CONSTRAINT fk_pixel_canvas FOREIGN KEY (canvas_id)
        REFERENCES canvas_view(canvas_id) ON DELETE CASCADE,

    -- Constraint único: un solo pixel por posición en cada canvas
    CONSTRAINT unique_pixel_position UNIQUE (canvas_id, x, y)
);

-- Índices para pixel_view
CREATE INDEX IF NOT EXISTS idx_pixel_canvas ON pixel_view(canvas_id);
CREATE INDEX IF NOT EXISTS idx_pixel_coords ON pixel_view(canvas_id, x, y);
CREATE INDEX IF NOT EXISTS idx_pixel_user ON pixel_view(user_id);
CREATE INDEX IF NOT EXISTS idx_pixel_placed_at ON pixel_view(placed_at DESC);

-- -----------------------------------------------
-- Comentarios en las tablas
-- -----------------------------------------------
COMMENT ON TABLE canvas_view IS 'Vista materializada de Canvas para queries eficientes';
COMMENT ON TABLE pixel_view IS 'Vista materializada de Pixels con UPSERT para actualizaciones';

COMMENT ON COLUMN canvas_view.canvas_id IS 'ID único del canvas (UUID)';
COMMENT ON COLUMN canvas_view.name IS 'Nombre del canvas';
COMMENT ON COLUMN canvas_view.width IS 'Ancho del canvas en pixels';
COMMENT ON COLUMN canvas_view.height IS 'Altura del canvas en pixels';
COMMENT ON COLUMN canvas_view.background_color IS 'Color de fondo en formato hex (#RRGGBB)';
COMMENT ON COLUMN canvas_view.created_at IS 'Timestamp UTC de creación';
COMMENT ON COLUMN canvas_view.updated_at IS 'Timestamp UTC de última actualización';

COMMENT ON COLUMN pixel_view.pixel_id IS 'ID del pixel en formato {x}_{y}';
COMMENT ON COLUMN pixel_view.canvas_id IS 'ID del canvas al que pertenece';
COMMENT ON COLUMN pixel_view.x IS 'Coordenada X del pixel';
COMMENT ON COLUMN pixel_view.y IS 'Coordenada Y del pixel';
COMMENT ON COLUMN pixel_view.color IS 'Color del pixel en formato hex (#RRGGBB)';
COMMENT ON COLUMN pixel_view.user_id IS 'ID del usuario que colocó el pixel';
COMMENT ON COLUMN pixel_view.placed_at IS 'Timestamp UTC de cuándo se colocó el pixel';