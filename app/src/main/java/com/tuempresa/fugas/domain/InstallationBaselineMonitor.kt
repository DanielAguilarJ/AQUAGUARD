package com.tuempresa.fugas.domain

import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.tuempresa.fugas.model.SensorData
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.ByteBuffer
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TreeMap
import kotlin.math.max
import kotlin.math.min

/**
 * Sistema de monitoreo inteligente que establece una línea base normal de comportamiento
 * para cada instalación y detecta desviaciones significativas en tiempo real.
 * 
 * Este mecanismo de "aprendizaje automático adaptativo" ajusta continuamente
 * su comprensión de patrones normales y anómalos específicos de cada instalación.
 */
object InstallationBaselineMonitor {
    private const val TAG = "BaselineMonitor"
    private const val MAX_BASELINE_SAMPLES = 1000
    private const val MIN_BASELINE_SAMPLES = 100
    private const val BASELINE_FILE = "installation_baseline.dat"
    
    // Almacenamiento de línea base como perfil de la instalación
    private data class BaselineProfile(
        val flujoMean: Float = 0f,
        val flujoStdDev: Float = 0f,
        val presionMean: Float = 0f,
        val presionStdDev: Float = 0f,
        val vibracionMean: Float = 0f,
        val vibracionStdDev: Float = 0f,
        // Patrones por hora del día
        val hourlyPatterns: Map<Int, HourlyPattern> = mutableMapOf(),
        // Correlaciones
        val flujoPressureCorrelation: Float = 0f,
        // Última actualización
        val lastUpdated: Long = 0
    )
    
    private data class HourlyPattern(
        val flujoMean: Float = 0f,
        val flujoStdDev: Float = 0f,
        val presionMean: Float = 0f,
        val presionStdDev: Float = 0f,
        val vibracionMean: Float = 0f,
        val sampleCount: Int = 0
    )
    
    // Cache de muestras históricas para establecer la línea base
    private val baselineSamples = mutableListOf<SensorData>()
    
    // Perfil actual de la instalación
    private var currentProfile = BaselineProfile()
    
    // Estadísticas en tiempo real para la sesión actual
    private val recentAnomalyScores = mutableListOf<Float>()
    
    // Flag para indicar si el sistema está calibrado
    private var isCalibrated = false
    
    // Patrones cíclicos por hora y día de la semana
    private val hourlyPatterns = TreeMap<Int, MutableList<SensorData>>()
    
    /**
     * Inicializa el monitor de línea base y carga el perfil existente si está disponible
     */
    fun init(context: Context) {
        try {
            loadBaselineProfile(context)
            isCalibrated = baselineSamples.size > MIN_BASELINE_SAMPLES
            Log.i(TAG, "Perfil de línea base cargado: ${if (isCalibrated) "calibrado" else "en calibración"}, " +
                       "${baselineSamples.size} muestras")
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar perfil de línea base: ${e.message}")
            isCalibrated = false
        }
    }
    
    /**
     * Procesa nuevas lecturas para actualizar la línea base y detectar anomalías
     * @return Puntuación de anomalía (0-1) donde valores más altos indican mayor anomalía
     */
    fun processSensorReading(sensorData: SensorData): Float {
        // Añadir a muestras para línea base si estamos en fase de calibración
        if (!isCalibrated || baselineSamples.size < MAX_BASELINE_SAMPLES) {
            synchronized(baselineSamples) {
                baselineSamples.add(sensorData)
                if (baselineSamples.size > MAX_BASELINE_SAMPLES) {
                    baselineSamples.removeAt(0)
                }
                
                // Actualizar si tenemos suficientes muestras
                if (baselineSamples.size >= MIN_BASELINE_SAMPLES && !isCalibrated) {
                    updateBaselineProfile()
                    isCalibrated = true
                }
            }
        }
        
        // Actualizar patrones horarios
        updateHourlyPattern(sensorData)
        
        // Calcular anomalía basada en desviación de la línea base
        val anomalyScore = if (isCalibrated) {
            calculateAnomalyScore(sensorData)
        } else {
            // Durante la calibración, devolver puntuación basada en reglas simples
            calculateBasicAnomalyScore(sensorData)
        }
        
        // Registrar para análisis de tendencias
        synchronized(recentAnomalyScores) {
            recentAnomalyScores.add(anomalyScore)
            if (recentAnomalyScores.size > 20) {
                recentAnomalyScores.removeAt(0)
            }
        }
        
        return anomalyScore
    }
    
