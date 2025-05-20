package com.tuempresa.fugas.model

/**
 * Representa una alerta generada por la detección de fuga.
 * Incluye metadatos avanzados para análisis en el backend.
 */
data class AlertData(
    val timestamp: String,
    val nivel: String,
    val mensaje: String,
    val metadatos: Map<String, Any> = emptyMap()
)
