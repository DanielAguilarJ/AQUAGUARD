package com.tuempresa.fugas

import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.repository.SensorRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SensorRepositoryTest {
    private val api = mockk<com.tuempresa.fugas.network.ApiService>()
    private val repo = SensorRepository(api)

    @Test
    fun `valida y filtra datos anómalos del backend`() = runBlocking {
        // Simula datos con valores fuera de rango
        val datos = listOf(
            SensorData("1", 123L, flujo = 8.5f, presion = 210f, vibracion = 0.5f), // presión fuera de rango
            SensorData("2", 124L, flujo = -1f, presion = 100f, vibracion = 0.2f), // flujo negativo
            SensorData("3", 125L, flujo = 5f, presion = 80f, vibracion = 0.3f) // válido
        )
        coEvery { api.getUltimosDatos() } returns datos
        val result = repo.getUltimosDatos()
        // Solo debe pasar el dato válido
        assertEquals(1, result.size)
        assertEquals("3", result[0].id)
    }
}
