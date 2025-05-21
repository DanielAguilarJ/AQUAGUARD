package com.tuempresa.fugas.domain

import com.tuempresa.fugas.model.SensorData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FugaDetectorTest {
    @Test
    fun `detecta fuga por flujo alto y presion baja`() {
        val data = SensorData("2025-05-19T12:00:00Z", flujo = 7.0f, presion = 40.0f, vibracion = 0.2f)
        assertTrue(FugaDetector.detectarFuga(data))
    }

    @Test
    fun `detecta fuga por vibracion alta`() {
        val data = SensorData("2025-05-19T12:00:00Z", flujo = 2.0f, presion = 80.0f, vibracion = 1.3f)
        assertTrue(FugaDetector.detectarFuga(data))
    }

    @Test
    fun `no detecta fuga en condiciones normales`() {
        val data = SensorData("2025-05-19T12:00:00Z", flujo = 2.0f, presion = 80.0f, vibracion = 0.1f)
        assertFalse(FugaDetector.detectarFuga(data))
    }

    @Test
    fun `detecta fuga en serie temporal`() {
        val datos = listOf(
            SensorData("2025-05-19T12:00:00Z", 2.0f, 80.0f, 0.1f),
            SensorData("2025-05-19T12:01:00Z", 7.0f, 40.0f, 0.2f),
            SensorData("2025-05-19T12:02:00Z", 7.2f, 39.0f, 0.3f),
            SensorData("2025-05-19T12:03:00Z", 7.1f, 38.0f, 0.2f),
            SensorData("2025-05-19T12:04:00Z", 2.0f, 80.0f, 0.1f)
        )
        assertTrue(FugaDetector.detectarFugaEnSerie(datos))
    }
}
