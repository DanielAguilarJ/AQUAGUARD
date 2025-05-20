@Composable
fun CircularMetricIndicator(
    value: Float,
    label: String,
    valueText: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(96.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = value,
            color = color,
            strokeWidth = 8.dp,
            modifier = Modifier.fillMaxSize()
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = valueText,
                color = color,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = FugasColors.TextPrimary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}