package com.tuempresa.fugas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.ui.theme.FugasColors
import kotlinx.coroutines.launch

@Composable
fun ModelUpdateDialog(
    context: android.content.Context,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Inicializar gestor de modelos al abrir el diálogo
    LaunchedEffect(Unit) {
        com.tuempresa.fugas.domain.ModelUpdater2.init(context)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Actualizar modelo IA") },
        text = {
            Column {
                Text("Introduce la URL del nuevo modelo .tflite")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL del modelo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
                if (error != null) {
                    Text(error!!, color = FugasColors.AlertRed, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isLoading = true
                    error = null
                    scope.launch {
                        val ok = com.tuempresa.fugas.domain.ModelUpdater2.downloadModel(
                            context = context,
                            url = url,
                            modelType = com.tuempresa.fugas.domain.ModelUpdater2.ModelType.MAIN
                        )
                        isLoading = false
                        if (ok) {
                            onSuccess()
                        } else {
                            error = "Error al descargar el modelo. " + (com.tuempresa.fugas.domain.ModelUpdater2.modelState.value.updateError ?: "")
                        }
                    }
                },
                enabled = url.isNotBlank() && !isLoading
            ) {
                Text("Actualizar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
