# ============================================================
# Dockerfile - pixel-place
#
# IMPORTANTE: Esta librería depende de axon-kafka-spring-boot-starter
# publicada en mavenLocal (~/.m2). Docker no puede acceder al
# repositorio local del host, así que el JAR se compila ANTES
# de hacer docker build.
#
# PASOS OBLIGATORIOS antes de "docker build":
#   1. ./gradlew bootJar          (en la raíz del proyecto)
#   2. docker build ...           (recién ahora)
# ============================================================

FROM eclipse-temurin:17-jre

WORKDIR /app

# Metadatos del contenedor
LABEL app="pixel-place"
LABEL version="1.0.0"

# Crear usuario no-root para seguridad
RUN groupadd -r pixelplace && useradd -r -g pixelplace pixelplace

# Directorio para imágenes del canvas (se monta como PVC en K8s)
RUN mkdir -p /var/canvas-images && \
    chown -R pixelplace:pixelplace /var/canvas-images

# Copiar el JAR pre-compilado desde build/libs/
# (debe existir antes de correr docker build)
COPY build/libs/*.jar app.jar
RUN chown pixelplace:pixelplace app.jar

USER pixelplace

# Puerto de la aplicación
EXPOSE 8080

# ── Configuración JVM optimizada para contenedor ─────────────
# -XX:+UseContainerSupport     → JVM detecta límites del contenedor K8s
# -XX:MaxRAMPercentage=75.0    → Usa 75% de la RAM del contenedor como heap
# -XX:+UseG1GC                 → G1GC para baja latencia (pixel placement en tiempo real)
# -Dspring.profiles.active     → Activa el profile de Kubernetes
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-XX:+UseG1GC", \
            "-XX:MaxGCPauseMillis=100", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-Dspring.profiles.active=kubernetes", \
            "-jar", "app.jar"]