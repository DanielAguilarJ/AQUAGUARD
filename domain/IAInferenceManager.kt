package com.tuempresa.fugas.domain

import android.content.Context
import com.tuempresa.fugas.model.SensorData
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import android.content.res.AssetFileDescriptor
import java.io.File
import com.tuempresa.fugas.domain.ModelUpdater

/**
 * Manager para realizar inferencia de IA usando un modelo TensorFlow Lite.
 */
object IAInferenceManager {
    private lateinit var interpreter: Interpreter
    private lateinit var inputBuffer: ByteBuffer
    private lateinit var outputBuffer: ByteBuffer

    fun init(context: Context) {
        val localModel = ModelUpdater.getModelFile(context)
        val modelBuffer = if (localModel != null) {
            FileInputStream(localModel).channel.map(FileChannel.MapMode.READ_ONLY, 0, localModel.length())
        } else {
            loadModelFile(context, "leak_detection.tflite")
        }
        // Configurar interpreter con opciones optimizadas
        val options = Interpreter.Options().apply {
            setNumThreads(4)
            // Puedes añadir delegados de GPU/NPU aquí si están disponibles
        }
        interpreter = Interpreter(modelBuffer, options)
        // Inicializar buffers reutilizables
        inputBuffer = ByteBuffer.allocateDirect(3 * java.lang.Float.SIZE / 8).order(ByteOrder.nativeOrder())
        outputBuffer = ByteBuffer.allocateDirect(java.lang.Float.SIZE / 8).order(ByteOrder.nativeOrder())
    }

    private fun loadModelFile(context: Context, modelPath: String): MappedByteBuffer {
        val afd: AssetFileDescriptor = context.assets.openFd(modelPath)
        FileInputStream(afd.fileDescriptor).use { input ->
            val channel: FileChannel = input.channel
            return channel.map(
                FileChannel.MapMode.READ_ONLY,
                afd.startOffset,
                afd.declaredLength
            )
        }
    }

    /**
     * Devuelve la probabilidad de fuga (0.0 - 1.0) para una lectura de sensor.
     */
    fun predict(sensorData: SensorData): Float {
        // Reset buffers
        inputBuffer.clear()
        outputBuffer.clear()
        // Llenar input
        inputBuffer.asFloatBuffer().put(floatArrayOf(sensorData.flujo, sensorData.presion, sensorData.vibracion))
        // Inferencia
        interpreter.run(inputBuffer, outputBuffer)
        outputBuffer.rewind()
        return outputBuffer.asFloatBuffer().get() // obtener el primer valor
    }

    /**
     * Realiza inferencia en una serie de lecturas y devuelve la probabilidad media.
     */
    fun predictSequence(datos: List<SensorData>): Float {
        if (datos.isEmpty()) return 0f
        val probs = datos.map { predict(it) }
        return probs.average().toFloat()
    }

    /**
     * Indica si la probabilidad supera el umbral.
     */
    fun detectarFuga(sensorData: SensorData, threshold: Float = 0.5f): Boolean {
        return predict(sensorData) > threshold
    }

    /**
     * Detecta fuga usando lógica de IA en una serie temporal.
     */
    fun detectarFugaEnSerieIA(datos: List<SensorData>, threshold: Float = 0.5f): Boolean {
        return predictSequence(datos) > threshold
    }

    /**
     * Devuelve la probabilidad de fuga (0.0 - 1.0) para una lectura de sensor.
     * Además, devuelve un mapa con explicabilidad avanzada (feature importance por sensibilidad).
     */
    fun predictWithExplain(sensorData: SensorData): Pair<Float, Map<String, Float>> {
        // Normalización simple (puedes ajustar con valores reales de tu dataset)
        fun norm(x: Float, min: Float, max: Float) = ((x - min) / (max - min)).coerceIn(0f, 1f)
        val flujoNorm = norm(sensorData.flujo, 0f, 10f)
        val presionNorm = norm(sensorData.presion, 0f, 200f)
        val vibracionNorm = norm(sensorData.vibracion, 0f, 2f)
        val normData = sensorData.copy(flujo = flujoNorm, presion = presionNorm, vibracion = vibracionNorm)
        val prob = predict(normData)
        // Explicabilidad: sensibilidad de la predicción a cada feature
        val base = prob
        val delta = 0.05f
        val flujoUp = predict(normData.copy(flujo = (flujoNorm + delta).coerceAtMost(1f)))
        val presionUp = predict(normData.copy(presion = (presionNorm + delta).coerceAtMost(1f)))
        val vibracionUp = predict(normData.copy(vibracion = (vibracionNorm + delta).coerceAtMost(1f)))
        val importancias = mapOf(
            "Flujo" to kotlin.math.abs(flujoUp - base),
            "Presión" to kotlin.math.abs(presionUp - base),
            "Vibración" to kotlin.math.abs(vibracionUp - base)
        )
        // Normalizar importancias
        val suma = importancias.values.sum() + 0.0001f
        val importanciasNorm = importancias.mapValues { it.value / suma }
        return prob to importanciasNorm
    }

    /**
     * Devuelve la predicción y explicabilidad para una serie temporal (promedio de importancias).
     */
    fun predictSequenceWithExplain(datos: List<SensorData>): Pair<Float, Map<String, Float>> {
        if (datos.isEmpty()) return 0f to emptyMap()
        val results = datos.map { predictWithExplain(it) }
        val avgProb = results.map { it.first }.average().toFloat()
        val avgImportancias = mutableMapOf<String, Float>()
        results.forEach { (_, imp) ->
            imp.forEach { (k, v) ->
                avgImportancias[k] = (avgImportancias[k] ?: 0f) + v
            }
        }
        avgImportancias.forEach { (k, v) -> avgImportancias[k] = v / results.size }
        return avgProb to avgImportancias
    }

    fun reloadModel(context: Context) {
        // Cierra el interpreter anterior si existe
        if (this::interpreter.isInitialized) {
            try { interpreter.close() } catch (_: Exception) {}
        }
        init(context)
    }
}