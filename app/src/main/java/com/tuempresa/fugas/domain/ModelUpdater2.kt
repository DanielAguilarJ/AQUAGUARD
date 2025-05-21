package com.tuempresa.fugas.domain

import android.content.Context
import com.tuempresa.fugas.model.AlertData
import com.tuempresa.fugas.model.SensorData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL
import android.content.res.AssetFileDescriptor
import android.util.Log
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Gestor avanzado de modelos IA con actualización automática, versionado y soporte multimodelo.
 */
object ModelUpdater2 {
    private const val TAG = "ModelUpdater"
    private const val MODEL_FILENAME = "leak_detection.tflite"
    const val DEFAULT_SERVER_URL = "https://api.tuempresa.com"
    private const val FORECAST_MODEL_FILENAME = "leak_forecast.tflite"
    private const val ANOMALY_MODEL_FILENAME = "anomaly_detection.tflite"
    
    // Metadata del modelo
    private data class ModelMetadata(
        val version: String,
        val timestamp: String,
        val accuracy: Float,
        val description: String
    )
    // Persistencia de metadata en SharedPreferences
    private const val PREFS_NAME = "model_prefs"
    private const val KEY_VERSION = "model_version"
    private const val KEY_ETAG = "model_etag"
    private const val KEY_TIMESTAMP = "last_updated"
    private const val KEY_MODEL_TYPE = "model_type"
    private lateinit var prefs: android.content.SharedPreferences

    /**
     * Inicializa el gestor cargando metadata persistida
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadMetadata()
    }
    
    /** Carga metadata persistida */
    private fun loadMetadata() {
        val version = prefs.getString(KEY_VERSION, "0.0") ?: "0.0"
        val etag = prefs.getString(KEY_ETAG, "") ?: ""
        val lastUpdated = prefs.getString(KEY_TIMESTAMP, "") ?: ""
        val modelType = prefs.getString(KEY_MODEL_TYPE, ModelType.MAIN.name) ?: ModelType.MAIN.name
        _modelState.update {
            it.copy(
                modelVersion = version,
                modelEtag = etag,
                lastUpdated = lastUpdated,
                modelType = modelType,
                isModelAvailable = version != "0.0"
            )
        }
    }
    
    /** Guarda metadata en SharedPreferences */
    private fun saveMetadata(state: ModelState) {
        prefs.edit().apply {
            putString(KEY_VERSION, state.modelVersion)
            putString(KEY_ETAG, state.modelEtag)
            putString(KEY_TIMESTAMP, state.lastUpdated)
            putString(KEY_MODEL_TYPE, state.modelType)
            apply()
        }
    }
    
    // Estado actual del modelo
    private val _modelState = MutableStateFlow(ModelState())
    val modelState = _modelState.asStateFlow()
    
