package com.tuempresa.fugas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.model.SensorData

@Composable
fun EstadoScreen(sensorData: SensorData?) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Última lectura: ${sensorData?.timestamp ?: "Sin datos"}")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Flujo: ${sensorData?.flujo ?: "-"} L/min")
        Text("Presión: ${sensorData?.presion ?: "-"} kPa")
        Text("Vibración: ${sensorData?.vibracion ?: "-"}")
        // Aquí puedes agregar un botón para ver histórico o navegar a otras pantallas
    }
}
