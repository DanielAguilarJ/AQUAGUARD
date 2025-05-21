#!/bin/zsh
# Script para migrar fuentes y recursos a estructura Android estándar

set -e

PAQUETE="com/tuempresa/fugas"
DEST="app/src/main/java/${PAQUETE}"
mkdir -p $DEST/ui/components
mkdir -p $DEST/domain
mkdir -p $DEST/model
mkdir -p $DEST/repository
mkdir -p $DEST/data
mkdir -p $DEST/datastore
mkdir -p app/src/main/res/values-en

# Mover fuentes principales
mv MainActivity.kt $DEST/ 2>/dev/null || true
mv FugasApp.kt $DEST/ 2>/dev/null || true
mv SensorViewModel.kt $DEST/ 2>/dev/null || true
mv SensorRepository.kt $DEST/ 2>/dev/null || true
mv ApiService.kt $DEST/ 2>/dev/null || true
mv NotificacionLocal.kt $DEST/ 2>/dev/null || true
mv MyFirebaseMessagingService.kt $DEST/ 2>/dev/null || true

# Mover carpetas
mv ui/*.kt $DEST/ui/ 2>/dev/null || true
mv ui/components/*.kt $DEST/ui/components/ 2>/dev/null || true
mv domain/*.kt $DEST/domain/ 2>/dev/null || true
mv model/*.kt $DEST/model/ 2>/dev/null || true
mv repository/*.kt $DEST/repository/ 2>/dev/null || true
mv data/*.kt $DEST/data/ 2>/dev/null || true
mv datastore/*.kt $DEST/datastore/ 2>/dev/null || true

# Mover recursos
mkdir -p app/src/main/res/values
mv strings.xml app/src/main/res/values/ 2>/dev/null || true
mv values-en/strings.xml app/src/main/res/values-en/ 2>/dev/null || true

# Mover AndroidManifest
mkdir -p app/src/main
mv AndroidManifest.xml app/src/main/ 2>/dev/null || true

echo "Migración completada. Ahora ejecuta: ./gradlew assembleDebug"
