package com.tuempresa.fugas.repository

import com.tuempresa.fugas.api.FugasApi
import com.tuempresa.fugas.model.LinkDeviceRequest
import com.tuempresa.fugas.util.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FugasRepository(private val api: FugasApi) {

    suspend fun updateAlertStatusRemote(timestamp: String, revisada: Boolean? = null, eliminada: Boolean? = null): Resource<Unit> {
        // ...existing code...
    }

    /**
     * Vincula el dispositivo al usuario/app en el backend
     */
    suspend fun linkDevice(request: LinkDeviceRequest): Resource<Unit> {
        return try {
            val response = api.linkDevice(request)
            if (response.isSuccessful) Resource.Success(Unit)
            else Resource.Error("Error al vincular dispositivo: ${response.code()}")
        } catch (e: Exception) {
            Resource.Error("Error de red al vincular dispositivo", e)
        }
    }
}