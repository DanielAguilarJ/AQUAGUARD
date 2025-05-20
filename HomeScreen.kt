package com.tuempresa.fugas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.domain.FugaDetector
import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.ui.components.ShimmerSensorCard
import com.tuempresa.fugas.ui.components.FadeInContent
import com.tuempresa.fugas.ui.components.GradientButton
import com.tuempresa.fugas.ui.components.AnimatedAlertCard
import com.tuempresa.fugas.ui.components.FlujoChartCard
import com.tuempresa.fugas.ui.components.SensorGroup
import com.tuempresa.fugas.ui.components.StatusIndicator
import com.tuempresa.fugas.ui.theme.FugasColors
import com.tuempresa.fugas.viewmodel.SensorViewModel
import org.koin.androidx.compose.getViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Report

@Composable
fun HomeScreen() {
    val viewModel: SensorViewModel = getViewModel()
    val datos = viewModel.sensorData
    val isLoading = viewModel.isLoading
    val error = viewModel.error

    LaunchedEffect(Unit) {
        viewModel.fetchSensorData()
    }

    val tieneAlarma = remember(datos) {
        FugaDetector.detectarFugaEnSerie(datos)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FugasColors.DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 0.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Barra superior y perfil
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Foto de perfil (placeholder)
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = FugasColors.SurfaceDark,
                        modifier = Modifier.size(40.dp)
                    ) {}
                    // Selector de fecha
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Día anterior */ }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, tint = FugasColors.TextPrimary)
                        }
                        Text("HOY", color = FugasColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { /* Día siguiente */ }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = FugasColors.TextPrimary)
                        }
                    }
                    // Indicador de batería (placeholder)
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = FugasColors.PrimaryGreen)
                }
            }
            // Indicadores circulares de métricas principales
            item {
                var metricDetail by remember { mutableStateOf<String?>(null) }
                if (!isLoading && error == null && datos.isNotEmpty()) {
                    val last = datos.last()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CircularMetricIndicator(
                            value = (last.flujo / 10f).coerceIn(0f, 1f),
                            label = "Flujo",
                            valueText = "${last.flujo.toInt()} L/min",
                            color = FugasColors.PrimaryGreen,
                            onClick = { metricDetail = "Flujo" }
                        )
                        CircularMetricIndicator(
                            value = (last.presion / 200f).coerceIn(0f, 1f),
                            label = "Presión",
                            valueText = "${last.presion.toInt()} psi",
                            color = FugasColors.ChartOrange,
                            onClick = { metricDetail = "Presión" }
                        )
                        CircularMetricIndicator(
                            value = (last.vibracion / 2f).coerceIn(0f, 1f),
                            label = "Vibración",
                            valueText = String.format("%.2f", last.vibracion),
                            color = FugasColors.AlertRed,
                            onClick = { metricDetail = "Vibración" }
                        )
                    }
                    if (metricDetail != null) {
                        ModalBottomSheet(onDismissRequest = { metricDetail = null }) {
                            val history = when (metricDetail) {
                                "Flujo" -> datos
                                "Presión" -> datos
                                "Vibración" -> datos
                                else -> emptyList()
                            }
                            com.tuempresa.fugas.ui.MetricDetailScreen(
                                metric = metricDetail!!,
                                history = history,
                                onBack = { metricDetail = null }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(3) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(64.dp),
                                color = FugasColors.PrimaryGreen
                            )
                        }
                    }
                }
            }
            // Tarjetas de estado y alerta
            item {
                if (tieneAlarma) {
                    AnimatedAlertCard(
                        title = "¡Posible fuga detectada!",
                        message = "Se ha detectado una anomalía en los sensores. Revisa los detalles.",
                        onActionClick = { /* manejar acción */ }
                    )
                } else if (!isLoading && error == null && datos.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = FugasColors.SurfaceDark)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Monitor de Salud",
                                style = MaterialTheme.typography.titleMedium,
                                color = FugasColors.PrimaryGreen
                            )
                            Text(
                                text = "Dentro del rango",
                                style = MaterialTheme.typography.bodyMedium,
                                color = FugasColors.TextPrimary
                            )
                        }
                    }
                }
            }
            // Estado del sistema y gráfico
            item {
                if (!isLoading && error == null && datos.isNotEmpty()) {
                    val lastData = datos.last()
                    val statusPercentage = when {
                        lastData.flujo > 6.0f || lastData.presion < 50.0f || lastData.vibracion > 0.8f -> 0.95f
                        lastData.flujo > 5.0f || lastData.presion < 100.0f || lastData.vibracion > 0.5f -> 0.75f
                        lastData.flujo > 4.0f || lastData.presion < 150.0f || lastData.vibracion > 0.3f -> 0.5f
                        else -> 0.25f
                    }
                    val statusColor = when {
                        statusPercentage > 0.8f -> FugasColors.AlertRed
                        statusPercentage > 0.6f -> FugasColors.ChartOrange
                        else -> FugasColors.PrimaryGreen
                    }
                    val statusText = when {
                        statusPercentage > 0.8f -> stringResource(R.string.critical)
                        statusPercentage > 0.6f -> stringResource(R.string.alert)
                        statusPercentage > 0.4f -> stringResource(R.string.attention)
                        else -> stringResource(R.string.normal)
                    }
                    StatusIndicator(
                        percentage = statusPercentage,
                        color = statusColor,
                        title = "Estado del Sistema",
                        subtitle = statusText
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = stringResource(R.string.historico_flujo),
                    style = MaterialTheme.typography.titleMedium,
                    color = FugasColors.TextPrimary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                if (!isLoading && error == null && datos.isNotEmpty()) {
                    FlujoChartCard(datos)
                }
            }
            // Sección de explicabilidad y predicción futura de IA
            item {
                if (!isLoading && error == null && datos.isNotEmpty()) {
                    val last = datos.last()
                    // Explicabilidad IA
                    val (probabilidad, factores) = com.tuempresa.fugas.domain.IAInferenceManager.predictWithExplain(last)
                    val explicacion = com.tuempresa.fugas.domain.IAInferenceManager.explainPrediction(probabilidad, factores)
                    com.tuempresa.fugas.ui.components.AIExplainabilityCard(
                        featureImportance = factores,
                        probability = probabilidad,
                        explanation = explicacion,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                    // Predicción futura IA
                    val (futureAvg, futureList) = com.tuempresa.fugas.domain.IAInferenceManager.predictFutureLeak(24)
                    if (futureList.isNotEmpty()) {
                        com.tuempresa.fugas.ui.components.FutureLeakPredictionChart(
                            predictions = futureList,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                        )
                    }
                }
            }
            // Manejo de carga y error
            item {
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = FugasColors.PrimaryGreen,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                } else if (error != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = FugasColors.CardBackground
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Filled.Error,
                                contentDescription = stringResource(R.string.error_label),
                                tint = FugasColors.AlertRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.error_label),
                                style = MaterialTheme.typography.titleMedium,
                                color = FugasColors.TextPrimary
                            )
                            Text(
                                text = stringResource(R.string.error_connection),
                                style = MaterialTheme.typography.titleMedium,
                                color = FugasColors.TextPrimary
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = FugasColors.TextSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            GradientButton(
                                onClick = { viewModel.fetchSensorData() },
                                startColor = FugasColors.PrimaryGreen,
                                endColor = FugasColors.SecondaryGreen
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Filled.Refresh,
                                    contentDescription = stringResource(R.string.onboarding_link),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.retry))
                            }
                        }
                    }
                } else {
                    FadeInContent {
                        SensorGroup(datos.lastOrNull())
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HomeScreen()
}