    /**
     * Calcula una puntuación de anomalía basada en desviaciones de la línea base
     * considerando patrones temporales y contextuales.
     */
    private fun calculateAnomalyScore(data: SensorData): Float {
        val hour = getHourFromTimestamp(data.timestamp)
        
        // Obtener patrón horario si existe
        val hourlyPattern = currentProfile.hourlyPatterns[hour]
        
        // Calcular Z-scores para cada variable
        val flujoZScore = if (hourlyPattern != null && hourlyPattern.sampleCount > 10) {
            calculateZScore(data.flujo, hourlyPattern.flujoMean, hourlyPattern.flujoStdDev)
        } else {
            calculateZScore(data.flujo, currentProfile.flujoMean, currentProfile.flujoStdDev)
        }
        
        val presionZScore = if (hourlyPattern != null && hourlyPattern.sampleCount > 10) {
            calculateZScore(data.presion, hourlyPattern.presionMean, hourlyPattern.presionStdDev)
        } else {
            calculateZScore(data.presion, currentProfile.presionMean, currentProfile.presionStdDev)
        }
        
        val vibracionZScore = if (hourlyPattern != null && hourlyPattern.sampleCount > 10) {
            calculateZScore(data.vibracion, hourlyPattern.vibracionMean, hourlyPattern.vibracionStdDev)
        } else {
            calculateZScore(data.vibracion, currentProfile.vibracionMean, currentProfile.vibracionStdDev)
        }
        
        // Verificar correlación flujo-presión (normalmente negativa en fugas)
        val correlationScore = if (flujoZScore > 1.0f && presionZScore < -1.0f) {
            // Anticorrelación (flujo alto, presión baja) -> más anomalía
            min(max(flujoZScore, -presionZScore) * 0.2f, 1.0f)
        } else {
            0f
        }
        
        // Pesos para diferentes factores
        val flujoWeight = 0.35f
        val presionWeight = 0.35f
        val vibracionWeight = 0.2f
        val correlationWeight = 0.1f
        
        // Calcular anomalía ponderada y normalizada (0-1)
        var totalScore = (
            flujoWeight * min(abs(flujoZScore) / 3f, 1f) +
            presionWeight * min(abs(presionZScore) / 3f, 1f) +
            vibracionWeight * min(abs(vibracionZScore) / 3f, 1f) +
            correlationWeight * correlationScore
        )
        
        // Limitar a 0-1
        return totalScore.coerceIn(0f, 1f)
    }
    
    /**
     * Calcula puntuación Z (cuántas desviaciones estándar se desvía del promedio)
     */
    private fun calculateZScore(value: Float, mean: Float, stdDev: Float): Float {
        return if (stdDev > 0) (value - mean) / stdDev else 0f
    }
    
    /**
     * Calcula una puntuación básica de anomalía durante la fase de calibración
     */
    private fun calculateBasicAnomalyScore(data: SensorData): Float {
        // Reglas básicas en ausencia de perfil completo
        val isFlowHigh = data.flujo > 6.0f
        val isPressureLow = data.presion < 50.0f
        val isVibrationHigh = data.vibracion > 0.8f
        
        var score = 0f
        
        // Flujo alto y presión baja juntos son muy sospechosos
        if (isFlowHigh && isPressureLow) score += 0.8f
        
        // Vibración alta con cualquier otra anomalía
        if (isVibrationHigh && (isFlowHigh || isPressureLow)) score += 0.7f
        
        // Solo uno anómalo
        if (isFlowHigh && !isPressureLow && !isVibrationHigh) score += 0.4f
        if (isPressureLow && !isFlowHigh && !isVibrationHigh) score += 0.4f
        if (isVibrationHigh && !isFlowHigh && !isPressureLow) score += 0.5f
        
        return score.coerceIn(0f, 1f)
    }
    
