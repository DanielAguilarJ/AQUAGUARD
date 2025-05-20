package com.tuempresa.fugas.domain

import com.tuempresa.fugas.model.SensorData

object FugaDetector {
    /**
     * Detecta fuga de agua a partir de los datos de sensores.
     * Reglas básicas (puedes ajustar según tu experiencia y datos reales):
     * - Fuga si el flujo es alto y la presión baja simultáneamente.
     * - Fuga si hay vibración anómala junto con flujo/presión anómalos.
     * - Fuga si el flujo se mantiene alto durante un periodo prolongado sin consumo esperado.
     */
    fun detectarFuga(sensorData: SensorData): Boolean {
        // Umbrales ajustables
        val flujoAlto = sensorData.flujo > 6.0f
        val presionBaja = sensorData.presion < 50.0f
        val vibracionAlta = sensorData.vibracion > 0.8f

        // Regla 1: flujo alto y presión baja
        if (flujoAlto && presionBaja) return true
        // Regla 2: vibración alta y flujo/presión anómalos
        if (vibracionAlta && (flujoAlto || presionBaja)) return true
        // Regla 3: solo vibración muy alta
        if (sensorData.vibracion > 1.2f) return true

        // Puedes agregar más reglas o lógica de IA aquí
        return false
    }

    /**
     * Detecta fuga en una serie temporal de datos (ventana móvil).
     * Retorna true si se detecta fuga en la ventana.
     */
    fun detectarFugaEnSerie(datos: List<SensorData>, ventana: Int = 5): Boolean {
        if (datos.size < ventana) return false
        val ventanaDatos = datos.takeLast(ventana)
        // Si 3 o más lecturas consecutivas detectan fuga, se considera real
        return ventanaDatos.count { detectarFuga(it) } >= 3
    }
}
