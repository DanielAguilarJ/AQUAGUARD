package com.tuempresa.fugas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.getViewModel
import com.tuempresa.fugas.ui.theme.FugasColors
import com.tuempresa.fugas.ui.theme.CardShape
import com.tuempresa.fugas.ui.theme.CardElevation
import com.tuempresa.fugas.viewmodel.SensorViewModel
import com.tuempresa.fugas.domain.IAInferenceManager
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ConfiguracionScreen() {
    val viewModel: SensorViewModel = getViewModel()
    val endpoint by viewModel.endpoint.collectAsState()
    var endpointText by remember { mutableStateOf(endpoint) }
    val scrollState = rememberScrollState()
    var showModelDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FugasColors.DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            // Título
            Text(
                text = stringResource(R.string.config_title),
                style = MaterialTheme.typography.headlineMedium,
                color = FugasColors.TextPrimary,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Tarjeta de endpoint
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = FugasColors.CardBackground
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = CardElevation
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "CONEXIÓN API",
                        style = MaterialTheme.typography.titleMedium,
                        color = FugasColors.PrimaryGreen
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = endpointText,
                        onValueChange = {
                            endpointText = it
                            viewModel.setEndpoint(it)
                        },
                        label = { 
                            Text(
                                stringResource(R.string.endpoint_label),
                                color = FugasColors.TextSecondary
                            ) 
                        },
                        placeholder = { 
                            Text(
                                stringResource(R.string.endpoint_hint),
                                color = FugasColors.TextSecondary.copy(alpha = 0.6f)
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FugasColors.PrimaryGreen,
                            unfocusedBorderColor = FugasColors.TextSecondary.copy(alpha = 0.5f),
                            focusedTextColor = FugasColors.TextPrimary,
                            unfocusedTextColor = FugasColors.TextPrimary,
                            cursorColor = FugasColors.PrimaryGreen
                        )
                    )
                    
                    Text(
                        text = "El endpoint debe incluir la dirección completa de la API, incluyendo el protocolo (https://).",
                        style = MaterialTheme.typography.bodySmall,
                        color = FugasColors.TextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tarjeta de notificaciones
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = FugasColors.CardBackground
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = CardElevation
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "NOTIFICACIONES",
                        style = MaterialTheme.typography.titleMedium,
                        color = FugasColors.ChartOrange
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Alertas de fuga",
                            style = MaterialTheme.typography.bodyLarge,
                            color = FugasColors.TextPrimary
                        )
                        
                        Switch(
                            checked = true,
                            onCheckedChange = { },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FugasColors.PrimaryGreen,
                                checkedTrackColor = FugasColors.DarkGreen,
                                uncheckedThumbColor = FugasColors.TextSecondary,
                                uncheckedTrackColor = FugasColors.TextSecondary.copy(alpha = 0.3f)
                            )
                        )
                    }
                    
                    Divider(
                        color = FugasColors.TextSecondary.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Reportes diarios",
                            style = MaterialTheme.typography.bodyLarge,
                            color = FugasColors.TextPrimary
                        )
                        
                        Switch(
                            checked = false,
                            onCheckedChange = { },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = FugasColors.PrimaryGreen,
                                checkedTrackColor = FugasColors.DarkGreen,
                                uncheckedThumbColor = FugasColors.TextSecondary,
                                uncheckedTrackColor = FugasColors.TextSecondary.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Tarjeta de actualización de modelo IA
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = CardDefaults.cardColors(
                    containerColor = FugasColors.CardBackground
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = CardElevation
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "MODELO IA",
                        style = MaterialTheme.typography.titleMedium,
                        color = FugasColors.ChartPurple
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showModelDialog = true }) {
                        Text("Actualizar modelo IA (OTA)")
                    }
                    if (showModelDialog) {
                        ModelUpdateDialog(
                            context = context,
                            onDismiss = { showModelDialog = false },
                            onSuccess = {
                                showModelDialog = false
                                IAInferenceManager.reloadModel(context)
                            }
                        )
                    }
                }
            }
            
            // Botón de logout
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { showLogoutDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = FugasColors.AlertRed)
            ) {
                Text("Cerrar sesión", color = FugasColors.TextPrimary)
            }
            if (showLogoutDialog) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    title = { Text("¿Cerrar sesión?") },
                    text = { Text("¿Estás seguro de que deseas cerrar tu sesión?") },
                    confirmButton = {
                        TextButton(onClick = {
                            FirebaseAuth.getInstance().signOut()
                            showLogoutDialog = false
                            // Reiniciar sesión: puedes usar un callback, o forzar recomposición global
                            // Aquí usamos un hack: reiniciar la actividad
                            (context as? android.app.Activity)?.recreate()
                        }) { Text("Sí, cerrar sesión") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showLogoutDialog = false }) { Text("Cancelar") }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ConfiguracionScreenPreview() {
    ConfiguracionScreen()
}
