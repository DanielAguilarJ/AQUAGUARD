#!/bin/zsh
# Descarga gradle wrapper si no existe
if [ ! -f gradlew ]; then
  echo "Descargando gradle wrapper..."
  gradle wrapper --gradle-version 8.2.1
fi
chmod +x gradlew
