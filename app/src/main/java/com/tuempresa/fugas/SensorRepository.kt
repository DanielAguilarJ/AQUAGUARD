package com.tuempresa.fugas.repository

import com.tuempresa.fugas.model.AlertData
import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.network.ApiService
import com.tuempresa.fugas.util.Resource // Importa el wrapper Resource
import java.io.IOException
import retrofit2.HttpException

class SensorRepository(private val api: ApiService) {
    suspend fun getUltimosDatos(): Resource<List<SensorData>> {
        return try {
            val data = api.getUltimosDatos()
            // Validación y saneamiento
            val datosValidos = data.filter {
                it.flujo in 0f..10f &&
                it.presion in 0f..200f &&
                it.vibracion in 0f..2f
            }
            Resource.Success(datosValidos)
        } catch (e: IOException) {
            Resource.Error("Sin conexión a internet", e)
        } catch (e: HttpException) {
            Resource.Error("Error del servidor: ${e.code()}", e)
        } catch (e: Exception) {
            Resource.Error("Error inesperado", e)
        }
    }

    suspend fun getAlertHistory(): Resource<List<AlertData>> {
        return try {
            val data = api.getAlertHistory()
            Resource.Success(data)
        } catch (e: IOException) {
            Resource.Error("Sin conexión a internet", e)
        } catch (e: HttpException) {
            Resource.Error("Error del servidor: ${e.code()}", e)
        } catch (e: Exception) {
            Resource.Error("Error inesperado", e)
        }
    }

    suspend fun updateAlertStatusRemote(timestamp: String, revisada: Boolean? = null, eliminada: Boolean? = null): Resource<Unit> {
        return try {
            val response = api.updateAlertStatus(timestamp, revisada, eliminada)
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error("Error al actualizar alerta en backend: ${response.code()}")
            }
        } catch (e: IOException) {
            Resource.Error("Sin conexión a internet", e)
        } catch (e: HttpException) {
            Resource.Error("Error del servidor: ${e.code()}", e)
        } catch (e: Exception) {
            Resource.Error("Error inesperado", e)
        }
    }
}