    /**
     * Actualiza el patrón horario para la hora actual
     */
    private fun updateHourlyPattern(data: SensorData) {
        val hour = getHourFromTimestamp(data.timestamp)
        synchronized(hourlyPatterns) {
            val hourData = hourlyPatterns.getOrPut(hour) { mutableListOf() }
            hourData.add(data)
            
            // Limitar muestras por hora
            if (hourData.size > 100) {
                hourData.removeAt(0)
            }
        }
    }
    
    /**
     * Extrae la hora del día del timestamp
     */
    private fun getHourFromTimestamp(timestamp: Long): Int {
        return try {
            val instant = Instant.ofEpochMilli(timestamp)
            val localTime = instant.atZone(ZoneId.systemDefault())
            localTime.hour
        } catch (e: Exception) {
            // Fallback a hora actual del sistema si hay error
            val cal = java.util.Calendar.getInstance()
            cal.timeInMillis = System.currentTimeMillis()
            cal.get(java.util.Calendar.HOUR_OF_DAY)
        }
    }
    
    /**
     * Actualiza el perfil de línea base con las muestras acumuladas
     */
    private fun updateBaselineProfile() {
        synchronized(baselineSamples) {
            if (baselineSamples.isEmpty()) return
            
            // Calcular estadísticas generales
            val flujoValues = baselineSamples.map { it.flujo }
            val presionValues = baselineSamples.map { it.presion }
            val vibracionValues = baselineSamples.map { it.vibracion }
            
            val flujoMean = flujoValues.average().toFloat()
            val presionMean = presionValues.average().toFloat()
            val vibracionMean = vibracionValues.average().toFloat()
            
            val flujoStdDev = calculateStdDev(flujoValues, flujoMean)
            val presionStdDev = calculateStdDev(presionValues, presionMean)
            val vibracionStdDev = calculateStdDev(vibracionValues, vibracionMean)
            
            // Calcular correlación flujo-presión
            val flujoPressureCorrelation = calculateCorrelation(flujoValues, presionValues)
            
            // Calcular patrones horarios
            val hourlyPatternMap = mutableMapOf<Int, HourlyPattern>()
            
            // Agrupar por hora
            val groupedByHour = baselineSamples.groupBy { getHourFromTimestamp(it.timestamp) }
            
            groupedByHour.forEach { (hour, samples) ->
                if (samples.size >= 10) { // Mínimo de muestras para ser significativo
                    val hourlyFlujoMean = samples.map { it.flujo }.average().toFloat()
                    val hourlyFlujoStdDev = calculateStdDev(samples.map { it.flujo }, hourlyFlujoMean)
                    val hourlyPresionMean = samples.map { it.presion }.average().toFloat()
                    val hourlyPresionStdDev = calculateStdDev(samples.map { it.presion }, hourlyPresionMean)
                    val hourlyVibracionMean = samples.map { it.vibracion }.average().toFloat()
                    val hourlyVibracionStdDev = calculateStdDev(samples.map { it.vibracion }, hourlyVibracionMean)
                    
                    hourlyPatternMap[hour] = HourlyPattern(
                        flujoMean = hourlyFlujoMean,
                        flujoStdDev = hourlyFlujoStdDev,
                        presionMean = hourlyPresionMean,
                        presionStdDev = hourlyPresionStdDev,
                        vibracionMean = hourlyVibracionMean,
                        sampleCount = samples.size
                    )
                }
            }
            
            // Actualizar perfil
            currentProfile = BaselineProfile(
                flujoMean = flujoMean,
                flujoStdDev = flujoStdDev,
                presionMean = presionMean,
                presionStdDev = presionStdDev,
                vibracionMean = vibracionMean,
                vibracionStdDev = vibracionStdDev,
                hourlyPatterns = hourlyPatternMap,
                flujoPressureCorrelation = flujoPressureCorrelation,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Guarda el perfil de línea base en almacenamiento persistente
     */
    fun saveBaselineProfile(context: Context) {
        try {
            val file = File(context.filesDir, BASELINE_FILE)
            BufferedWriter(FileWriter(file)).use { writer ->
                // Guardar estadísticas generales
                writer.write("${currentProfile.flujoMean},${currentProfile.flujoStdDev},")
                writer.write("${currentProfile.presionMean},${currentProfile.presionStdDev},")
                writer.write("${currentProfile.vibracionMean},${currentProfile.vibracionStdDev},")
                writer.write("${currentProfile.flujoPressureCorrelation},${currentProfile.lastUpdated}\n")
                
                // Guardar patrones horarios
                writer.write("${currentProfile.hourlyPatterns.size}\n")
                currentProfile.hourlyPatterns.forEach { (hour, pattern) ->
                    writer.write("$hour,${pattern.flujoMean},${pattern.flujoStdDev},")
                    writer.write("${pattern.presionMean},${pattern.presionStdDev},")
                    writer.write("${pattern.vibracionMean},${pattern.sampleCount}\n")
                }
                
                Log.i(TAG, "Perfil de línea base guardado exitosamente")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al guardar perfil de línea base: ${e.message}")
        }
    }
    
    /**
     * Carga el perfil de línea base desde almacenamiento
     */
    private fun loadBaselineProfile(context: Context) {
        try {
            val file = File(context.filesDir, BASELINE_FILE)
            if (!file.exists()) {
                Log.i(TAG, "No existe perfil de línea base previo")
                return
            }
            
            val lines = file.readLines()
            if (lines.isEmpty()) return
            
            // Leer estadísticas generales
            val generalStats = lines[0].split(",")
            if (generalStats.size >= 8) {
                val flujoMean = generalStats[0].toFloat()
                val flujoStdDev = generalStats[1].toFloat()
                val presionMean = generalStats[2].toFloat()
                val presionStdDev = generalStats[3].toFloat()
                val vibracionMean = generalStats[4].toFloat()
                val vibracionStdDev = generalStats[5].toFloat()
                val flujoPressureCorrelation = generalStats[6].toFloat()
                val lastUpdated = generalStats[7].toLong()
                
                // Leer patrones horarios
                val hourlyPatternMap = mutableMapOf<Int, HourlyPattern>()
                
                if (lines.size >= 2) {
                    val numPatterns = lines[1].toIntOrNull() ?: 0
                    for (i in 2 until min(2 + numPatterns, lines.size)) {
                        val patternParts = lines[i].split(",")
                        if (patternParts.size >= 7) {
                            val hour = patternParts[0].toInt()
                            hourlyPatternMap[hour] = HourlyPattern(
                                flujoMean = patternParts[1].toFloat(),
                                flujoStdDev = patternParts[2].toFloat(),
                                presionMean = patternParts[3].toFloat(),
                                presionStdDev = patternParts[4].toFloat(),
                                vibracionMean = patternParts[5].toFloat(),
                                sampleCount = patternParts[6].toInt()
                            )
                        }
                    }
                }
                
                // Actualizar perfil
                currentProfile = BaselineProfile(
                    flujoMean = flujoMean,
                    flujoStdDev = flujoStdDev,
                    presionMean = presionMean,
                    presionStdDev = presionStdDev,
                    vibracionMean = vibracionMean,
                    vibracionStdDev = vibracionStdDev,
                    hourlyPatterns = hourlyPatternMap,
                    flujoPressureCorrelation = flujoPressureCorrelation,
                    lastUpdated = lastUpdated
                )
                
                // Recrear algunas muestras sintéticas para baseline
                generateSyntheticSamples()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al cargar perfil de línea base: ${e.message}")
        }
    }
    
    /**
     * Genera muestras sintéticas basadas en perfil para mantener calibración
     */
    private fun generateSyntheticSamples() {
        synchronized(baselineSamples) {
            baselineSamples.clear()
            
            // Generar muestras representativas basadas en los patrones guardados
            val now = System.currentTimeMillis()
            val random = java.util.Random()
            
            // Por cada hora con patrón, generar muestras sintéticas
            currentProfile.hourlyPatterns.forEach { (hour, pattern) ->
                repeat(min(pattern.sampleCount, 20)) {
                    // Generar con distribución normal alrededor de la media
                    val flujo = randomNormal(pattern.flujoMean, pattern.flujoStdDev, random)
                    val presion = randomNormal(pattern.presionMean, pattern.presionStdDev, random)
                    val vibracion = randomNormal(pattern.vibracionMean, pattern.vibracionStdDev, random)
                    
                    // Timestamp sintético para esta hora
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = now
                    cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
                    cal.set(java.util.Calendar.MINUTE, random.nextInt(60))
                    
                    baselineSamples.add(
                        SensorData(
                            id = "synthetic_${baselineSamples.size}",
                            timestamp = cal.timeInMillis,
                            flujo = flujo,
                            presion = presion,
                            vibracion = vibracion
                        )
                    )
                }
            }
            
            // Si no hay suficientes muestras por hora, generar algunas generales
            if (baselineSamples.size < MIN_BASELINE_SAMPLES) {
                repeat(MIN_BASELINE_SAMPLES - baselineSamples.size) {
                    val flujo = randomNormal(currentProfile.flujoMean, currentProfile.flujoStdDev, random)
                    val presion = randomNormal(currentProfile.presionMean, currentProfile.presionStdDev, random)
                    val vibracion = randomNormal(currentProfile.vibracionMean, currentProfile.vibracionStdDev, random)
                    
                    baselineSamples.add(
                        SensorData(
                            id = "synthetic_general_${baselineSamples.size}",
                            timestamp = now - random.nextInt(24 * 60 * 60 * 1000),
                            flujo = flujo,
                            presion = presion,
                            vibracion = vibracion
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Genera un número aleatorio con distribución normal
     */
    private fun randomNormal(mean: Float, stdDev: Float, random: java.util.Random): Float {
        val u1 = 1.0f - random.nextFloat() // (0,1]
        val u2 = 1.0f - random.nextFloat()
        val randStdNormal = kotlin.math.sqrt(-2.0f * kotlin.math.ln(u1)) *
                            kotlin.math.sin(2.0f * kotlin.math.PI.toFloat() * u2)
        return mean + stdDev * randStdNormal
    }
    
    /**
     * Calcula la desviación estándar de una lista de valores
     */
    private fun calculateStdDev(values: List<Float>, mean: Float): Float {
        if (values.isEmpty()) return 0f
        val variance = values.fold(0f) { acc, value ->
            acc + (value - mean) * (value - mean)
        } / values.size
        return kotlin.math.sqrt(variance)
    }
    
    /**
     * Calcula la correlación entre dos series de valores
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
     * Obtener explicación de la anomalía basada en el contexto
     */
    fun explainAnomaly(data: SensorData, anomalyScore: Float): String {
        if (anomalyScore < 0.4f) {
            return "Comportamiento normal dentro de los parámetros esperados."
        }
        
        val sb = StringBuilder()
        
        // Calcular desviaciones específicas
        val hour = getHourFromTimestamp(data.timestamp)
        val hourPattern = currentProfile.hourlyPatterns[hour]
        
        val flujoMean = hourPattern?.flujoMean ?: currentProfile.flujoMean
        val flujoStdDev = hourPattern?.flujoStdDev ?: currentProfile.flujoStdDev
        val presionMean = hourPattern?.presionMean ?: currentProfile.presionMean
        val presionStdDev = hourPattern?.presionStdDev ?: currentProfile.presionStdDev
        val vibracionMean = hourPattern?.vibracionMean ?: currentProfile.vibracionMean
        val vibracionStdDev = hourPattern?.vibracionStdDev ?: currentProfile.vibracionStdDev
        
        val flujoZScore = calculateZScore(data.flujo, flujoMean, flujoStdDev)
        val presionZScore = calculateZScore(data.presion, presionMean, presionStdDev)
        val vibracionZScore = calculateZScore(data.vibracion, vibracionMean, vibracionStdDev)
        
        sb.append("Anomalía detectada (${(anomalyScore * 100).toInt()}%):\n")
        
        // Analizar patrones de fuga típicos
        if (flujoZScore > 1.5f && presionZScore < -1.5f) {
            sb.append("• Patrón crítico: Flujo alto con presión baja, indicador principal de fuga.\n")
        } else if (flujoZScore > 2.0f) {
            sb.append("• Flujo anormalmente alto: ${formatSigma(flujoZScore)}.\n")
        } else if (presionZScore < -2.0f) {
            sb.append("• Presión anormalmente baja: ${formatSigma(presionZScore)}.\n")
        }
        
        if (vibracionZScore > 2.0f) {
            sb.append("• Vibración anormal detectada: ${formatSigma(vibracionZScore)}.\n")
        }
        
        // Contextualización por hora
        if (hourPattern != null) {
            val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
            val instant = Instant.ofEpochMilli(data.timestamp)
            val localTime = instant.atZone(ZoneId.systemDefault())
            
            sb.append("• En contexto: Este es un ${if (anomalyScore > 0.7f) "comportamiento muy inusual" else "comportamiento inusual"} ")
            sb.append("para esta hora (${localTime.format(timeFormatter)}).\n")
        }
        
        // Análisis de tendencia
        if (recentAnomalyScores.size >= 5) {
            val recent = recentAnomalyScores.takeLast(3).average()
            val earlier = recentAnomalyScores.take(recentAnomalyScores.size - 3).takeLast(3).average()
            val delta = recent - earlier
            
            if (delta > 0.1) {
                sb.append("• Tendencia: La situación está empeorando con el tiempo.\n")
            } else if (delta < -0.1) {
                sb.append("• Tendencia: La situación parece estar mejorando.\n")
            }
        }
        
        return sb.toString()
    }
    
    /**
     * Formatea la puntuación Z a una descripción de sigma
     */
    private fun formatSigma(zScore: Float): String {
        val absZ = kotlin.math.abs(zScore)
        return when {
            absZ > 3 -> "extremo (${String.format("%.1f", absZ)}σ)"
            absZ > 2 -> "muy significativo (${String.format("%.1f", absZ)}σ)"
            else -> "significativo (${String.format("%.1f", absZ)}σ)"
        }
    }
    
    // Utilidad para valor absoluto
    private fun abs(value: Float): Float = kotlin.math.abs(value)
    
    /**
     * Indica si el sistema está completamente calibrado
     */
    fun isSystemCalibrated(): Boolean = isCalibrated
    
    /**
     * Obtiene el progreso de calibración como porcentaje (0-100)
     */
    fun getCalibrationProgress(): Int {
        return if (isCalibrated) 100 else ((baselineSamples.size * 100f) / MIN_BASELINE_SAMPLES).toInt().coerceIn(0, 99)
    }
    
    /**
     * Obtiene estadísticas de línea base actuales para visualización
     */
    fun getBaselineStats(): Map<String, Any> {
        return mapOf(
            "flujoMean" to currentProfile.flujoMean,
            "flujoStdDev" to currentProfile.flujoStdDev,
            "presionMean" to currentProfile.presionMean,
            "presionStdDev" to currentProfile.presionStdDev,
            "vibracionMean" to currentProfile.vibracionMean,
            "vibracionStdDev" to currentProfile.vibracionStdDev,
            "hourlyPatternCount" to currentProfile.hourlyPatterns.size,
            "sampleCount" to baselineSamples.size,
            "isCalibrated" to isCalibrated,
            "lastUpdated" to currentProfile.lastUpdated
        )
    }
}