    /**
     * Actualiza el modelo desde URL remota con verificación de versión y checksum
     */
    suspend fun downloadModel(
        context: Context, 
        url: String,
        modelType: ModelType = ModelType.MAIN
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val filename = getFilenameForType(modelType)
            Log.d(TAG, "Descargando modelo $modelType desde $url")
            // Comparación semántica de versiones desde URL
            val remoteVersion = extractVersion(url)
            remoteVersion?.let { rv ->
                val currentVersion = _modelState.value.modelVersion
                if (compareVersions(rv, currentVersion) <= 0) {
                    _modelState.update { it.copy(
                        isUpdating = false,
                        updateMessage = "Modelo ya actualizado (v$rv <= v$currentVersion)"
                    ) }
                    Log.d(TAG, "Saltando descarga: version remota ($rv) <= local ($currentVersion)")
                    return@withContext true
                }
            }
            
            _modelState.update { it.copy(isUpdating = true, updateMessage = "Iniciando descarga...") }
            
            // Conectar y verificar headers
            val connection = URL(url).openConnection()
            connection.connectTimeout = 10000
            connection.readTimeout = 30000
            connection.connect()
            
            val contentLength = connection.contentLength
            val etag = connection.getHeaderField("ETag")
            val checksumHeader = connection.getHeaderField("Content-MD5")
            val lastModified = connection.getHeaderField("Last-Modified")
            
            _modelState.update { it.copy(
                updateMessage = "Descargando ${contentLength / 1024} KB..."
            )}
            
            // Verificar si necesitamos actualizar
            val currentFile = File(context.filesDir, filename)
            if (currentFile.exists() && etag != null && etag == _modelState.value.modelEtag) {
                _modelState.update { it.copy(
                    isUpdating = false,
                    updateMessage = "Modelo ya actualizado."
                )}
                Log.d(TAG, "Modelo ya actualizado (ETag coincide)")
                return@withContext true
            }
            
            // Descargar modelo
            val tempFile = File(context.filesDir, "${filename}.temp")
            val input = connection.getInputStream()
            
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(4 * 1024) // 4KB buffer
                var bytesRead: Int
                var totalBytesRead = 0L
                var lastProgressUpdate = 0
                
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    
                    // Actualizar progreso cada 10%
                    if (contentLength > 0) {
                        val progress = (totalBytesRead * 100 / contentLength).toInt()
                        if (progress >= lastProgressUpdate + 10) {
                            lastProgressUpdate = progress
                            _modelState.update { it.copy(
                                updateProgress = progress,
                                updateMessage = "Descargado $progress%"
                            )}
                        }
                    }
                }
            }
            
            // Verificar modelo descargado
            if (validateModel(tempFile, checksumHeader)) {
                // Reemplazar modelo anterior
                if (currentFile.exists()) {
                    currentFile.delete()
                }
                tempFile.renameTo(currentFile)
                
                // Actualizar estado y metadata
                val newVersion = extractVersion(url) ?: _modelState.value.modelVersion
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val newState = ModelState(
                    isModelAvailable = true,
                    modelVersion = newVersion,
                    lastUpdated = timestamp,
                    modelEtag = etag ?: "",
                    modelType = modelType.name,
                    isUpdating = false,
                    updateProgress = 100,
                    updateMessage = "Modelo actualizado correctamente",
                    updateError = null
                )
                _modelState.value = newState
                saveMetadata(newState)
                 
                Log.d(TAG, "Modelo $modelType descargado correctamente")
                true
            } else {
                tempFile.delete()
                _modelState.update { it.copy(
                    isUpdating = false,
                    updateMessage = "Error: modelo inválido",
                    updateError = "El modelo descargado no es válido"
                )}
                Log.e(TAG, "Modelo descargado inválido")
                false
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error descargando modelo: ${e.message}")
            _modelState.update { it.copy(
                isUpdating = false,
                updateMessage = "Error: ${e.message}",
                updateError = e.message
            )}
            false
        }
    }
    
    /**
     * Valida que el modelo descargado sea válido y tenga el formato correcto
     */
    fun validateModel(
        file: File,
        expectedChecksum: String?
    ): Boolean {
        // Simple validación de tamaño mínimo
        if (file.length() < 1024) return false
        // Verificación de checksum MD5 si está disponible
        expectedChecksum?.let { expected ->
            val actual = computeMD5(file)
            if (!actual.equals(expected, ignoreCase = true)) {
                Log.e(TAG, "Checksum inválido: esperado $expected, obtenido $actual")
                return false
            }
        }
        return true
    }

    /**
     * Calcula MD5 de un archivo
     */
    private fun computeMD5(file: File): String {
        val md = java.security.MessageDigest.getInstance("MD5")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(4 * 1024)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString(separator = "") { "%02x".format(it) }
    }
    
    /**
     * Obtiene la versión del modelo desde la URL o metadata
     */
    fun extractVersion(url: String): String? {
        val versionRegex = Regex("v(\\d+\\.\\d+(\\.\\d+)?)")
        val match = versionRegex.find(url)
        return match?.groupValues?.get(1)
    }
    
    /**
     * Obtiene el modelo según su tipo
     */
    fun getModelFile(context: Context, modelType: ModelType = ModelType.MAIN): File? {
        val filename = getFilenameForType(modelType)
        val file = File(context.filesDir, filename)
        return if (file.exists()) file else null
    }
    
    /**
     * Obtiene todos los modelos disponibles
     */
    fun getAvailableModels(context: Context): Map<ModelType, File?> {
        return ModelType.values().associateWith { getModelFile(context, it) }
    }
    
    /**
     * Verifica si hay actualizaciones disponibles contactando al servidor
     */
    suspend fun checkForUpdates(context: Context, serverUrl: String): UpdateInfo = withContext(Dispatchers.IO) {
        try {
            val updateUrl = "$serverUrl/api/model/latest_version"
            val connection = URL(updateUrl).openConnection()
            connection.connectTimeout = 5000
            connection.connect()
            
            val responseCode = (connection as java.net.HttpURLConnection).responseCode
            if (responseCode == 200) {
                val input = connection.inputStream
                val response = input.bufferedReader().use { it.readText() }
                
                // Parse JSON (usando string parsing simple por brevedad)
                val version = extractJsonValue(response, "version") ?: "1.0"
                val url = extractJsonValue(response, "url") ?: ""
                val description = extractJsonValue(response, "description") ?: ""
                val requiredString = extractJsonValue(response, "required") ?: "false"
                val required = requiredString.equals("true", ignoreCase = true)
                
                // Verificar si necesitamos actualizar
                val currentVersion = _modelState.value.modelVersion
                val needsUpdate = compareVersions(version, currentVersion) > 0
                
                return@withContext UpdateInfo(
                    updateAvailable = needsUpdate,
                    newVersion = version,
                    updateUrl = url,
                    description = description,
                    isRequired = required
                )
            }
            
            UpdateInfo(updateAvailable = false)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error verificando actualizaciones: ${e.message}")
            UpdateInfo(updateAvailable = false)
        }
    }
    
    /**
     * Parse simple de valores JSON
     */
    private fun extractJsonValue(json: String, key: String): String? {
        val pattern = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
        val match = pattern.find(json)
        return match?.groupValues?.get(1)
    }
    
    /**
     * Compara versiones semánticas
     * @return >0 si v1 > v2, 0 si iguales, <0 si v1 < v2
     */
    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split('.').map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split('.').map { it.toIntOrNull() ?: 0 }
        
        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val part1 = parts1.getOrNull(i) ?: 0
            val part2 = parts2.getOrNull(i) ?: 0
            
            if (part1 != part2) {
                return part1 - part2
            }
        }
        
        return 0
    }
    
    /**
     * Obtiene el nombre de archivo según el tipo de modelo
     */
    private fun getFilenameForType(modelType: ModelType): String {
        return when (modelType) {
            ModelType.MAIN -> MODEL_FILENAME
            ModelType.FORECAST -> FORECAST_MODEL_FILENAME
            ModelType.ANOMALY -> ANOMALY_MODEL_FILENAME
        }
    }
    
    /**
     * Tipos de modelos soportados
     */
    enum class ModelType {
        MAIN,       // Modelo principal de detección
        FORECAST,   // Modelo de predicción futura
        ANOMALY     // Modelo de detección de anomalías
    }
    
    /**
     * Estado actual del modelo en la app
     */
    data class ModelState(
        val isModelAvailable: Boolean = false,
        val modelVersion: String = "0.0",
        val lastUpdated: String = "",
        val modelEtag: String = "",
        val modelType: String = "MAIN",
        val isUpdating: Boolean = false,
        val updateProgress: Int = 0,
        val updateMessage: String = "",
        val updateError: String? = null
    )
    
    /**
     * Información sobre actualizaciones disponibles
     */
    data class UpdateInfo(
        val updateAvailable: Boolean,
        val newVersion: String = "",
        val updateUrl: String = "",
        val description: String = "",
        val isRequired: Boolean = false
    )
}
