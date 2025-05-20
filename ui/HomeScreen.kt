package com.tuempresa.fugas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.viewmodel.SensorViewModel
import com.tuempresa.fugas.domain.IAInferenceManager
import androidx.compose.material.icons.filled.Brain
import androidx.compose.material3.IconToggleButton

@Composable
fun HomeScreen() {
    val viewModel: SensorViewModel = getViewModel()
    val datos = viewModel.sensorData

    LaunchedEffect(Unit) {
        viewModel.fetchSensorData()
    }

    // Modo de detección: IA vs reglas
    var useAI by remember { mutableStateOf(false) }
    var showExplain by remember { mutableStateOf(false) }
    // Probabilidades y detección
    val probabilidad = remember(datos, useAI) {
        if (useAI) IAInferenceManager.predictSequence(datos) else 0f
    }
    val tieneAlarma = remember(datos, useAI) {
        if (useAI) IAInferenceManager.detectarFugaEnSerieIA(datos)
        else FugaDetector.detectarFugaEnSerie(datos)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(FugasColors.SurfaceDark, FugasColors.ChartBlue.copy(alpha = 0.08f))
                )
            )
            .padding(0.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                // Barra superior visual
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(FugasColors.SurfaceDark)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icono usuario
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Usuario",
                        tint = FugasColors.ChartBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "FugasApp",
                        color = FugasColors.PrimaryGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                    // Indicador batería real
                    val bateria = datos.lastOrNull()?.bateria
                    val (batColor, batText) = when {
                        bateria == null -> FugasColors.TextSecondary to "--"
                        bateria < 5 -> { // voltios
                            when {
                                bateria < 3.5f -> FugasColors.AlertRed to String.format("%.2f V", bateria)
                                bateria < 3.7f -> FugasColors.ChartOrange to String.format("%.2f V", bateria)
                                else -> FugasColors.PrimaryGreen to String.format("%.2f V", bateria)
                            }
                        }
                        else -> { // porcentaje
                            when {
                                bateria < 20 -> FugasColors.AlertRed to "${bateria.toInt()}%"
                                bateria < 50 -> FugasColors.ChartOrange to "${bateria.toInt()}%"
                                else -> FugasColors.PrimaryGreen to "${bateria.toInt()}%"
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryFull,
                            contentDescription = "Batería",
                            tint = batColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Text(
                            text = batText,
                            color = batColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
                // Toggle IA visual con tooltip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("IA avanzada", color = FugasColors.TextPrimary, fontWeight = FontWeight.Medium)
                    IconToggleButton(
                        checked = useAI,
                        onCheckedChange = { useAI = it },
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "Modo IA",
                            tint = if (useAI) FugasColors.ChartBlue else FugasColors.TextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    if (useAI) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "IA activa",
                            tint = FugasColors.ChartBlue,
                            modifier = Modifier.size(18.dp).padding(start = 2.dp)
                        )
                    }
                }
            }

            // Métricas principales visuales
            item {
                if (datos.isNotEmpty()) {
                    val last = datos.last()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricCard(
                            icon = painterResource(id = android.R.drawable.ic_menu_rotate),
                            label = "Flujo",
                            value = "${last.flujo} L/min",
                            color = FugasColors.PrimaryGreen
                        )
                        MetricCard(
                            icon = painterResource(id = android.R.drawable.ic_menu_compass),
                            label = "Presión",
                            value = "${last.presion} psi",
                            color = FugasColors.ChartOrange
                        )
                        MetricCard(
                            icon = painterResource(id = android.R.drawable.ic_menu_recent_history),
                            label = "Vibración",
                            value = String.format("%.2f", last.vibracion),
                            color = FugasColors.AlertRed
                        )
                    }
                }
            }

            // Tarjetas de estado y alerta
            item {
                if (tieneAlarma) {
                    AnimatedAlertCard(
                        title = if (useAI) "¡IA: posible fuga detectada!" else "¡Posible fuga detectada!",
                        message = if (useAI) String.format("Probabilidad %.1f%%", probabilidad * 100)
                                    else "Se ha detectado una anomalía en los sensores. Revisa los detalles.",
                        onActionClick = {
                            if (useAI) showExplain = true
                         }
                    )
                    // Dialogo de explicabilidad IA
                    if (showExplain) {
                        if (datos.isNotEmpty()) {
                            val (p, imp) = IAInferenceManager.predictWithExplain(datos.last())
                            val explicacion = IAInferenceManager.explainPrediction(p, imp)
                            AlertDialog(
                                onDismissRequest = { showExplain = false },
                                title = { Text("Explicación IA avanzada") },
                                text = {
                                    Column {
                                        Text(String.format("Probabilidad: %.1f%%", p * 100), style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.height(8.dp))
                                        Text(explicacion, style = MaterialTheme.typography.bodyMedium)
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showExplain = false }) { Text("Cerrar") }
                                }
                            )
                        } else {
                            AlertDialog(
                                onDismissRequest = { showExplain = false },
                                title = { Text("Explicación IA") },
                                text = { Text("No hay datos suficientes para explicación.") },
                                confirmButton = {
                                    TextButton(onClick = { showExplain = false }) { Text("Cerrar") }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Tarjeta visual para métricas
@Composable
fun MetricCard(icon: Painter, label: String, value: String, color: Color) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(90.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Text(label, color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(value, color = FugasColors.TextPrimary, fontSize = 13.sp)
        }
    }
}