package com.tuempresa.fugas.ui.components

import kotlin.math.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Fill
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.ui.theme.CardElevation
import com.tuempresa.fugas.ui.theme.CardShape
import com.tuempresa.fugas.ui.theme.FugasColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun StatusIndicator(
    percentage: Float, // 0.0 a 1.0
    color: Color = FugasColors.PrimaryGreen,
    size: Int = 120,
    title: String = "Estado",
    subtitle: String = "Normal"
) {
    // Animación del porcentaje
    val animatedPercentage = remember { Animatable(0f) }
    
    LaunchedEffect(percentage) {
        animatedPercentage.animateTo(
            targetValue = min(1f, max(0f, percentage)),
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }
    
    // Efectos de color y vibración para alertas críticas
    val pulsatingAnimation = remember { Animatable(1f) }
    LaunchedEffect(key1 = percentage) {
        if (percentage > 0.8f) {
            // Solo pulsar para alertas críticas
            while (true) {
                pulsatingAnimation.animateTo(
                    targetValue = 1.08f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                )
                pulsatingAnimation.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                )
                delay(600)
            }
        } else {
            pulsatingAnimation.snapTo(1f)
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .scale(pulsatingAnimation.value),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = FugasColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (percentage > 0.8f) CardElevation * 1.5f else CardElevation
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = if (percentage > 0.8f) color else FugasColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(FugasColors.DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                // Círculo de progreso
                Canvas(modifier = Modifier.size(size.dp)) {
                    // Background circle
                    drawCircle(
                        color = FugasColors.TextSecondary.copy(alpha = 0.1f),
                        radius = (size / 2) * 0.9f,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                    
                    // Progress arc
                    val sweepAngle = 360f * animatedPercentage.value
                    drawArc(
                        color = color,
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 12f, cap = StrokeCap.Round)
                    )
                    
                    // Puntero para indicar valor exacto
                    val angle = (-90f + sweepAngle) * (PI / 180f).toFloat()
                    val radius = (size / 2) * 0.9f
                    val x = cos(angle) * radius
                    val y = sin(angle) * radius
                    
                    drawCircle(
                        color = color,
                        radius = 6f,
                        center = Offset(center.x + x, center.y + y),
                        style = Fill
                    )
                }
                
                // Texto de porcentaje
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${(animatedPercentage.value * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineMedium,
                        color = FugasColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = color
                    )
                    
                    if (percentage > 0.8f) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Requiere atención inmediata",
                                style = MaterialTheme.typography.labelSmall,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}
