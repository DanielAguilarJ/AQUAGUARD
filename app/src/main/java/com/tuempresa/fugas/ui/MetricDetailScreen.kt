package com.tuempresa.fugas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.ui.components.AIExplainabilityCard
import com.tuempresa.fugas.ui.components.FlujoChartCard
import com.tuempresa.fugas.ui.theme.FugasColors
import com.tuempresa.fugas.model.SensorData
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MetricDetailScreen(
    metric: String,
    history: List<SensorData>,
    onBack: () -> Unit
) {
    var period by remember { mutableStateOf("24h") }
    val filtered = when (period) {
        "24h" -> history.takeLast(24)
        "7d" -> history.takeLast(24 * 7)
        else -> history
    }
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de $metric") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = FugasColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FugasColors.DarkBackground,
                    titleContentColor = FugasColors.TextPrimary
                )
            )
        },
        containerColor = FugasColors.DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Histórico de $metric", style = MaterialTheme.typography.titleMedium, color = FugasColors.TextPrimary)
                Spacer(Modifier.weight(1f))
                FilterChip(selected = period == "24h", onClick = { period = "24h" }, label = { Text("24h") })
                Spacer(Modifier.width(4.dp))
                FilterChip(selected = period == "7d", onClick = { period = "7d" }, label = { Text("7d") })
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Gráfico histórico real
            if (filtered.isNotEmpty()) {
                FlujoChartCard(filtered) // Puedes adaptar para presión/vibración
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Tabla de valores y explicabilidad IA para cada punto
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered) { data ->
                    val valor = when (metric) {
                        "Flujo" -> data.flujo
                        "Presión" -> data.presion
                        "Vibración" -> data.vibracion
                        else -> 0f
                    }
                    val (prob, factores) = com.tuempresa.fugas.domain.IAInferenceManager.predictWithExplain(data)
                    val explicacion = com.tuempresa.fugas.domain.IAInferenceManager.explainPrediction(prob, factores)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = FugasColors.SurfaceDark)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(dateFormat.format(Date(data.timestamp)), color = FugasColors.TextSecondary, modifier = Modifier.width(60.dp))
                            Text(String.format("%.2f", valor), color = FugasColors.TextPrimary, modifier = Modifier.width(60.dp))
                            Spacer(Modifier.weight(1f))
                            if (prob > 0.5f) {
                                Icon(Icons.Default.Report, contentDescription = null, tint = FugasColors.AlertRed)
                            }
                        }
                        if (prob > 0.3f) {
                            AIExplainabilityCard(
                                featureImportance = factores,
                                probability = prob,
                                explanation = explicacion
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Resumen de tendencias
            val max = filtered.maxOfOrNull {
                when (metric) {
                    "Flujo" -> it.flujo
                    "Presión" -> it.presion
                    "Vibración" -> it.vibracion
                    else -> 0f
                }
            } ?: 0f
            val min = filtered.minOfOrNull {
                when (metric) {
                    "Flujo" -> it.flujo
                    "Presión" -> it.presion
                    "Vibración" -> it.vibracion
                    else -> 0f
                }
            } ?: 0f
            Text("Máximo: $max   Mínimo: $min", color = FugasColors.TextSecondary)
        }
    }
}
