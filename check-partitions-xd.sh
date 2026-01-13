#!/bin/bash

# ============================================
# Script para Verificar Distribución de Particiones
# Pixel Place - Monitoring (Windows Compatible)
# ============================================

# NO usar set -e para que no se cierre al primer error

BOOTSTRAP_SERVER="localhost:9092"
KAFKA_CONTAINER="pixel-place-kafka"
COMMAND_TOPIC="pixel-place-commands"
EVENT_TOPIC="pixel-place-events"

echo "================================================================"
echo "    PIXEL PLACE - ANALISIS DE PARTICIONES"
echo "================================================================"
echo ""

# Verificar que Docker esté corriendo
echo "[1/5] Verificando Docker..."
if ! docker ps > /dev/null 2>&1; then
    echo "ERROR: Docker no esta corriendo o no tienes permisos"
    echo ""
    echo "Soluciones:"
    echo "  1. Inicia Docker Desktop"
    echo "  2. Ejecuta este script como Administrador"
    echo ""
    read -p "Presiona Enter para salir..."
    exit 1
fi
echo "OK - Docker esta corriendo"
echo ""

# Verificar que el contenedor de Kafka exista
echo "[2/5] Verificando contenedor de Kafka..."
if ! docker ps --format '{{.Names}}' | grep -q "$KAFKA_CONTAINER"; then
    echo "ERROR: Contenedor $KAFKA_CONTAINER no esta corriendo"
    echo ""
    echo "Contenedores disponibles:"
    docker ps --format "  - {{.Names}}"
    echo ""
    read -p "Presiona Enter para salir..."
    exit 1
fi
echo "OK - Contenedor encontrado"
echo ""

# Describir topic de comandos
echo "[3/5] Analizando topic de comandos..."
echo "================================================================"
docker exec $KAFKA_CONTAINER kafka-topics \
    --bootstrap-server $BOOTSTRAP_SERVER \
    --describe --topic $COMMAND_TOPIC 2>&1

if [ $? -ne 0 ]; then
    echo ""
    echo "ERROR: No se pudo describir el topic"
    echo "El topic probablemente no existe aun"
    echo ""
    read -p "Presiona Enter para salir..."
    exit 1
fi
echo ""

# Obtener offsets por partición
echo "[4/5] Obteniendo distribucion de mensajes..."
echo "================================================================"
echo ""

docker exec $KAFKA_CONTAINER kafka-run-class kafka.tools.GetOffsetShell \
    --broker-list $BOOTSTRAP_SERVER \
    --topic $COMMAND_TOPIC \
    --time -1 2>&1 | while IFS=: read -r partition offset; do
    
    # Extraer numero de particion
    partition_num=$(echo "$partition" | grep -o '[0-9]*$')
    
    if [ -n "$offset" ] && [ "$offset" -gt 0 ]; then
        printf "  Particion %3s: %5s mensajes\n" "$partition_num" "$offset"
    fi
done

echo ""

# Ver routing keys
echo "[5/5] Analizando routing keys (ultimos 50 comandos)..."
echo "================================================================"
echo ""

# Timeout de 5 segundos para evitar que se quede colgado
docker exec $KAFKA_CONTAINER timeout 5s kafka-console-consumer \
    --bootstrap-server $BOOTSTRAP_SERVER \
    --topic $COMMAND_TOPIC \
    --from-beginning \
    --max-messages 50 \
    --property print.key=true \
    --timeout-ms 5000 2>/dev/null | \
    grep -o '^[^:]*' | sort | uniq -c | sort -rn

if [ $? -ne 0 ]; then
    echo "  (No se encontraron mensajes o timeout)"
fi

echo ""
echo "================================================================"
echo "Analisis completado"
echo "================================================================"
echo ""
echo "Para ver en tiempo real:"
echo '  docker exec '"$KAFKA_CONTAINER"' kafka-console-consumer \'
echo '    --bootstrap-server localhost:9092 \'
echo '    --topic '"$COMMAND_TOPIC"' \'
echo '    --from-beginning \'
echo '    --property print.key=true \'
echo '    --max-messages 10'
echo ""

# Pausar para que el usuario pueda leer
read -p "Presiona Enter para salir..."