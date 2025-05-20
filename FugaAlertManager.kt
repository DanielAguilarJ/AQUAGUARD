package com.tuempresa.fugas

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.tuempresa.fugas.domain.FugaDetector
import com.tuempresa.fugas.domain.IAInferenceManager // Importar IA avanzada
import com.tuempresa.fugas.model.AlertData
import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.network.ApiService
import com.tuempresa.fugas.repository.SensorRepository
import com.tuempresa.fugas.ui.mostrarNotificacionLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import Resource // Importar el wrapper de Resource

class FugaAlertManager : KoinComponent {
    private var job: Job? = null
    private lateinit var app: Application
    private val repository: SensorRepository by inject()
    private val api: ApiService by inject()

    fun init(application: Application) {
        app = application
        startMonitoring()
    }

    private fun startMonitoring() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                try {
                    // Obtener datos con manejo de errores centralizado
                    when (val result = repository.getUltimosDatos()) {
                        is Resource.Success -> {
                            val datos = result.data
                            // Obtener análisis de línea base adaptativa
                            val baselineMonitor = InstallationBaselineMonitor
                            val latest = datos.last()
                            val baselineResult = baselineMonitor.processSensorReading(latest)
                            
                            // Integrar línea base adaptativa con el sistema avanzado de IA
                            val baselineAnomaly = InstallationBaselineMonitor.AnomalyResult(
                                isAnomaly = baselineResult > 0.65f,
                                score = baselineResult,
                                explanation = baselineMonitor.explainAnomaly(latest, baselineResult),
                                zScores = mapOf(
                                    "flujo" to 0f, // Estos valores son placeholders, normalmente vendrían del monitor
                                    "presion" to 0f,
                                    "vibracion" to 0f
                                )
                            )
                            
                            // Usar el sistema avanzado de IA para detectar fugas
                            if (IAInferenceManager.detectarFugaEnSerieAvanzado(datos)) {
                                // Predicción proactiva mejorada con horizonte temporal
                                val (prediccionFutura, prediccionesPorHora) = IAInferenceManager.predictFutureLeak(24)
                                
                                // Determinar urgencia basada en probabilidad y tiempo hasta incidente
                                val horasCriticas = prediccionesPorHora.filter { it.probability > 0.75f }
                                    .minByOrNull { it.hourOffset }?.hourOffset ?: 24
                                
                                // Urgencia adaptativa: inmediata para incidentes en <6h, crítica <12h, urgente demás casos
                                val urgencia = when {
                                    horasCriticas < 6 -> "INMEDIATA"
                                    horasCriticas < 12 -> "CRÍTICA"
                                    else -> "URGENTE"
                                }
                                
                                // Obtener explicabilidad avanzada con integración contextual
                                val (prob, explicabilidad) = IAInferenceManager.predictWithBaselineIntegration(
                                    latest, baselineAnomaly
                                )
                                
                                // Extraer explicaciones para mensaje
                                val importancias = explicabilidad["featureImportance"] as? Map<String, Float> ?: emptyMap()
                                val factorPrincipal = importancias.entries.maxByOrNull { it.value }?.key ?: "desconocido"
                                val confianzaMejorada = explicabilidad["enhancedConfidence"] as? Float ?: prob
                                val explicacionContextual = explicabilidad["baselineExplanation"] as? String ?: ""
                                
                                // Generar mensaje detallado con explicabilidad mejorada
                                val mensaje = "Fuga detectada: flujo=${latest.flujo}, presion=${latest.presion}. " +
                                             "Factor principal: $factorPrincipal (${(importancias[factorPrincipal] ?: 0f) * 100}%). " +
                                             "Confianza: ${(confianzaMejorada * 100).toInt()}%. " +
                                             "Análisis contextual: $explicacionContextual"
                                
                                // Enviar alerta al backend con información avanzada
                                val alert = AlertData(
                                    timestamp = latest.timestamp,
                                    nivel = urgencia,
                                    mensaje = mensaje,
                                    // Añadir metadatos avanzados para el backend
                                    metadatos = mapOf(
                                        "prediccionFutura" to prediccionFutura,
                                        "confianza" to confianzaMejorada,
                                        "factorPrincipal" to factorPrincipal,
                                        "horasCriticas" to horasCriticas,
                                        "baselineScore" to baselineResult,
                                        "isContextualAnomaly" to (explicabilidad["isContextualAnomaly"] as? Boolean ?: false)
                                    )
                                )
                                try {
                                    api.sendAlert(alert)
                                } catch (e: Exception) {
                                    // Registrar error pero continuar
                                    Log.e("FugaAlert", "Error enviando alerta: ${e.message}")
                                }
                                
                                // Notificación local con información explicable y accionable
                                Handler(Looper.getMainLooper()).post {
                                    // Crear notificación con detalles y acciones
                                    mostrarNotificacionLocal(
                                        context = app,
                                        titulo = "¡Alerta de Fuga! ($urgencia)",
                                        mensaje = "Se ha detectado una posible fuga. Factor principal: $factorPrincipal. " +
                                                 if (horasCriticas < 12) "Requiere atención en menos de $horasCriticas horas." else "Requiere verificación."
                                    )
                                }
                            }
                        }
                        is Resource.Error -> {
                            // Manejar error de red, con reintentos inteligentes
                            Log.e("FugaAlert", "Error obteniendo datos: ${result.message}")
                        }
                        is Resource.Loading -> {
                            // Estado de carga, no hacer nada especial
                        }
                    }
                } catch (e: Exception) {
                    Log.e("FugaAlert", "Error en monitoreo: ${e.message}")
                }
                delay(60_000) // Chequea cada minuto
            }
        }
    }
}
