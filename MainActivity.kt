package com.tuempresa.fugas

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.tuempresa.fugas.FugasApp
import com.tuempresa.fugas.ui.AppNavigation
import com.tuempresa.fugas.ui.theme.FugasAppTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.tuempresa.fugas.domain.IAInferenceManager
import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.viewmodel.SensorViewModel
import org.koin.androidx.compose.getViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.ui.components.GradientButton
import androidx.compose.animation.animateContentSize
import com.tuempresa.fugas.ui.theme.FugasColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateListOf
import com.tuempresa.fugas.model.AlertData
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Leer extras de notificación
        val openAlert = intent.getBooleanExtra("openAlert", false)
        val alertMessage = intent.getStringExtra("alertMessage") ?: ""
        setContent {
            var showDialog by remember { mutableStateOf(openAlert) }
            var showHistory by remember { mutableStateOf(false) }
            val viewModel: SensorViewModel = getViewModel()
            val datos = viewModel.sensorData
            val lastData = datos.lastOrNull()
            val iaProb = lastData?.let { IAInferenceManager.predict(it) } ?: 0f
            // Usar historial real del backend
            val alertHistory by viewModel.alertHistory.collectAsState()
            // Cuando se detecta una alerta, podrías actualizar el historial si lo deseas
            // Al abrir historial, refrescar desde backend
            if (showHistory) {
                LaunchedEffect(Unit) { viewModel.fetchAlertHistory() }
            }
            // Botón flotante para historial
            Box(Modifier.fillMaxSize()) {
                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(FugasColors.AlertRed, CircleShape)
                                        .padding(end = 8.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Detalle de Alerta")
                            }
                        },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(alertMessage, style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(12.dp))
                                if (lastData != null) {
                                    val (iaProbExplain, importancias) = IAInferenceManager.predictWithExplain(lastData)
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateContentSize(),
                                        colors = CardDefaults.cardColors(containerColor = FugasColors.CardBackground),
                                        shape = RoundedCornerShape(12.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    "Probabilidad IA: ",
                                                    color = FugasColors.TextPrimary,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    "${(iaProbExplain * 100).toInt()}%",
                                                    color = when {
                                                        iaProbExplain > 0.7f -> FugasColors.AlertRed
                                                        iaProbExplain > 0.4f -> FugasColors.ChartOrange
                                                        else -> FugasColors.PrimaryGreen
                                                    },
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(12.dp))
                                            
                                            // Visualización de explicabilidad
                                            Text(
                                                "Importancia de cada sensor:",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = FugasColors.TextSecondary
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            importancias.forEach { (feature, value) ->
                                                val color = when (feature) {
                                                    "Flujo" -> FugasColors.ChartBlue
                                                    "Presión" -> FugasColors.ChartPurple
                                                    "Vibración" -> FugasColors.ChartOrange
                                                    else -> FugasColors.PrimaryGreen
                                                }
                                                
                                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Text(
                                                            feature,
                                                            color = color,
                                                            style = MaterialTheme.typography.bodyMedium
                                                        )
                                                        Text(
                                                            "${(value * 100).toInt()}%",
                                                            color = color,
                                                            style = MaterialTheme.typography.labelMedium
                                                        )
                                                    }
                                                    
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    
                                                    LinearProgressIndicator(
                                                        progress = value,
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .height(8.dp)
                                                            .clip(RoundedCornerShape(4.dp)),
                                                        color = color,
                                                        trackColor = color.copy(alpha = 0.2f)
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(4.dp))
                                            }
                                            
                                            Divider(
                                                color = FugasColors.TextSecondary.copy(alpha = 0.2f),
                                                modifier = Modifier.padding(vertical = 12.dp)
                                            )
                                            
                                            // Valores de sensores con iconos
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.WaterDrop,
                                                    contentDescription = "Flujo",
                                                    tint = FugasColors.ChartBlue,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Flujo: ${lastData.flujo} L/min",
                                                    color = FugasColors.ChartBlue,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Speed,
                                                    contentDescription = "Presión",
                                                    tint = FugasColors.ChartPurple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Presión: ${lastData.presion} kPa",
                                                    color = FugasColors.ChartPurple,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Vibration,
                                                    contentDescription = "Vibración",
                                                    tint = FugasColors.ChartOrange,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Vibración: ${lastData.vibracion} Hz",
                                                    color = FugasColors.ChartOrange,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            
                                            Text(
                                                "Timestamp: ${formatDate(lastData.timestamp)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = FugasColors.TextSecondary,
                                                modifier = Modifier.padding(top = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            GradientButton(
                                onClick = { showDialog = false },
                                startColor = FugasColors.PrimaryGreen,
                                endColor = FugasColors.SecondaryGreen
                            ) {
                                Text("Cerrar")
                            }
                        }
                    )
                }
                if (showHistory) {
                    AlertDialog(
                        onDismissRequest = { showHistory = false },
                        title = { 
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = "Historial",
                                    tint = FugasColors.PrimaryGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Historial de Alertas")
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                if (alertHistory.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Filled.History,
                                                contentDescription = "Sin historial",
                                                tint = FugasColors.TextSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Text(
                                                "No hay alertas registradas",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = FugasColors.TextSecondary
                                            )
                                        }
                                    }
                                } else {
                                    alertHistory.sortedByDescending { it.timestamp }.forEachIndexed { index, alert ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .animateContentSize(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = when(alert.nivel) {
                                                    "Crítico" -> FugasColors.AlertRed.copy(alpha = 0.1f)
                                                    "Alerta" -> FugasColors.ChartOrange.copy(alpha = 0.1f)
                                                    else -> FugasColors.CardBackground
                                                }
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(8.dp)
                                                                .background(
                                                                    when(alert.nivel) {
                                                                        "Crítico" -> FugasColors.AlertRed
                                                                        "Alerta" -> FugasColors.ChartOrange
                                                                        else -> FugasColors.PrimaryGreen
                                                                    },
                                                                    CircleShape
                                                                )
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            "Nivel: ${alert.nivel}",
                                                            color = when(alert.nivel) {
                                                                "Crítico" -> FugasColors.AlertRed
                                                                "Alerta" -> FugasColors.ChartOrange
                                                                else -> FugasColors.PrimaryGreen
                                                            },
                                                            style = MaterialTheme.typography.titleMedium.copy(
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        )
                                                    }
                                                    
                                                    Text(
                                                        formatDate(alert.timestamp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = FugasColors.TextSecondary
                                                    )
                                                }
                                                
                                                Spacer(modifier = Modifier.height(8.dp))
                                                
                                                Text(
                                                    alert.mensaje,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = FugasColors.TextPrimary
                                                )
                                                
                                                // Explicabilidad IA si hay datos de sensor para este timestamp
                                                val datos = viewModel.sensorData.find { it.timestamp == alert.timestamp }
                                                if (datos != null) {
                                                    val (_, importancias) = IAInferenceManager.predictWithExplain(datos)
                                                    
                                                    Divider(
                                                        color = FugasColors.TextSecondary.copy(alpha = 0.2f),
                                                        modifier = Modifier.padding(vertical = 8.dp)
                                                    )
                                                    
                                                    Text(
                                                        "Análisis IA:",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = FugasColors.TextSecondary
                                                    )
                                                    
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    
                                                    importancias.forEach { (feature, value) ->
                                                        val color = when (feature) {
                                                            "Flujo" -> FugasColors.ChartBlue
                                                            "Presión" -> FugasColors.ChartPurple
                                                            "Vibración" -> FugasColors.ChartOrange
                                                            else -> FugasColors.PrimaryGreen
                                                        }
                                                        
                                                        Row(
                                                            modifier = Modifier.padding(vertical = 2.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = when(feature) {
                                                                    "Flujo" -> Icons.Filled.WaterDrop
                                                                    "Presión" -> Icons.Filled.Speed
                                                                    "Vibración" -> Icons.Filled.Vibration
                                                                    else -> Icons.Filled.WaterDrop
                                                                },
                                                                contentDescription = feature,
                                                                tint = color,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            
                                                            Text(
                                                                feature,
                                                                modifier = Modifier.width(60.dp),
                                                                color = color,
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                            
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            
                                                            LinearProgressIndicator(
                                                                progress = value,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .height(6.dp)
                                                                    .clip(RoundedCornerShape(3.dp)),
                                                                color = color,
                                                                trackColor = color.copy(alpha = 0.2f)
                                                            )
                                                            
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            
                                                            Text(
                                                                "${(value * 100).toInt()}%",
                                                                color = color,
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        
                                        if (index < alertHistory.size - 1) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            GradientButton(
                                onClick = { showHistory = false },
                                startColor = FugasColors.PrimaryGreen, 
                                endColor = FugasColors.SecondaryGreen
                            ) {
                                Text("Cerrar")
                            }
                        }
                    )
                }
                // Botón flotante para abrir historial
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .background(
                                Color(0xFF1A1A1A).copy(alpha = 0.7f),
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { showHistory = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.Filled.History,
                                contentDescription = "Historial de alertas",
                                tint = FugasColors.PrimaryGreen,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = datos.isNotEmpty(),
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            Row {
                                Divider(
                                    modifier = Modifier
                                        .height(24.dp)
                                        .width(1.dp)
                                        .padding(horizontal = 8.dp),
                                    color = FugasColors.TextSecondary.copy(alpha = 0.3f)
                                )
                                
                                IconButton(
                                    onClick = {
                                        viewModel.fetchSensorData()
                                        // Mostrar Snackbar o mensaje temporal "Datos actualizados"
                                    },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Refresh,
                                        contentDescription = "Actualizar datos",
                                        tint = FugasColors.ChartBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            FugasAppTheme {
                AppNavigation()
            }
        }
    }
}

fun formatDate(timestamp: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = parser.parse(timestamp)
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        formatter.format(date ?: Date())
    } catch (e: Exception) {
        timestamp
    }
}
