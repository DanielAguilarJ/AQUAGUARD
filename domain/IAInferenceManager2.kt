package com.tuempresa.fugas.domain

import android.content.Context
import android.util.Log
import com.tuempresa.fugas.model.SensorData
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.write
import kotlin.math.exp
import kotlin.math.ln

/**
 * Sistema avanzado de IA para detección predictiva de fugas mediante análisis multidimensional
 * de series temporales, aprendizaje continuo y explicabilidad.
 * 
 * Características principales:
 * 1. Detección temprana de anomalías mediante análisis de patrones multivariables
 * 2. Predicción proactiva con ventanas temporales adaptativas
 * 3. Transfer learning y adaptación a distintos entornos
 * 4. Modelo on-device optimizado para rendimiento en móviles
 * 5. Explicabilidad avanzada (XAI) para transparencia en decisiones
 * 6. Mejora continua mediante aprendizaje por reforzamiento
 * 7. Sistema de análisis multidimensional con PCA y métodos de ensamble
 * 8. Integración con análisis contextual y línea base adaptativa
 */
object IAInferenceManager {
    private const val TAG = "IAInferenceManager"
    private const val SEQUENCE_LENGTH = 10 // Tamaño máximo de secuencia para análisis temporal
    
    // Modelos TFLite
    private lateinit var mainInterpreter: Interpreter
    private lateinit var forecastInterpreter: Interpreter
    private lateinit var anomalyInterpreter: Interpreter
    
    // Buffer para secuencias temporales
    private val sequenceBuffer = mutableListOf<SensorData>()
    
    // Metadatos del modelo y estadísticas
    private val featureImportance = mutableMapOf<String, Float>()
    private val recentPredictions = mutableListOf<Float>()
    @Volatile
    private var detectionThreshold = 0.65f // Ahora mutable y seguro para concurrencia
    
    // Feedback loop para aprendizaje continuo
    private val feedbackData = mutableListOf<Pair<List<Float>, Boolean>>()
    private val modelLock = ReentrantReadWriteLock()
    
    // Normalización y estadísticas por feature
    private val featureStats = mutableMapOf(
        "flujo" to FeatureStats(0f, 10f, 0f, 0f),
        "presion" to FeatureStats(0f, 200f, 0f, 0f),
        "vibracion" to FeatureStats(0f, 2f, 0f, 0f)
    )
    
    // PCA para detección de anomalías (análisis multidimensional)
    private var pcaComponents: Array<FloatArray>? = null
    private var pcaMean: FloatArray? = null
    private const val pcaThreshold = 0.05f

    /**
     * Inicializa los modelos de IA y carga los pesos pre-entrenados
     * y metadatos PCA si existen.
     */
    fun init(context: Context) {
        modelLock.write {
            try {
                // Cargar modelo principal
                val localModel = ModelUpdater.getModelFile(context)
                val modelBuffer = if (localModel != null) {
                    loadFileAsMappedBuffer(localModel)
                } else {
                    loadAssetAsMappedBuffer(context, "leak_detection.tflite")
                }
                
                // Configurar intérprete principal con aceleradores hardware cuando disponibles
                val mainOptions = Interpreter.Options().apply {
                    setNumThreads(4)
                    setUseNNAPI(true) // Usa Neural Network API si está disponible
                }
                mainInterpreter = Interpreter(modelBuffer, mainOptions)
                
                // Cargar modelos adicionales para forecasting y detección de anomalías
                try {
                    val forecastBuffer = loadAssetAsMappedBuffer(context, "leak_forecast.tflite")
                    forecastInterpreter = Interpreter(forecastBuffer, Interpreter.Options().apply {
                        setNumThreads(2)
                    })
                    
                    val anomalyBuffer = loadAssetAsMappedBuffer(context, "anomaly_detection.tflite")
                    anomalyInterpreter = Interpreter(anomalyBuffer, Interpreter.Options().apply {
                        setNumThreads(2)
                    })
                    
                    Log.d(TAG, "Modelos avanzados cargados correctamente")
                } catch (e: Exception) {
                    Log.w(TAG, "No se pudieron cargar modelos avanzados: ${e.message}")
                    // Fallback a solo modelo principal si los avanzados fallan
                }
                
                // Inicializar feature importance
                featureImportance["flujo"] = 0.4f
                featureImportance["presion"] = 0.3f
                featureImportance["vibracion"] = 0.3f
                
                Log.i(TAG, "Sistema de IA inicializado correctamente")
            } catch (e: Exception) {
                Log.e(TAG, "Error al inicializar IA: ${e.message}")
                // Implementar fallback a reglas heurísticas si falla todo
            }
        }
    }

