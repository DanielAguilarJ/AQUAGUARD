package com.tuempresa.fugas.model

data class SensorData(
    val timestamp: String,
    val flujo: Float,
    val presion: Float,
    val vibracion: Float
)
