package com.tuempresa.fugas.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.model.AlertData
import com.tuempresa.fugas.ui.components.AIExplainabilityCard
import com.tuempresa.fugas.ui.theme.FugasColors
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import org.koin.androidx.compose.getViewModel
import com.tuempresa.fugas.viewmodel.SensorViewModel
import kotlinx.coroutines.launch

@Composable
fun AlertHistoryScreen(
    alerts: List<AlertData>,
    onAlertClick: (AlertData) -> Unit
) {
    val viewModel: SensorViewModel = getViewModel()
    val alertStatus by viewModel.alertStatus.collectAsState()
    var search by remember { mutableStateOf("") }
    // Filtros avanzados
    var filterStartDate by remember { mutableStateOf<Date?>(null) }
    var filterEndDate by remember { mutableStateOf<Date?>(null) }
    var filterLevels by remember { mutableStateOf(setOf<String>()) }
    var filterRevisadaState by remember { mutableStateOf<Boolean?>(null) }
    var filterKeyword by remember { mutableStateOf("") }
    var showAdvancedFilter by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val context = LocalContext.current
    var exportRequested by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }
    var snackbarMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Parser para timestamps ISO
    val timestampParser = remember { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) }

    // Exportación real
    if (exportRequested) {
        val file = exportAlertsToCSV(allFiltered)
        if (file != null) {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir historial de alertas"))
        }
        exportRequested = false
    }
    if (exportError != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { exportError = null }) { Text("Cerrar") }
            }
        ) { Text(exportError!!) }
    }

    val grouped = alerts
        .filter { it.mensaje.contains(search, ignoreCase = true) }
        .filter { filterKeyword.isBlank() || it.mensaje.contains(filterKeyword, ignoreCase = true) }
        .filter {
            filterLevels.isEmpty() || it.nivel in filterLevels
        }
        .filter { alert ->
            filterRevisadaState == null || (alertStatus[alert.timestamp]?.revisada == filterRevisadaState)
        }
        .filter { alert ->
            filterStartDate == null && filterEndDate == null || run {
                val date = timestampParser.parse(alert.timestamp) ?: return@run true
                val okStart = filterStartDate?.let { !date.before(it) } ?: true
                val okEnd = filterEndDate?.let { !date.after(it) } ?: true
                okStart && okEnd
            }
        }
        .filter { alertStatus[it.timestamp]?.eliminada != true }
        .groupBy { dateFormat.format(timestampParser.parse(it.timestamp) ?: Date()) }
        .toSortedMap(compareByDescending { it })

    val allFiltered = grouped.values.flatten()

    fun exportAlertsToCSV(alerts: List<AlertData>): File? {
        return try {
            val file = File(context.cacheDir, "alertas_export_${System.currentTimeMillis()}.csv")
            FileWriter(file).use { writer ->
                writer.appendLine("timestamp,nivel,mensaje,metadatos")
                alerts.forEach { alert ->
                    val safeMsg = alert.mensaje.replace("\n", " ").replace(",", ";")
                    val safeMeta = alert.metadatos.entries.joinToString(";") { "${it.key}=${it.value}" }
                    writer.appendLine("${alert.timestamp},${alert.nivel},$safeMsg,$safeMeta")
                }
            }
            file
        } catch (e: Exception) {
            exportError = "Error al exportar: ${e.message}"
            null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Alertas") },
                actions = {
                    IconButton(onClick = { exportRequested = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Exportar")
                    }
                    IconButton(onClick = { showAdvancedFilter = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtro avanzado")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = FugasColors.DarkBackground,
                    titleContentColor = FugasColors.TextPrimary
                )
            )
        },
        containerColor = FugasColors.DarkBackground,
        snackbarHost = {
            snackbarMsg?.let { msg ->
                Snackbar(
                    action = {
                        TextButton(onClick = { snackbarMsg = null }) { Text("Cerrar") }
                    }
                ) { Text(msg) }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Buscar alerta") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                singleLine = true
            )
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filtrar por nivel:", color = FugasColors.TextSecondary)
                Spacer(Modifier.width(8.dp))
                listOf("CRÍTICA", "URGENTE", "INMEDIATA", "").forEach { nivel ->
                    val selected = filterLevels.contains(nivel) || (nivel == "" && filterLevels.isEmpty())
                    FilterChip(
                        selected = selected,
                        onClick = { 
                            filterLevels = if (nivel == "") emptySet() else setOf(nivel) 
                        },
                        label = { Text(if (nivel == "") "Todas" else nivel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FugasColors.PrimaryGreen.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                grouped.forEach { (fecha, alertas) ->
                    stickyHeader {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(FugasColors.DarkBackground)
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = fecha,
                                color = FugasColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f)
                            )
                            // Resumen de alertas por día
                            val criticas = alertas.count { it.nivel == "CRÍTICA" || it.nivel == "INMEDIATA" }
                            val urgentes = alertas.count { it.nivel == "URGENTE" }
                            if (criticas > 0) Badge(containerColor = FugasColors.AlertRed) { Text("$criticas") }
                            if (urgentes > 0) Badge(containerColor = FugasColors.ChartOrange) { Text("$urgentes") }
                        }
                    }
                    items(alertas) { alert ->
                        val status = alertStatus[alert.timestamp]
                        val revisada = status?.revisada == true
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAlertClick(alert) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = FugasColors.SurfaceDark)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = when (alert.nivel) {
                                                    "CRÍTICA", "INMEDIATA" -> FugasColors.AlertRed
                                                    "URGENTE" -> FugasColors.ChartOrange
                                                    else -> FugasColors.PrimaryGreen
                                                }
                                            ) { Text(alert.nivel.take(1)) }
                                        }
                                    ) {
                                        Text(
                                            text = alert.nivel,
                                            color = when (alert.nivel) {
                                                "CRÍTICA", "INMEDIATA" -> FugasColors.AlertRed
                                                "URGENTE" -> FugasColors.ChartOrange
                                                else -> FugasColors.PrimaryGreen
                                            },
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(start = 8.dp, end = 8.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = alert.timestamp,
                                        color = FugasColors.TextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.weight(1f))
                                    // Acciones rápidas
                                    IconButton(onClick = {
                                        if (!revisada) {
                                            scope.launch {
                                                viewModel.marcarRevisada(alert.timestamp)
                                                snackbarMsg = "Marcada como revisada"
                                            }
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = if (revisada) "Revisada" else "Marcar como revisada",
                                            tint = if (revisada) FugasColors.PrimaryGreen else FugasColors.TextSecondary
                                        )
                                    }
                                    IconButton(onClick = {
                                        scope.launch {
                                            viewModel.eliminarAlerta(alert.timestamp)
                                            snackbarMsg = "Alerta eliminada"
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Eliminar alerta",
                                            tint = FugasColors.AlertRed
                                        )
                                    }
                                }
                                Text(
                                    text = alert.mensaje,
                                    color = if (revisada) FugasColors.TextSecondary else FugasColors.TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (revisada) {
                                    Text(
                                        text = "Revisada",
                                        color = FugasColors.PrimaryGreen,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de filtro avanzado
    if (showAdvancedFilter) {
        AlertDialog(
            onDismissRequest = { showAdvancedFilter = false },
            title = { Text("Filtro avanzado") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Rango de fechas
                    DateRangePicker(
                        startDate = filterStartDate,
                        endDate = filterEndDate,
                        onStartDateSelected = { filterStartDate = it },
                        onEndDateSelected = { filterEndDate = it }
                    )
                    Spacer(Modifier.height(8.dp))
                    // Niveles
                    Text("Niveles", color = FugasColors.TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Row { listOf("CRÍTICA","URGENTE","INMEDIATA").forEach { nivel ->
                        FilterChip(
                            selected = nivel in filterLevels,
                            onClick = {
                                filterLevels = if (nivel in filterLevels) filterLevels - nivel else filterLevels + nivel
                            },
                            label = { Text(nivel) },
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }}
                    Spacer(Modifier.height(8.dp))
                    // Estado revisada
                    Text("Estado de revisión", color = FugasColors.TextSecondary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = filterRevisadaState == null,
                            onClick = { filterRevisadaState = null }
                        ); Text("Todas")
                        Spacer(Modifier.width(8.dp))
                        RadioButton(
                            selected = filterRevisadaState == false,
                            onClick = { filterRevisadaState = false }
                        ); Text("Pendientes")
                        Spacer(Modifier.width(8.dp))
                        RadioButton(
                            selected = filterRevisadaState == true,
                            onClick = { filterRevisadaState = true }
                        ); Text("Revisadas")
                    }
                    Spacer(Modifier.height(8.dp))
                    // Palabra clave
                    OutlinedTextField(
                        value = filterKeyword,
                        onValueChange = { filterKeyword = it },
                        label = { Text("Palabra clave") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAdvancedFilter = false }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = {
                    // Reiniciar filtros
                    filterStartDate = null; filterEndDate = null
                    filterLevels = emptySet(); filterRevisadaState = null; filterKeyword = ""
                    showAdvancedFilter = false
                }) { Text("Limpiar") }
            }
        )
    }
}

@Composable
fun ExportAlertDialog(alerts: List<AlertData>, onDismiss: () -> Unit) {
    // Simula exportación (puedes implementar compartir CSV/JSON real)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = { Text("Exportar alertas") },
        text = {
            Text("Exporta el historial de alertas como CSV o compártelo por email desde aquí. (Funcionalidad demo)")
        },
        containerColor = FugasColors.SurfaceDark
    )
}

@Composable
fun AlertDetailDialog(alert: AlertData, onDismiss: () -> Unit) {
    val dateTimeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = {
            Column {
                Text("Detalle de Alerta", fontWeight = FontWeight.Bold)
                Text(dateTimeFormat.format(alert.timestamp), color = FugasColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
        },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BadgedBox(badge = {
                        Badge(
                            containerColor = when (alert.nivel) {
                                "CRÍTICA", "INMEDIATA" -> FugasColors.AlertRed
                                "URGENTE" -> FugasColors.ChartOrange
                                else -> FugasColors.PrimaryGreen
                            }
                        ) { Text(alert.nivel.take(1)) }
                    }) {
                        Text(alert.nivel, color = when (alert.nivel) {
                            "CRÍTICA", "INMEDIATA" -> FugasColors.AlertRed
                            "URGENTE" -> FugasColors.ChartOrange
                            else -> FugasColors.PrimaryGreen
                        }, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp, end = 8.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(alert.mensaje, color = FugasColors.TextPrimary)
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Explicabilidad avanzada si existe
                val factores = alert.metadatos["featureImportance"] as? Map<String, Float> ?: emptyMap()
                val prob = (alert.metadatos["confianza"] as? Float) ?: 0f
                val explicacion = alert.metadatos["explicacion"] as? String ?: ""
                if (factores.isNotEmpty()) {
                    AIExplainabilityCard(
                        featureImportance = factores,
                        probability = prob,
                        explanation = explicacion
                    )
                }
                // Otros metadatos clave
                alert.metadatos.forEach { (k, v) ->
                    if (k !in listOf("featureImportance", "confianza", "explicacion")) {
                        Text("$k: $v", color = FugasColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        containerColor = FugasColors.SurfaceDark
    )
}
