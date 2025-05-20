package com.tuempresa.fugas.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.network.ApiService
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SensorViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val api: ApiService = mockk()
    private lateinit var viewModel: SensorViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        viewModel = SensorViewModel(api)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `fetchSensorData actualiza sensorData correctamente`() {
        val datos = listOf(SensorData("2025-05-19T12:00:00Z", 1.2f, 2.3f, 0.5f))
        coEvery { api.getUltimosDatos() } returns datos
        viewModel.fetchSensorData()
        assertEquals(datos, viewModel.sensorData)
    }
}
