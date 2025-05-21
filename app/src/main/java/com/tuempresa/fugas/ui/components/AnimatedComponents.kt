package com.tuempresa.fugas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.ui.theme.FugasColors
import kotlinx.coroutines.delay

@Composable
fun PulsatingIndicator(
    isActive: Boolean = true,
    color: Color = FugasColors.AlertRed,
    size: Float = 12f
) {
    val animatedSize = remember { androidx.compose.animation.core.Animatable(size) }
    val animatedAlpha = remember { androidx.compose.animation.core.Animatable(0.7f) }
    
    LaunchedEffect(isActive) {
        if (isActive) {
            while (true) {
                animatedSize.animateTo(
                    targetValue = size * 1.3f,
                    animationSpec = androidx.compose.animation.core.tween(400)
                )
                animatedAlpha.animateTo(
                    targetValue = 0.4f,
                    animationSpec = androidx.compose.animation.core.tween(400)
                )
                animatedSize.animateTo(
                    targetValue = size,
                    animationSpec = androidx.compose.animation.core.tween(400)
                )
                animatedAlpha.animateTo(
                    targetValue = 0.7f,
                    animationSpec = androidx.compose.animation.core.tween(400)
                )
                delay(800)
            }
        } else {
            animatedSize.snapTo(size)
            animatedAlpha.snapTo(0.7f)
        }
    }
    
    Box(
        modifier = Modifier
            .size(animatedSize.value.dp)
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = animatedAlpha.value))
    )
}

@Composable
fun AnimatedAlertCard(
    title: String,
    message: String,
    color: Color = FugasColors.AlertRed,
    onActionClick: () -> Unit = {}
) {
    var cardVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        cardVisible = true
    }
    
    val slideInAnimation = remember {
        androidx.compose.animation.core.Animatable(if (cardVisible) 0f else -100f)
    }
    
    LaunchedEffect(cardVisible) {
        slideInAnimation.animateTo(
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 500,
                easing = androidx.compose.animation.core.EaseOutCubic
            )
        )
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .offset(y = slideInAnimation.value.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulsatingIndicator(color = color)
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = color
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            
            IconButton(onClick = onActionClick) {
                androidx.compose.material.icons.Icons.Filled.ChevronRight.let {
                    Icon(
                        imageVector = it,
                        contentDescription = "Ver más",
                        tint = color
                    )
                }
            }
        }
    }
}

@Composable
fun GradientButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = Color.White,
    startColor: Color = FugasColors.PrimaryGreen,
    endColor: Color = FugasColors.SecondaryGreen,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = contentColor.copy(alpha = 0.3f)
        ),
        contentPadding = PaddingValues(),
    ) {
        androidx.compose.foundation.background(
            brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                colors = listOf(startColor, endColor),
                startX = 0f,
                endX = 300f
            ),
            shape = RoundedCornerShape(8.dp)
        ).let {
            Box(
                modifier = Modifier
                    .then(it)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

@Composable
fun FadeInContent(
    visible: Boolean = true,
    initialAlpha: Float = 0f,
    animationDuration: Int = 500,
    content: @Composable () -> Unit
) {
    val alpha = remember { androidx.compose.animation.core.Animatable(initialAlpha) }
    
    LaunchedEffect(visible) {
        alpha.animateTo(
            targetValue = if (visible) 1f else 0f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = animationDuration
            )
        )
    }
    
    androidx.compose.animation.AnimatedVisibility(
        visible = alpha.value > 0f,
        enter = androidx.compose.animation.fadeIn(
            animationSpec = androidx.compose.animation.core.tween(animationDuration)
        ),
        exit = androidx.compose.animation.fadeOut(
            animationSpec = androidx.compose.animation.core.tween(animationDuration)
        )
    ) {
        Box(modifier = Modifier.alpha(alpha.value)) {
            content()
        }
    }
}

@Composable
fun ShimmerLoading(
    modifier: Modifier = Modifier,
    color: Color = FugasColors.CardBackground,
    highlightColor: Color = FugasColors.SurfaceDark
) {
    val shimmerColors = listOf(
        color,
        highlightColor,
        color
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnimation = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer animation"
    )
    
    val brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
        colors = shimmerColors,
        startX = translateAnimation.value - 1000f,
        endX = translateAnimation.value
    )
    
    Box(
        modifier = modifier
            .then(
                androidx.compose.foundation.background(
                    brush = brush,
                    shape = RoundedCornerShape(8.dp)
                )
            )
    )
}

@Composable
fun ShimmerSensorCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = FugasColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ShimmerLoading(
                modifier = Modifier
                    .width(80.dp)
                    .height(16.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                ShimmerLoading(
                    modifier = Modifier
                        .width(60.dp)
                        .height(32.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                ShimmerLoading(
                    modifier = Modifier
                        .width(24.dp)
                        .height(16.dp)
                )
            }
        }
    }
}
