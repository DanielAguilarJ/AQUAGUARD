package com.tuempresa.fugas.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.tuempresa.fugas.ui.theme.Red500
import com.tuempresa.fugas.ui.theme.Green500
import com.tuempresa.fugas.ui.theme.FugasAppTheme
import com.tuempresa.fugas.domain.IAInferenceManager.HourlyPrediction
import java.text.SimpleDateFormat
import java.util.*

/**
 * Componente para visualizar predicciones futuras de fugas.
 * Muestra un gráfico de línea con la probabilidad de fuga para las próximas horas.
 */
@Composable
fun FutureLeakPredictionChart(
    predictions: List<HourlyPrediction>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Título
            Text(
                text = "Predicción de fugas (próximas 24h)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            val maxProbability = predictions.maxOfOrNull { it.probability } ?: 0f
            val criticalHours = predictions.filter { it.probability > 0.7f }.minByOrNull { it.hourOffset }
            
            // Mostrar alerta si hay horas críticas
            if (criticalHours != null) {
                AlertBox(
                    message = "Posible fuga en ${criticalHours.hourOffset} horas",
                    severity = "critical"
                )
                Spacer(modifier = Modifier.height(8.dp))
            } else if (maxProbability > 0.5f) {
                AlertBox(
                    message = "Potencial riesgo futuro detectado",
                    severity = "warning"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Gráfico de línea para predicciones
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(top = 8.dp, bottom = 24.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (predictions.isNotEmpty()) {
                        val width = size.width
                        val height = size.height
                        val horizontalStep = width / (predictions.size - 1).coerceAtLeast(1)
                        
                        // Dibujar líneas de referencia horizontales
                        val dashWidth = 4f
                        val dashGap = 4f
                        
                        // Línea de threshold para alertas (0.7)
                        val thresholdY = height * (1f - 0.7f)
                        var startX = 0f
                        while (startX < width) {
                            drawLine(
                                color = Red500.copy(alpha = 0.3f),
                                start = Offset(startX, thresholdY),
                                end = Offset((startX + dashWidth).coerceAtMost(width), thresholdY),
                                strokeWidth = 1f
                            )
                            startX += dashWidth + dashGap
                        }
                        
                        // Línea de advertencia (0.5)
                        val warningY = height * (1f - 0.5f)
                        startX = 0f
                        while (startX < width) {
                            drawLine(
                                color = Color(0xFFFFA000).copy(alpha = 0.3f), // Amber
                                start = Offset(startX, warningY),
                                end = Offset((startX + dashWidth).coerceAtMost(width), warningY),
                                strokeWidth = 1f
                            )
                            startX += dashWidth + dashGap
                        }
                        
                        // Dibujar línea principal
                        for (i in 0 until predictions.size - 1) {
                            val startX = i * horizontalStep
                            val startY = height * (1f - predictions[i].probability)
                            val endX = (i + 1) * horizontalStep
                            val endY = height * (1f - predictions[i + 1].probability)
                            
                            // Color basado en probabilidad
                            val color = lerp(
                                Green500,
                                Red500,
                                predictions[i].probability
                            )
                            
                            drawLine(
                                color = color,
                                start = Offset(startX, startY),
                                end = Offset(endX, endY),
                                strokeWidth = 3f
                            )
                        }
                        
                        // Dibujar puntos
                        predictions.forEachIndexed { index, prediction ->
                            val x = index * horizontalStep
                            val y = height * (1f - prediction.probability)
                            
                            // Color basado en probabilidad
                            val color = lerp(
                                Green500,
                                Red500,
                                prediction.probability
                            )
                            
                            // Dibujar círculo
                            drawCircle(
                                color = color,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                            
                            // Resaltar puntos críticos
                            if (prediction.probability > 0.7f) {
                                drawCircle(
                                    color = color.copy(alpha = 0.3f),
                                    radius = 8.dp.toPx(),
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = color,
                                    radius = 8.dp.toPx(),
                                    center = Offset(x, y),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                    }
                }
                
                // Eje Y - Etiquetas
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(end = 8.dp)
                        .height(180.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "100%",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "70%",
                        fontSize = 10.sp,
                        color = Red500.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "50%",
                        fontSize = 10.sp,
                        color = Color(0xFFFFA000).copy(alpha = 0.7f)
                    )
                    Text(
                        text = "0%",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                // Eje X - Horas
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(start = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    
                    // Mostrar solo algunas horas para no saturar
                    val steps = if (predictions.size <= 12) 3 else 6
                    predictions.filterIndexed { index, _ -> 
                        index % steps == 0 || index == predictions.size - 1 
                    }.forEach { prediction ->
                        val date = Date(prediction.timestamp)
                        Text(
                            text = timeFormat.format(date),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(40.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Resumen de predicción futura
            val horasAlRiesgo = predictions.count { it.probability > 0.6f }
            Text(
                text = when {
                    horasAlRiesgo > 0 -> "$horasAlRiesgo horas con riesgo significativo en las próximas 24h"
                    maxProbability > 0.4f -> "Riesgo moderado en las próximas horas"
                    else -> "Sin riesgo significativo detectado"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    horasAlRiesgo > 0 -> MaterialTheme.colorScheme.error
                    maxProbability > 0.4f -> Color(0xFFFFA000) // Amber
                    else -> Green500
                }
            )
        }
    }
}

@Composable
fun AlertBox(
    message: String,
    severity: String
) {
    val backgroundColor = when (severity) {
        "critical" -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
        "warning" -> Color(0xFFFFA000).copy(alpha = 0.1f) // Amber
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    }
    
    val textColor = when (severity) {
        "critical" -> MaterialTheme.colorScheme.error
        "warning" -> Color(0xFFFFA000) // Amber
        else -> MaterialTheme.colorScheme.primary
    }
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Interpolación lineal entre dos colores
 */
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    val adjustedFraction = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * adjustedFraction,
        green = start.green + (end.green - start.green) * adjustedFraction,
        blue = start.blue + (end.blue - start.blue) * adjustedFraction,
        alpha = start.alpha + (end.alpha - start.alpha) * adjustedFraction
    )
}

@Preview(showBackground = true)
@Composable
fun FutureLeakPredictionChartPreview() {
    // Datos de prueba para previsualización
    val currentTime = System.currentTimeMillis()
    val hourInMillis = 60 * 60 * 1000L
    
    val predictions = listOf(
        HourlyPrediction(1, currentTime + hourInMillis, 0.2f, 0.95f, emptyMap()),
        HourlyPrediction(2, currentTime + 2 * hourInMillis, 0.3f, 0.9f, emptyMap()),
        HourlyPrediction(3, currentTime + 3 * hourInMillis, 0.4f, 0.85f, emptyMap()),
        HourlyPrediction(4, currentTime + 4 * hourInMillis, 0.5f, 0.8f, emptyMap()),
        HourlyPrediction(5, currentTime + 5 * hourInMillis, 0.6f, 0.75f, emptyMap()),
        HourlyPrediction(6, currentTime + 6 * hourInMillis, 0.7f, 0.7f, emptyMap()),
        HourlyPrediction(7, currentTime + 7 * hourInMillis, 0.8f, 0.65f, emptyMap()),
        HourlyPrediction(8, currentTime + 8 * hourInMillis, 0.7f, 0.6f, emptyMap()),
        HourlyPrediction(9, currentTime + 9 * hourInMillis, 0.6f, 0.55f, emptyMap()),
        HourlyPrediction(10, currentTime + 10 * hourInMillis, 0.5f, 0.5f, emptyMap()),
        HourlyPrediction(11, currentTime + 11 * hourInMillis, 0.4f, 0.45f, emptyMap()),
        HourlyPrediction(12, currentTime + 12 * hourInMillis, 0.3f, 0.4f, emptyMap())
    )
    
    FugasAppTheme {
        FutureLeakPredictionChart(predictions = predictions)
    }
}
