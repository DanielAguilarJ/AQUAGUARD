package com.tuempresa.fugas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tuempresa.fugas.R
import com.tuempresa.fugas.domain.ModelUpdater2
import com.tuempresa.fugas.domain.IAInferenceManager
import com.tuempresa.fugas.ui.theme.FugasColors
import kotlinx.coroutines.launch

/**
 * Diálogo mejorado para gestión de modelos de IA con soporte multimodelo,
 * actualización automática y progreso visual.
 */
@Composable
fun ModelUpdateAdvancedDialog(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    // Inicializar gestor de modelos
    LaunchedEffect(Unit) {
        ModelUpdater2.init(context)
    }
    val scope = rememberCoroutineScope()
    
    // Estados
    var activeTab by remember { mutableStateOf(ModelTab.UPDATE) }
    var updateUrl by remember { mutableStateOf("") }
    var selectedModelType by remember { mutableStateOf(ModelUpdater2.ModelType.MAIN) }
    var isCheckingForUpdates by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<ModelUpdater2.UpdateInfo?>(null) }
    
    // Estado del modelo actual (observar cambios)
    val modelState by remember { mutableStateOf(ModelUpdater2.modelState) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera
                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = "IA Avanzada",
                    tint = FugasColors.PrimaryGreen,
                    modifier = Modifier.size(48.dp)
                )
                
                Text(
                    text = "Sistema IA Avanzado",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                // Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    ModelTab.values().forEach { tab ->
                        ModelTabButton(
                            tab = tab,
                            isSelected = tab == activeTab,
                            onClick = { activeTab = tab },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Contenido de tabs
                when (activeTab) {
                    ModelTab.UPDATE -> {
                        UpdateModelContent(
                            updateUrl = updateUrl,
                            onUpdateUrlChange = { updateUrl = it },
                            selectedModelType = selectedModelType,
                            onModelTypeChange = { selectedModelType = it },
                            modelState = modelState.collectAsState().value,
                            onStartUpdate = {
                                scope.launch {
                                    ModelUpdater2.downloadModel(
                                        context = context,
                                        url = updateUrl,
                                        modelType = selectedModelType
                                    )
                                }
                            },
                            onCheckUpdates = {
                                isCheckingForUpdates = true
                                scope.launch {
                                    updateInfo = ModelUpdater2.checkForUpdates(
                                        context = context,
                                        serverUrl = ModelUpdater2.DEFAULT_SERVER_URL
                                    )
                                    isCheckingForUpdates = false
                                }
                            },
                            isCheckingForUpdates = isCheckingForUpdates,
                            updateInfo = updateInfo
                        )
                    }
                    
                    ModelTab.STATUS -> {
                        ModelStatusContent(
                            modelState = modelState.collectAsState().value,
                            availableModels = remember { 
                                mutableStateOf(
                                    ModelUpdater2.getAvailableModels(context)
                                )
                            }.value
                        )
                    }
                }
                
                // Botones de acción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Cerrar")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    if (activeTab == ModelTab.UPDATE && updateInfo?.updateAvailable == true) {
                        Button(
                            onClick = {
                                scope.launch {
                                    updateInfo?.updateUrl?.takeIf { it.isNotEmpty() }?.let { url ->
                                        val success = ModelUpdater2.downloadModel(
                                            context = context,
                                            url = url,
                                            modelType = selectedModelType
                                        )
                                        if (success) {
                                            IAInferenceManager.reloadModel(context)
                                            onSuccess()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Actualizar ahora")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelTabButton(
    tab: ModelTab,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    val textColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }
    
    Button(
        onClick = onClick,
        modifier = modifier
            .height(40.dp)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = textColor
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Text(
            text = tab.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun UpdateModelContent(
    updateUrl: String,
    onUpdateUrlChange: (String) -> Unit,
    selectedModelType: ModelUpdater2.ModelType,
    onModelTypeChange: (ModelUpdater2.ModelType) -> Unit,
    modelState: ModelUpdater2.ModelState,
    onStartUpdate: () -> Unit,
    onCheckUpdates: () -> Unit,
    isCheckingForUpdates: Boolean,
    updateInfo: ModelUpdater2.UpdateInfo?
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Actualización manual
        Text(
            text = "Actualización de modelo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        OutlinedTextField(
            value = updateUrl,
            onValueChange = onUpdateUrlChange,
            label = { Text("URL del modelo") },
            placeholder = { Text("https://example.com/modelo.tflite") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Selector de tipo de modelo
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tipo de modelo:",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            ModelUpdater2.ModelType.values().forEach { type ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedModelType == type,
                        onClick = { onModelTypeChange(type) }
                    )
                    Text(
                        text = type.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onStartUpdate,
            enabled = updateUrl.isNotBlank() && !modelState.isUpdating,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Actualizar manualmente")
        }
        
        Divider(modifier = Modifier.padding(vertical = 16.dp))
        
        // Actualizaciones automáticas
        Text(
            text = "Actualizaciones disponibles",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Button(
            onClick = onCheckUpdates,
            enabled = !isCheckingForUpdates,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Buscar actualizaciones")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isCheckingForUpdates) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        updateInfo?.let { info ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (info.updateAvailable) {
                        FugasColors.ChartBlue.copy(alpha = 0.1f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (info.updateAvailable) {
                                Icons.Filled.CloudDownload
                            } else {
                                Icons.Filled.CheckCircle
                            },
                            contentDescription = null,
                            tint = if (info.updateAvailable) {
                                FugasColors.ChartBlue
                            } else {
                                FugasColors.PrimaryGreen
                            }
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Text(
                            text = if (info.updateAvailable) {
                                "Nueva versión disponible: ${info.newVersion}"
                            } else {
                                "Tu modelo está actualizado"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    if (info.description.isNotEmpty()) {
                        Text(
                            text = info.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    
                    if (info.isRequired) {
                        Text(
                            text = "¡Actualización crítica requerida!",
                            color = FugasColors.AlertRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        
        // Mostrar progreso de actualización si está en curso
        if (modelState.isUpdating) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = modelState.updateMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = { modelState.updateProgress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Error de actualización
        modelState.updateError?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Error",
                    tint = FugasColors.AlertRed
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = error,
                    color = FugasColors.AlertRed,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ModelStatusContent(
    modelState: ModelUpdater2.ModelState,
    availableModels: Map<ModelUpdater2.ModelType, File?>
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Estado del sistema IA",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Información del modelo principal
        InfoRow(
            label = "Modelo principal",
            value = if (modelState.isModelAvailable) "Instalado" else "No disponible",
            valueColor = if (modelState.isModelAvailable) {
                FugasColors.PrimaryGreen
            } else {
                FugasColors.AlertRed
            }
        )
        
        InfoRow(
            label = "Versión",
            value = modelState.modelVersion
        )
        
        InfoRow(
            label = "Última actualización",
            value = if (modelState.lastUpdated.isNotEmpty()) {
                modelState.lastUpdated
            } else {
                "Nunca"
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Estado de los módulos avanzados
        Text(
            text = "Módulos IA avanzados",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        availableModels.forEach { (type, file) ->
            val isAvailable = file != null
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isAvailable) {
                        Icons.Filled.CheckCircle
                    } else {
                        Icons.Filled.Error
                    },
                    contentDescription = null,
                    tint = if (isAvailable) {
                        FugasColors.PrimaryGreen
                    } else {
                        Color.Gray
                    },
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = when (type) {
                        ModelUpdater2.ModelType.MAIN -> "Detección de fugas"
                        ModelUpdater2.ModelType.FORECAST -> "Predicción futura"
                        ModelUpdater2.ModelType.ANOMALY -> "Detección de anomalías"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = if (isAvailable) "Activo" else "No instalado",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAvailable) {
                        FugasColors.PrimaryGreen
                    } else {
                        Color.Gray
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Características del sistema IA
        Text(
            text = "Características avanzadas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FeatureCard(
                title = "Predicción\nproactiva",
                isEnabled = availableModels[ModelUpdater2.ModelType.FORECAST] != null
            )
            
            FeatureCard(
                title = "Explicabilidad\nXAI",
                isEnabled = true // Siempre disponible
            )
            
            FeatureCard(
                title = "Adaptación\ncontinua",
                isEnabled = true // Siempre disponible
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
private fun FeatureCard(
    title: String,
    isEnabled: Boolean
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(80.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = if (isEnabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
                fontWeight = if (isEnabled) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class ModelTab(val title: String) {
    UPDATE("Actualizar"),
    STATUS("Estado")
}
