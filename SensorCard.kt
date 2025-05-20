package com.tuempresa.fugas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.ui.theme.CardElevation
import com.tuempresa.fugas.ui.theme.CardShape
import com.tuempresa.fugas.ui.theme.FugasColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SensorCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = FugasColors.TextSecondary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(32.dp).padding(end = 8.dp)
                    )
                }
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineLarge,
                    color = FugasColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyLarge,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(4.dp)
                    .width(40.dp)
                    .background(color, shape = RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun SensorGroup(sensorData: SensorData?) {
    if (sensorData == null) {
        Text("Sin datos disponibles", color = FugasColors.TextSecondary)
        return
    }
    
    val formatter = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
    val formattedDate = try {
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            .parse(sensorData.timestamp)
        formatter.format(date)
    } catch (e: Exception) {
        "Fecha desconocida"
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Última actualización: $formattedDate",
            style = MaterialTheme.typography.labelMedium,
            color = FugasColors.TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SensorCard(
                title = "FLUJO",
                value = String.format("%.1f", sensorData.flujo),
                unit = "L/min",
                color = FugasColors.ChartBlue,
                icon = Icons.Filled.WaterDrop,
                modifier = Modifier.weight(1f)
            )
            SensorCard(
                title = "PRESIÓN",
                value = String.format("%.1f", sensorData.presion),
                unit = "kPa",
                color = FugasColors.ChartPurple,
                icon = Icons.Filled.Speed,
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        SensorCard(
            title = "VIBRACIÓN",
            value = String.format("%.2f", sensorData.vibracion),
            unit = "Hz",
            color = FugasColors.ChartOrange,
            icon = Icons.Filled.Vibration
        )
    }
}
