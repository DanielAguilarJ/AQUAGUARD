package com.tuempresa.fugas.viewmodel

import Resource // Importa el wrapper Resource
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.tuempresa.fugas.FugasApp
import com.tuempresa.fugas.data.AlertStatusDao
import com.tuempresa.fugas.datastore.SettingsDataStore
import com.tuempresa.fugas.model.AlertData
import com.tuempresa.fugas.model.AlertStatus
import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.repository.SensorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SensorViewModel(
    private val repository: SensorRepository,
    private val settingsDataStore: SettingsDataStore,
    application: Application
) : AndroidViewModel(application) {
    var sensorData by mutableStateOf<List<SensorData>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private val _endpoint = MutableStateFlow("")
    val endpoint: StateFlow<String> = _endpoint.asStateFlow()

    // Nuevo: historial de alertas
    private val _alertHistory = MutableStateFlow<List<AlertData>>(emptyList())
    val alertHistory: StateFlow<List<AlertData>> = _alertHistory.asStateFlow()

    private val alertStatusDao: AlertStatusDao =
        (application as FugasApp).alertStatusDao
    private val _alertStatus = MutableStateFlow<Map<String, AlertStatus>>(emptyMap())
    val alertStatus: StateFlow<Map<String, AlertStatus>> = _alertStatus.asStateFlow()

    init {
        viewModelScope.launch {
            settingsDataStore.endpoint.collect {
                _endpoint.value = it
            }
        }
        // Cargar historial al iniciar
        fetchAlertHistory()

        viewModelScope.launch {
            alertStatusDao.getAll().collect { list ->
                _alertStatus.value = list.associateBy { it.timestamp }
            }
        }
    }

    fun setEndpoint(url: String) {
        viewModelScope.launch {
            settingsDataStore.setEndpoint(url)
        }
    }

    fun fetchSensorData() {
        isLoading = true
        error = null
        viewModelScope.launch {
            when (val result = repository.getUltimosDatos()) {
                is Resource.Success -> {
                    sensorData = result.data
                }
                is Resource.Error -> {
                    error = result.message
                }
                is Resource.Loading -> { /* No usado aquí */ }
            }
            isLoading = false
        }
    }

    // Nuevo: obtener historial de alertas desde backend
    fun fetchAlertHistory() {
        viewModelScope.launch {
            when (val result = repository.getAlertHistory()) {
                is Resource.Success -> {
                    _alertHistory.value = result.data
                }
                is Resource.Error -> {
                    // Puedes propagar el error a la UI si lo deseas
                }
                is Resource.Loading -> { /* No usado aquí */ }
            }
        }
    }

    fun marcarRevisada(timestamp: String) {
        viewModelScope.launch {
            // Primero intenta en backend
            when (val result = repository.updateAlertStatusRemote(timestamp, revisada = true)) {
                is Resource.Success -> {
                    val prev = alertStatusDao.getByTimestamp(timestamp)
                    alertStatusDao.upsert(
                        AlertStatus(timestamp, revisada = true, eliminada = prev?.eliminada ?: false)
                    )
                }
                is Resource.Error -> {
                    // Manejo de error: podrías mostrar un mensaje o fallback local
                }
                else -> {}
            }
        }
    }

    fun eliminarAlerta(timestamp: String) {
        viewModelScope.launch {
            // Primero intenta en backend
            when (val result = repository.updateAlertStatusRemote(timestamp, eliminada = true)) {
                is Resource.Success -> {
                    val prev = alertStatusDao.getByTimestamp(timestamp)
                    alertStatusDao.upsert(
                        AlertStatus(timestamp, revisada = prev?.revisada ?: false, eliminada = true)
                    )
                }
                is Resource.Error -> {
                    // Manejo de error: podrías mostrar un mensaje o fallback local
                }
                else -> {}
            }
        }
    }
}