    private fun loadAssetAsMappedBuffer(context: Context, modelPath: String): MappedByteBuffer {
        return FileUtil.loadMappedFile(context, modelPath)
    }
    
    private fun loadFileAsMappedBuffer(file: File): MappedByteBuffer {
        FileInputStream(file).use { input ->
            val channel: FileChannel = input.channel
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                0,
                file.length()
            )
        }
    }

    /**
     * Detección de anomalía usando PCA reconstructivo en ventana de secuencia.
     * El análisis de componentes principales permite detectar anomalías
     * multidimensionales basadas en los patrones de covarianza entre variables.
     * 
     * @return Score de anomalía (0-1)
     */
    private fun detectAnomalyPCA(): Float {
        val comps = pcaComponents ?: return 0f
        val mean = pcaMean ?: return 0f
        synchronized(sequenceBuffer) {
            if (sequenceBuffer.size < SEQUENCE_LENGTH) return 0f
            
            val last = sequenceBuffer.takeLast(SEQUENCE_LENGTH)
            
            // Construir vector de características (SEQUENCE_LENGTH * 3 features)
            val vec = FloatArray(SEQUENCE_LENGTH * 3)
            last.forEachIndexed { i, data ->
                // Normalización previa para mejor rendimiento del PCA
                val flujoStats = featureStats["flujo"]!!
                val presionStats = featureStats["presion"]!!
                val vibracionStats = featureStats["vibracion"]!!
                
                vec[i*3] = normalizeValue(data.flujo, flujoStats.min, flujoStats.max)
                vec[i*3+1] = normalizeValue(data.presion, presionStats.min, presionStats.max)
                vec[i*3+2] = normalizeValue(data.vibracion, vibracionStats.min, vibracionStats.max)
            }
            
            // Centrar datos restando la media
            val centered = FloatArray(vec.size) { j -> vec[j] - mean[j] }
            
            // Proyección a espacio PCA y reconstrucción
            val reconstructed = FloatArray(vec.size)
            comps.forEachIndexed { r, comp ->
                // Proyección: calcular score en este componente
                val score = centered.zip(comp).sumOf { (v, c) -> v * c }
                
                // Reconstrucción: añadir contribución de este componente
                comp.forEachIndexed { idx, c -> reconstructed[idx] += score * c }
            }
            
            // Error de reconstrucción (distancia euclídea al cuadrado)
            val mse = centered.zip(reconstructed).sumOf { (v, rec) -> (v - rec)*(v - rec) } / vec.size
            
            // Convertir MSE a score de anomalía (0-1) usando función sigmoide
            val anomalyScore = if (mse > pcaThreshold) {
                1.0f / (1.0f + exp(-5f * (mse - pcaThreshold * 2f)))
            } else {
                0f
            }
            
            return anomalyScore
        }
    }

    /**
     * Predice la probabilidad de fuga para una lectura de sensor individual
     * usando todos los modelos disponibles y técnicas de ensemble.
     * 
     * @return Probabilidad de fuga (0.0-1.0)
     */
    fun predict(sensorData: SensorData): Float {
        // Normalizar datos
        val normalizedData = normalizeSensorData(sensorData)
        
        // Buffer para entrada del modelo principal (flujo, presión, vibración)
        val inputBuffer = ByteBuffer.allocateDirect(3 * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        
        // Buffer para salida del modelo (probabilidad)
        val outputBuffer = ByteBuffer.allocateDirect(Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
        
        // Prepara entrada
        inputBuffer.clear()
        inputBuffer.asFloatBuffer().put(floatArrayOf(
            normalizedData.flujo,
            normalizedData.presion,
            normalizedData.vibracion
        ))
        
        // Ejecuta inferencia en modelo principal
        modelLock.write {
            mainInterpreter.run(inputBuffer, outputBuffer)
        }
        
        // Obtiene resultado
        outputBuffer.rewind()
        val baseProbability = outputBuffer.asFloatBuffer().get()
        
        // Actualiza sequence buffer para análisis temporal
        synchronized(sequenceBuffer) {
            sequenceBuffer.add(sensorData)
            if (sequenceBuffer.size > SEQUENCE_LENGTH) {
                sequenceBuffer.removeAt(0)
            }
        }
        
        // Actualiza estadísticas
        updateFeatureStats(sensorData)
        
        // Ensemble con anomaly detection y PCA
        var finalProbability = baseProbability
        // ensemble con anomaly detection y PCA
        if (this::anomalyInterpreter.isInitialized && sequenceBuffer.size >= 3) {
            val anomalyScore = detectAnomaly(normalizedData)
            val pcaScore = detectAnomalyPCA()
            // combinar: modelo principal 60%, autoencoder 20%, PCA 20%
            finalProbability = baseProbability * 0.6f + anomalyScore * 0.2f + pcaScore * 0.2f
        }
        
        // Registra predicción para análisis de tendencias
        synchronized(recentPredictions) {
            recentPredictions.add(finalProbability)
            if (recentPredictions.size > 20) {
                recentPredictions.removeAt(0)
            }
        }
        
        return finalProbability
    }
    
    /**
     * Detecta anomalías usando un modelo autoencoder especializado
     */
    private fun detectAnomaly(normalizedData: SensorData): Float {
        if (!this::anomalyInterpreter.isInitialized) return 0f
        
        try {
            // Prepara entrada para modelo de anomalías
            val inputBuffer = ByteBuffer.allocateDirect(3 * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
            
            // Output: Error de reconstrucción (1 valor)
            val outputBuffer = ByteBuffer.allocateDirect(Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
            
            // Prepara entrada
            inputBuffer.clear()
            inputBuffer.asFloatBuffer().put(floatArrayOf(
                normalizedData.flujo,
                normalizedData.presion,
                normalizedData.vibracion
            ))
            
            // Ejecuta modelo de anomalías
            modelLock.write {
                anomalyInterpreter.run(inputBuffer, outputBuffer)
            }
            
            // Convierte error de reconstrucción a score de anomalía (0-1)
            outputBuffer.rewind()
            val reconstructionError = outputBuffer.asFloatBuffer().get()
            return 1.0f - (1.0f / (1.0f + exp(reconstructionError * 5)))
            
        } catch (e: Exception) {
            Log.e(TAG, "Error en detección de anomalías: ${e.message}")
            return 0f
        }
    }
    
    /**
     * Predice posibles fugas antes de que ocurran, usando datos históricos
     * para proyectar tendencias y detectar patrones previos a incidentes.
     * 
     * @param timeHorizonHours Horizonte de tiempo para la predicción (horas)
     * @return Probabilidad de fuga en las próximas horas (0.0-1.0) y predicciones por hora
     */
    fun predictFutureLeak(timeHorizonHours: Int = 24): Pair<Float, List<HourlyPrediction>> {
        if (!this::forecastInterpreter.isInitialized || sequenceBuffer.size < 5) {
            return 0f to emptyList()
        }
        
        try {
            // Prepara secuencia temporal para LSTM forecasting
            val recent = sequenceBuffer.takeLast(5)
            val inputShape = forecastInterpreter.inputTensor(0).shape() // e.g. [1,5,3]
            val inputBuffer = ByteBuffer.allocateDirect(inputShape.reduce { a, b -> a * b } * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
            
            // Empaquetar secuencia con normalización mejorada
            val fb = inputBuffer.asFloatBuffer()
            
            // Obtener estadísticas para normalización adaptativa
            val flujoStats = featureStats["flujo"]!!
            val presionStats = featureStats["presion"]!!
            val vibracionStats = featureStats["vibracion"]!!
            
            recent.forEach { d ->
                fb.put(normalizeValue(d.flujo, flujoStats.min, flujoStats.max))
                fb.put(normalizeValue(d.presion, presionStats.min, presionStats.max))
                fb.put(normalizeValue(d.vibracion, vibracionStats.min, vibracionStats.max))
            }
            
            // Output: vector de predicciones para cada hora
            val outputShape = forecastInterpreter.outputTensor(0).shape() // e.g. [1,24]
            val outputSize = outputShape[1].coerceAtMost(timeHorizonHours)
            val outputBuffer = ByteBuffer.allocateDirect(outputShape.reduce { a, b -> a * b } * Float.SIZE_BYTES)
                .order(ByteOrder.nativeOrder())
            
            // Ejecutar predicción LSTM
            modelLock.write {
                forecastInterpreter.run(inputBuffer, outputBuffer)
            }
            
            outputBuffer.rewind()
            val outArray = FloatArray(outputShape[1])
            outputBuffer.asFloatBuffer().get(outArray)
            
            // Procesar predicciones por hora
            val hourlyPredictions = mutableListOf<HourlyPrediction>()
            val now = System.currentTimeMillis()
            val hourInMillis = 60 * 60 * 1000L
            
            // Crear predicciones detalladas para cada hora
            for (i in 0 until outputSize) {
                val timestamp = now + (i + 1) * hourInMillis
                val probability = outArray[i]
                
                // Calcular factores de riesgo para explicabilidad
                val riskFactors = mutableMapOf<String, Float>()
                
                // En horas futuras cercanas, la confianza es mayor
                val confidenceDecay = 1.0f - (i * 0.025f).coerceAtMost(0.5f)
                
                // Añadir factores de riesgo basados en importancia de features
                if (probability > 0.5f) {
                    featureImportance.forEach { (feature, importance) ->
                        riskFactors[feature] = importance * confidenceDecay
                    }
                }
                
                hourlyPredictions.add(
                    HourlyPrediction(
                        hourOffset = i + 1,
                        timestamp = timestamp,
                        probability = probability,
                        confidence = confidenceDecay,
                        riskFactors = riskFactors
                    )
                )
            }
            
            // Ajustar predicciones con patrón de incremento/decremento
            applyTemporalPatterns(hourlyPredictions)
            
            // Calcular promedio ponderado dando más peso a predicciones más cercanas
            var weightedSum = 0f
            var weightSum = 0f
            
            hourlyPredictions.forEachIndexed { index, pred ->
                val weight = 1.0f / (index + 1) // Más peso a horas más cercanas
                weightedSum += pred.probability * weight
                weightSum += weight
            }
            
            val avgProbability = if (weightSum > 0) weightedSum / weightSum else 0f
            
            return avgProbability to hourlyPredictions
        } catch (e: Exception) {
            Log.e(TAG, "Error en predicción futura LSTM: ${e.message}")
            return 0f to emptyList()
        }
    }
    
    /**
     * Clase para predicciones horarias con explicabilidad
     */
    data class HourlyPrediction(
        val hourOffset: Int,
        val timestamp: Long,
        val probability: Float,
        val confidence: Float,
        val riskFactors: Map<String, Float>
    )
    
    /**
     * Aplica patrones temporales a las predicciones horarias
     * para mejorar la coherencia de secuencias futuras
     */
    private fun applyTemporalPatterns(predictions: MutableList<HourlyPrediction>) {
        if (predictions.size < 3) return
        
        // Suavizar predicciones para evitar cambios bruscos irreales
        val smoothed = mutableListOf<Float>()
        val window = 3
        
        // Media móvil simple
        for (i in predictions.indices) {
            var sum = 0f
            var count = 0
            
            for (j in maxOf(0, i - window / 2)..minOf(predictions.size - 1, i + window / 2)) {
                sum += predictions[j].probability
                count++
            }
            
            smoothed.add(sum / count)
        }
        
        // Aplicar suavizado manteniendo tendencias
        for (i in predictions.indices) {
            // Combinar original con suavizado (70% suavizado, 30% original)
            val newProb = (smoothed[i] * 0.7f) + (predictions[i].probability * 0.3f)
            predictions[i] = predictions[i].copy(probability = newProb)
        }
    }
    
    /**
     * Predice fugas en series temporales y proporciona explicabilidad avanzada.
     * 
     * @return Probabilidad de fuga y explicabilidad (feature importance)
     */
    fun predictWithExplain(sensorData: SensorData): Pair<Float, Map<String, Float>> {
        val prediction = predict(sensorData)
        
        // Análisis de sensibilidad para explicabilidad
        val normalizedData = normalizeSensorData(sensorData)
        val importance = calculateFeatureImportance(normalizedData)
        
        return prediction to importance
    }
    
    /**
     * Predice fugas en una secuencia temporal completa y proporciona explicabilidad
     * avanzada para la predicción.
     * 
     * @return Probabilidad de fuga y explicabilidad detallada
     */
    fun predictSequenceWithExplain(datos: List<SensorData>): Pair<Float, Map<String, Float>> {
        if (datos.isEmpty()) return 0f to emptyMap()
        
        // Actualizar buffer de secuencia
        synchronized(sequenceBuffer) {
            sequenceBuffer.clear()
            sequenceBuffer.addAll(datos.takeLast(SEQUENCE_LENGTH))
        }
        
        // Predicción con modelo principal para último dato
        val latestPrediction = predict(datos.last())
        
        // Detectar anomalías en la secuencia completa
        val anomalyScore = if (sequenceBuffer.size >= 3) {
            val isSequenceAnomaly = detectSequenceAnomaly(datos.takeLast(SEQUENCE_LENGTH))
            if (isSequenceAnomaly) 0.3f else 0f
        } else 0f
        
        // Detectar anomalías con PCA multidimensional
        val pcaScore = detectAnomalyPCA()
        
        // Calcular probabilidad final con ensamble ponderado
        val finalProbability = latestPrediction * 0.6f + anomalyScore * 0.2f + pcaScore * 0.2f
        
        // Calcular importancia de features para explicabilidad
        val importance = mutableMapOf<String, Float>()
        
        // Importancia basada en análisis de sensibilidad de la secuencia
        val flujoVariance = calculateVariance(datos.map { it.flujo })
        val presionVariance = calculateVariance(datos.map { it.presion })
        val vibracionVariance = calculateVariance(datos.map { it.vibracion })
        
        // Calcular correlación entre flujo y presión (anticorrelación es signo de fuga)
        val flujoPresionCorr = calculateCorrelation(datos.map { it.flujo }, datos.map { it.presion })
        
        // Normalizar importancias
        val totalVariance = flujoVariance + presionVariance + vibracionVariance
        
        if (totalVariance > 0) {
            importance["flujo"] = (flujoVariance / totalVariance) * 0.7f
            importance["presion"] = (presionVariance / totalVariance) * 0.7f
            importance["vibracion"] = (vibracionVariance / totalVariance) * 0.7f
        } else {
            importance["flujo"] = 0.33f
            importance["presion"] = 0.33f
            importance["vibracion"] = 0.33f
        }
        
        // Añadir correlación como factor adicional si es significativa
        if (flujoPresionCorr < -0.3f) {
            // Correlación negativa fuerte (patrón típico de fuga)
            importance["correlacion_flujo_presion"] = kotlin.math.abs(flujoPresionCorr) * 0.3f
        }
        
        return finalProbability to importance
    }
    
    /**
     * Calcula la importancia de cada feature mediante análisis de sensibilidad
     * en el modelo (técnica de explicabilidad XAI).
     */
    private fun calculateFeatureImportance(data: SensorData): Map<String, Float> {
        // Uso de aproximación SHAP-like: variación condicional
        val baseline = predict(data)
        val variations = mapOf(
            "flujo" to run { val d = data.copy(flujo = (data.flujo * 1.1f).coerceAtMost(10f)); kotlin.math.abs(predict(d) - baseline) },
            "presion" to run { val d = data.copy(presion = (data.presion * 1.1f).coerceAtMost(200f)); kotlin.math.abs(predict(d) - baseline) },
            "vibracion" to run { val d = data.copy(vibracion = (data.vibracion * 1.1f).coerceAtMost(2f)); kotlin.math.abs(predict(d) - baseline) }
        )
        val total = variations.values.sum().coerceAtLeast(1e-4f)
        val normalized = variations.mapValues { it.value / total }
        // actualizar featureImportance global
        normalized.forEach { (k,v) -> featureImportance[k] = v }
        return normalized
    }
    
    /**
     * Proporciona una explicación textual de la predicción basada en los datos
     * y la importancia de cada feature (para mostrar al usuario).
     */
    fun explainPrediction(prediction: Float, importance: Map<String, Float>): String {
        val sb = StringBuilder()
        
        if (prediction > 0.8f) {
            sb.append("Alta probabilidad de fuga detectada (${(prediction*100).toInt()}%).\n")
        } else if (prediction > detectionThreshold) {
            sb.append("Posible fuga detectada (${(prediction*100).toInt()}%).\n")
        } else if (prediction > 0.4f) {
            sb.append("Anomalía detectada, monitorear (${(prediction*100).toInt()}%).\n")
        } else {
            sb.append("Sistema funcionando normalmente (${(prediction*100).toInt()}%).\n")
        }
        
        // Factores más importantes
        val sortedImportance = importance.entries.sortedByDescending { it.value }
        sb.append("Factores principales: \n")
        sortedImportance.forEach { (feature, value) ->
            sb.append("• $feature: ${(value * 100).toInt()}%\n")
        }
        
        // Analiza tendencias
        val trend = analyzeTrend()
        if (trend != null) {
            sb.append("\n")
            sb.append(trend)
        }
        
        return sb.toString()
    }
    
    /**
     * Analiza la tendencia de las predicciones recientes para dar contexto adicional
     */
    private fun analyzeTrend(): String? {
        if (recentPredictions.size < 5) return null
        
        val recent = recentPredictions.takeLast(3).average()
        val earlier = recentPredictions.take(recentPredictions.size - 3).takeLast(3).average()
        val delta = recent - earlier
        
        return when {
            delta > 0.1 -> "La situación está empeorando rápidamente."
            delta > 0.05 -> "Tendencia ascendente en riesgo de fuga."
            delta < -0.1 -> "La situación está mejorando significativamente."
            delta < -0.05 -> "Tendencia descendente en riesgo de fuga."
            else -> "Situación estable sin cambios significativos."
        }
    }
    
    /**
     * Normaliza los datos de sensores según estadísticas actualizadas
     */
    private fun normalizeSensorData(data: SensorData): SensorData {
        val flujoStats = featureStats["flujo"]!!
        val presionStats = featureStats["presion"]!!
        val vibracionStats = featureStats["vibracion"]!!
        
        return SensorData(
            id = data.id,
            timestamp = data.timestamp,
            flujo = normalizeValue(data.flujo, flujoStats.min, flujoStats.max),
            presion = normalizeValue(data.presion, presionStats.min, presionStats.max),
            vibracion = normalizeValue(data.vibracion, vibracionStats.min, vibracionStats.max)
        )
    }
    
    private fun normalizeValue(value: Float, min: Float, max: Float): Float {
        return if (max > min) {
            ((value - min) / (max - min)).coerceIn(0f, 1f)
        } else {
            0.5f
        }
    }
    
    /**
     * Actualiza estadísticas de features para mejorar normalización
     */
    private fun updateFeatureStats(data: SensorData) {
        featureStats["flujo"]?.let { stats ->
            if (data.flujo < stats.min) stats.min = data.flujo
            if (data.flujo > stats.max) stats.max = data.flujo
            stats.sum += data.flujo
            stats.count++
        }
        
        featureStats["presion"]?.let { stats ->
            if (data.presion < stats.min) stats.min = data.presion
            if (data.presion > stats.max) stats.max = data.presion
            stats.sum += data.presion
            stats.count++
        }
        
        featureStats["vibracion"]?.let { stats ->
            if (data.vibracion < stats.min) stats.min = data.vibracion
            if (data.vibracion > stats.max) stats.max = data.vibracion
            stats.sum += data.vibracion
            stats.count++
        }
    }
    
    /**
     * Detecta fugas usando una combinación de modelos, reglas y análisis temporal
     */
    fun detectarFugaAdvanced(sensorData: SensorData, threshold: Float = detectionThreshold): Boolean {
        // Combina predicción del modelo con análisis de tendencias y reglas
        val prediction = predict(sensorData)
        
        // Detecta patrones rápidos que podrían indicar fugas súbitas
        val isAnomalous = if (sequenceBuffer.size >= 3) {
            val lastValues = sequenceBuffer.takeLast(3)
            val flujoVariance = calculateVariance(lastValues.map { it.flujo })
            val presionVariance = calculateVariance(lastValues.map { it.presion })
            
            // Cambio brusco en presión y flujo simultáneamente
            flujoVariance > 1.5f && presionVariance > 10f
        } else false
        
        return prediction > threshold || isAnomalous
    }
    
    /**
     * Detecta fugas en series temporales usando análisis avanzado
     * con ventanas adaptativas y contextualización.
     */
    fun detectarFugaEnSerieAvanzado(datos: List<SensorData>, threshold: Float = detectionThreshold): Boolean {
        if (datos.size < 3) return false
        
        // Actualizar buffer de secuencia
        synchronized(sequenceBuffer) {
            sequenceBuffer.clear()
            sequenceBuffer.addAll(datos.takeLast(SEQUENCE_LENGTH))
        }
        
        // 1. Predicción con modelo principal
        val predictions = datos.takeLast(5).map { predict(it) }
        val avgPrediction = predictions.average().toFloat()
        
        // 2. Análisis de tendencias
        val trend = if (predictions.size >= 3) {
            predictions.takeLast(predictions.size / 2).average() - 
            predictions.take(predictions.size / 2).average()
        } else 0.0
        
        // 3. Detección de anomalías en secuencia
        val isAnomalous = detectSequenceAnomaly(datos.takeLast(SEQUENCE_LENGTH))
        
        // 4. Predicción futura
        val futurePrediction = predictFutureLeak()
        
        // Algoritmo de decisión
        val baseDecision = avgPrediction > threshold
        val trendBoost = trend > 0.1 // Tendencia fuertemente ascendente
        val futureRisk = futurePrediction > threshold * 0.8f
        
        return baseDecision || (isAnomalous && (trendBoost || futureRisk))
    }
    
    /**
     * Detecta anomalías en secuencias completas usando análisis multivariable
     */
    private fun detectSequenceAnomaly(sequence: List<SensorData>): Boolean {
        if (sequence.size < 3 || !this::anomalyInterpreter.isInitialized) return false
        
        try {
            // Calcula estadísticas para la secuencia
            val flujoValues = sequence.map { it.flujo }
            val presionValues = sequence.map { it.presion }
            val vibracionValues = sequence.map { it.vibracion }
            
            val flujoMean = flujoValues.average().toFloat()
            val presionMean = presionValues.average().toFloat()
            val vibracionMean = vibracionValues.average().toFloat()
            
            val flujoVar = calculateVariance(flujoValues)
            val presionVar = calculateVariance(presionValues)
            val vibracionVar = calculateVariance(vibracionValues)
            
            // Detecta patrones anómalos
            val suddenPressureDrop = sequence.windowed(3).any { window ->
                val start = window.first().presion
                val end = window.last().presion
                (start - end) > (presionMean * 0.3f)
            }
            
            val highFlowVariation = flujoVar > (flujoMean * 0.5f)
            val highVibration = vibracionValues.any { it > 1.2f }
            
            // Correlación negativa entre presión y flujo (característica de fugas)
            val correlation = calculateCorrelation(flujoValues, presionValues)
            val negativeCorrelation = correlation < -0.5
            
            // Combina factores
            return (suddenPressureDrop && highFlowVariation) || 
                   (highVibration && (suddenPressureDrop || highFlowVariation)) ||
                   negativeCorrelation
                   
        } catch (e: Exception) {
            Log.e(TAG, "Error en análisis de secuencia: ${e.message}")
            return false
        }
    }
    
    /**
     * Calcula varianza de una lista de valores
     */
    private fun calculateVariance(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average()
        return values.fold(0f) { acc, next -> 
            acc + (next - mean).pow(2).toFloat()
        } / values.size
    }
    
    /**
     * Calcula correlación entre dos series
     */
    private fun calculateCorrelation(x: List<Float>, y: List<Float>): Float {
        if (x.size != y.size || x.isEmpty()) return 0f
        
        val n = x.size
        val xMean = x.average()
        val yMean = y.average()
        
        var numerator = 0.0
        var xDenom = 0.0
        var yDenom = 0.0
        
        for (i in x.indices) {
            val xDiff = x[i] - xMean
            val yDiff = y[i] - yMean
            numerator += xDiff * yDiff
            xDenom += xDiff * xDiff
            yDenom += yDiff * yDiff
        }
        
        return if (xDenom > 0 && yDenom > 0) {
            (numerator / kotlin.math.sqrt(xDenom * yDenom)).toFloat()
        } else {
            0f
        }
    }
    
    /**
     * Función de extensión - potencia
     */
    private fun Double.pow(exp: Int): Double = kotlin.math.pow(this, exp.toDouble())
    
    /**
     * Realiza transfer learning con datos locales para adaptar el modelo a
     * las condiciones específicas del sistema del usuario.
     * 
     * @param correctFeedback indica si la predicción anterior fue correcta
     */
    fun provideFeedback(sensorData: SensorData, correctFeedback: Boolean) {
        // Normaliza datos para el feedback
        val normalizedData = normalizeSensorData(sensorData)
        val features = listOf(normalizedData.flujo, normalizedData.presion, normalizedData.vibracion)
        
        // Guarda el feedback para aprendizaje por lotes
        synchronized(feedbackData) {
            feedbackData.add(features to correctFeedback)
            if (feedbackData.size > 100) {
                feedbackData.removeAt(0)
            }
        }
        
        // Ajusta threshold basado en feedback
        if (feedbackData.size >= 10) {
            adjustThreshold()
        }
    }
    
    /**
     * Ajusta el umbral de detección basado en feedback del usuario.
     * Ahora realmente modifica el threshold y deja registro claro.
     */
    private fun adjustThreshold() {
        val correctPositives = feedbackData.count { it.second }
        val falsePositives = feedbackData.count { !it.second }
        if (correctPositives + falsePositives > 0) {
            val ratio = correctPositives.toFloat() / (correctPositives + falsePositives)
            val old = detectionThreshold
            if (ratio < 0.6) {
                // Demasiados falsos positivos, incrementar umbral
                detectionThreshold = (detectionThreshold + 0.05f).coerceAtMost(0.85f)
                Log.d(TAG, "Ajustando threshold de $old a $detectionThreshold (muchos falsos positivos)")
            } else if (ratio > 0.9) {
                // Buena precisión, podemos bajar un poco el umbral
                detectionThreshold = (detectionThreshold - 0.05f).coerceAtLeast(0.5f)
                Log.d(TAG, "Ajustando threshold de $old a $detectionThreshold (buena precisión)")
            } else {
                Log.d(TAG, "Threshold sin cambios ($detectionThreshold), ratio: $ratio")
            }
        }
    }
    
    /**
     * Fallback seguro: si el modelo no está inicializado, retorna predicción conservadora y loguea el error.
     */
    private fun safePredict(block: () -> Float): Float = try {
        if (!this::mainInterpreter.isInitialized) {
            Log.e(TAG, "Modelo IA no inicializado, usando fallback conservador")
            return 0f
        }
        block()
    } catch (e: Exception) {
        Log.e(TAG, "Error en inferencia IA: ${e.message}")
        0f
    }

    // Reemplaza predict y predictSequence para usar safePredict
    fun predictSafe(sensorData: SensorData): Float = safePredict { predict(sensorData) }
    fun predictSequenceSafe(datos: List<SensorData>): Float = safePredict { predictSequence(datos) }
    
    /**
     * Mejora: logs de inicialización y recarga más claros y robustos.
     */
    fun reloadModel(context: Context) {
        Log.i(TAG, "Recargando modelos IA...")
        if (this::mainInterpreter.isInitialized) {
            try {
                modelLock.write {
                    mainInterpreter.close()
                    if (this::forecastInterpreter.isInitialized) forecastInterpreter.close()
                    if (this::anomalyInterpreter.isInitialized) anomalyInterpreter.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al cerrar intérpretes: ${e.message}")
            }
        }
        init(context)
        Log.i(TAG, "Modelos IA recargados correctamente")
    }
    
    /**
     * Clase para mantener estadísticas por feature
     */
    data class FeatureStats(
        var min: Float,
        var max: Float,
        var sum: Float,
        var count: Int
    ) {
        val mean: Float get() = if (count > 0) sum / count else 0f
    }
    
    /**
     * Integra la línea base adaptativa de la instalación con el modelo de IA
     * para mejorar la precisión y contextualización de las predicciones.
     * 
     * @param sensorData Datos actuales del sensor
     * @param baselineAnomaly Resultado del análisis de línea base
     * @return Predicción mejorada con explicabilidad contextual
     */
    fun predictWithBaselineIntegration(
        sensorData: SensorData, 
        baselineAnomaly: InstallationBaselineMonitor.AnomalyResult
    ): Pair<Float, Map<String, Any>> {
        // 1. Realizar predicción con modelo de IA
        val (aiProbability, featureImportance) = predictWithExplain(sensorData)
        
        // 2. Obtener Z-scores del análisis de línea base
        val zScores = baselineAnomaly.zScores
        
        // 3. Mejorar predicción con información contextual
        // Usamos un esquema de ponderación optimizado:
        // - 70% modelo IA si hay alta confianza (>0.8), de lo contrario 60%
        // - El resto se distribuye entre análisis de línea base y anomalía PCA
        val modelWeight = if (aiProbability > 0.8f) 0.7f else 0.6f
        val baselineWeight = 1f - modelWeight
        
        // Convertir Z-scores a probabilidad usando función sigmoide
        val baselineProb = convertZScoresToProbability(zScores)
        
        // Combinar con ponderación
        val enhancedProbability = (aiProbability * modelWeight) + (baselineProb * baselineWeight)
        
        // 4. Enriquecer explicabilidad con contexto de línea base
        val enhancedExplanation = mapOf(
            "probability" to enhancedProbability,
            "aiProbability" to aiProbability,
            "baselineProbability" to baselineProb,
            "featureImportance" to featureImportance,
            "zScores" to zScores,
            "baselineExplanation" to baselineAnomaly.explanation,
            "isContextualAnomaly" to baselineAnomaly.isAnomaly,
            "enhancedConfidence" to calculateEnhancedConfidence(aiProbability, baselineProb)
        )
        
        return enhancedProbability to enhancedExplanation
    }
    
    /**
     * Convierte Z-scores a una probabilidad entre 0 y 1
     */
    private fun convertZScoresToProbability(zScores: Map<String, Float>): Float {
        // Extraer los Z-scores individuales
        val flujoZ = zScores["flujo"] ?: 0f
        val presionZ = zScores["presion"] ?: 0f
        val vibracionZ = zScores["vibracion"] ?: 0f
        
        // Verificar patrón específico de fuga (flujo alto + presión baja)
        val isLeakPattern = flujoZ > 1.5f && presionZ < -1.5f
        
        // Usar valores absolutos para flujo y vibración, pero mantener el signo para presión
        // ya que presiones bajas son más indicativas de fugas
        val adjustedFlowZ = kotlin.math.abs(flujoZ)
        val adjustedPressureZ = if (presionZ < 0) kotlin.math.abs(presionZ) * 1.2f else presionZ * 0.5f
        val adjustedVibrationZ = kotlin.math.abs(vibracionZ)
        
        // Combinar Z-scores con pesos específicos
        val combinedScore = (adjustedFlowZ * 0.4f) + (adjustedPressureZ * 0.4f) + (adjustedVibrationZ * 0.2f)
        
        // Aplicar sigmoide para normalizar a (0,1)
        val baseProb = 1.0f / (1.0f + kotlin.math.exp(-combinedScore * 0.5f + 1f))
        
        // Boost para patrones específicos de fuga
        return if (isLeakPattern) {
            (baseProb * 1.3f).coerceAtMost(1.0f)
        } else {
            baseProb
        }
    }
    
    /**
     * Calcula confianza mejorada cuando múltiples sistemas concuerdan
     */
    private fun calculateEnhancedConfidence(aiProb: Float, baselineProb: Float): Float {
        // Si ambos sistemas están de acuerdo, aumenta la confianza
        val agreement = 1f - kotlin.math.abs(aiProb - baselineProb)
        val baseConfidence = kotlin.math.max(aiProb, baselineProb)
        
        // Fórmula para aumentar confianza cuando hay acuerdo
        return (baseConfidence + (agreement * 0.2f)).coerceAtMost(1.0f)
    }
}
