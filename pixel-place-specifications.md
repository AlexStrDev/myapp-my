# Especificaciones Funcionales - Réplica de r/place

## 1. Descripción General

Sistema colaborativo de arte digital donde múltiples usuarios pueden colocar píxeles de colores en un canvas compartido, con limitaciones de tiempo para fomentar la colaboración y estrategia.

**Objetivo**: Crear una prueba de concepto funcional de un canvas colectivo similar a r/place de Reddit.

## 2. Características Principales

### 2.1 Canvas Compartido
- Tamaño: 1000x1000 píxeles (ajustable según recursos)
- Estado inicial: Canvas completamente blanco
- Actualización en tiempo real para todos los usuarios conectados

### 2.2 Paleta de Colores
- 16 colores predefinidos:
  - Blanco (#FFFFFF)
  - Negro (#000000)
  - Gris (#888888)
  - Rojo (#E50000)
  - Naranja (#FF8B00)
  - Amarillo (#FFD635)
  - Verde (#00A368)
  - Verde oscuro (#00756F)
  - Azul claro (#00CCC0)
  - Azul (#0083C7)
  - Azul oscuro (#0000EA)
  - Púrpura (#820080)
  - Rosa (#FF99AA)
  - Rosado fuerte (#FF3881)
  - Marrón (#6D482F)
  - Beige (#FFD8B1)

## 3. Reglas del Sistema

### 3.1 Limitación de Colocación
- **Cooldown**: 5 minutos entre cada píxel por usuario
- Cada usuario puede colocar 1 píxel por turno
- El temporizador se reinicia después de colocar un píxel
- Indicador visual del tiempo restante para el próximo píxel

### 3.2 Identificación de Usuarios
- Los usuarios deben estar autenticados (login simple)
- Sistema de sesiones para rastrear cooldowns individuales
- Historial de últimos 10 píxeles colocados por usuario

### 3.3 Persistencia
- El canvas se guarda cada 30 segundos
- Historial completo de cambios (quién, cuándo, dónde, qué color)
- Capacidad de exportar el canvas como imagen PNG

## 4. Funcionalidades del Usuario

### 4.1 Interacción con el Canvas
- **Zoom**: Niveles 1x, 2x, 5x, 10x
- **Pan**: Navegación con click y arrastrar
- **Selector de color**: Panel con la paleta disponible
- **Cursor**: Indicador visual del píxel que se va a colocar
- **Preview**: Vista previa del color antes de confirmar

### 4.2 Información en Pantalla
- Contador de cooldown personal
- Coordenadas del cursor (X, Y)
- Color seleccionado actualmente
- Mini-mapa de ubicación en el canvas

### 4.3 Historial y Actividad
- Feed en vivo de últimos píxeles colocados (últimos 50)
- Información por píxel: usuario, timestamp, coordenadas
- Vista de "time-lapse" opcional (reproducción acelerada de cambios)

## 5. Especificaciones Técnicas Básicas

### 5.1 Frontend
- Canvas HTML5 para renderizado
- WebSocket para actualizaciones en tiempo real
- Interfaz responsiva (desktop prioritario)

### 5.2 Backend
- API REST para operaciones CRUD
- WebSocket server para broadcast de cambios
- Base de datos para:
  - Estado actual del canvas (array de píxeles)
  - Historial de cambios
  - Usuarios y sus cooldowns

### 5.3 Datos a Almacenar

**Píxel**:
```json
{
  "x": 0,
  "y": 0,
  "color": "#FFFFFF",
  "userId": "user123",
  "timestamp": "2024-01-01T12:00:00Z"
}
```

**Usuario**:
```json
{
  "userId": "user123",
  "username": "artista_digital",
  "lastPixelTime": "2024-01-01T12:00:00Z",
  "pixelsPlaced": 42
}
```

## 6. Reglas Adicionales

### 6.1 Moderación Básica
- Límite de 1 cuenta por dirección IP (básico)
- Capacidad de banear usuarios por comportamiento abusivo
- Sistema de reportes (futuro)

### 6.2 Límites del Sistema
- Máximo 1000 usuarios concurrentes (POC)
- Rate limiting: 10 requests/segundo por usuario
- Timeout de sesión: 24 horas de inactividad

## 7. Casos de Uso Principales

### 7.1 Colocar un Píxel
1. Usuario selecciona un color de la paleta
2. Usuario hace click en el canvas en las coordenadas deseadas
3. Sistema verifica que el cooldown haya expirado
4. Sistema actualiza el píxel y lo broadcast a todos
5. Sistema inicia el cooldown de 5 minutos

### 7.2 Ver Actividad Reciente
1. Usuario accede al panel de actividad
2. Sistema muestra últimos 50 cambios
3. Usuario puede hacer click en un cambio para ver ubicación en canvas

### 7.3 Navegar el Canvas
1. Usuario usa zoom para acercarse/alejarse
2. Usuario arrastra para moverse por el canvas
3. Sistema actualiza el mini-mapa en tiempo real

## 8. Exclusiones (Fuera del Alcance - POC)

- Sistema de equipos/facciones
- Chat integrado
- Notificaciones push
- Aplicación móvil nativa
- Sistema de logros/badges
- API pública para bots
- Moderación automática avanzada

## 9. Métricas de Éxito

- Canvas funcional con al menos 100 usuarios simultáneos
- Latencia < 500ms para actualización de píxeles
- 99% uptime durante período de prueba
- Historial completo sin pérdida de datos

## 10. Fases de Implementación

### Fase 1: MVP Básico (2 semanas)
- Canvas estático con colocación manual
- Sistema de cooldown básico
- Persistencia simple

### Fase 2: Tiempo Real (1 semana)
- WebSocket para actualizaciones
- Optimización de renderizado

### Fase 3: Pulimiento (1 semana)
- UI/UX mejorada
- Historial y time-lapse
- Exportación de imagen

---

**Versión**: 1.0  
**Fecha**: Diciembre 2024  
**Estado**: Prueba de Concepto