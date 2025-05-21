package com.tuempresa.fugas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.tuempresa.fugas.ui.theme.Blue500
import com.tuempresa.fugas.ui.theme.Red500
import com.tuempresa.fugas.ui.theme.Green500
import com.tuempresa.fugas.ui.theme.FugasAppTheme

/**
 * Componente para visualización de explicabilidad de IA en detección de fugas.
 * Muestra la importancia de cada factor en la detección y predicción.
 */
@Composable
fun AIExplainabilityCard(
    featureImportance: Map<String, Float>,
    probability: Float,
    explanation: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Título y probabilidad
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Análisis de IA",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                val probColor = when {
                    probability > 0.7f -> Red500
                    probability > 0.5f -> MaterialTheme.colorScheme.error
                    probability > 0.3f -> Color(0xFFFFA000) // Amber
                    else -> Green500
                }
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = probColor.copy(alpha = 0.2f),
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(
                        text = "${(probability * 100).toInt()}%",
                        color = probColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Visualización de factores importantes
            Text(
                text = "Factores determinantes",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Graficar importancia de factores
            Column {
                featureImportance.entries
                    .sortedByDescending { it.value }
                    .take(4)
                    .forEach { (feature, importance) ->
                        FeatureImportanceBar(
                            featureName = translateFeatureName(feature),
                            importance = importance
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Explicación textual
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun FeatureImportanceBar(
    featureName: String,
    importance: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = featureName,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = "${(importance * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(2.dp))
        
        val barColor = when {
            importance > 0.6f -> Red500
            importance > 0.3f -> Blue500
            else -> Green500
        }
        
        LinearProgressIndicator(
            progress = importance,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = barColor,
            trackColor = barColor.copy(alpha = 0.2f)
        )
    }
}

/**
 * Traduce los nombres técnicos de features a nombres más amigables para el usuario
 */
private fun translateFeatureName(feature: String): String = when(feature) {
    "flujo" -> "Flujo de agua"
    "presion" -> "Presión del sistema"
    "vibracion" -> "Vibración"
    "correlacion_flujo_presion" -> "Relación flujo-presión"
    else -> feature.replaceFirstChar { it.uppercase() }
}

/**
 * Vista previa del componente
 */
@Preview(showBackground = true)
@Composable
fun AIExplainabilityCardPreview() {
    FugasAppTheme {
        AIExplainabilityCard(
            featureImportance = mapOf(
                "flujo" to 0.65f,
                "presion" to 0.25f,
                "vibracion" to 0.1f,
                "correlacion_flujo_presion" to 0.45f
            ),
            probability = 0.75f,
            explanation = "Se detectó un patrón anómalo con flujo alto y presión baja simultáneamente, " +
                    "característico de una fuga. La vibración también está por encima de lo normal para esta hora del día."
        )
    }
}
