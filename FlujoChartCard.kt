package com.tuempresa.fugas.ui.components

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.ui.theme.CardElevation
import com.tuempresa.fugas.ui.theme.CardShape
import com.tuempresa.fugas.ui.theme.FugasColors
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FlujoChartCard(datos: List<SensorData>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = CardShape,
        colors = CardDefaults.cardColors(
            containerColor = FugasColors.CardBackground
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = CardElevation
        )
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(8.dp),
            factory = { context ->
                LineChart(context).apply {
                    // Configuración visual inspirada en WHOOP
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    description.isEnabled = false
                    legend.isEnabled = false
                    
                    // Sin bordes
                    setDrawBorders(false)
                    
                    // Ajustes de interacción
                    setTouchEnabled(true)
                    setScaleEnabled(true)
                    setPinchZoom(true)
                    
                    // Cuadrícula mínima
                    xAxis.apply {
                        position = XAxis.XAxisPosition.BOTTOM
                        setDrawGridLines(false)
                        textColor = android.graphics.Color.parseColor("#AAAAAA")
                        
                        // Formateador para mostrar horas
                        if (datos.isNotEmpty()) {
                            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                            val labels = datos.map { 
                                try {
                                    formatter.format(dateFormatter.parse(it.timestamp) ?: Date())
                                } catch (e: Exception) {
                                    ""
                                }
                            }
                            valueFormatter = IndexAxisValueFormatter(labels)
                        }
                    }
                    
                    axisLeft.apply {
                        setDrawGridLines(true)
                        gridColor = android.graphics.Color.parseColor("#333333")
                        textColor = android.graphics.Color.parseColor("#AAAAAA")
                    }
                    
                    axisRight.isEnabled = false
                    
                    // Datos del gráfico
                    if (datos.isNotEmpty()) {
                        val entries = datos.mapIndexed { idx, data -> Entry(idx.toFloat(), data.flujo) }
                        val dataSet = LineDataSet(entries, "Flujo").apply {
                            color = android.graphics.Color.parseColor("#29B6F6") // Azul
                            setDrawCircles(false)
                            lineWidth = 2.5f
                            mode = LineDataSet.Mode.CUBIC_BEZIER
                            
                            // Relleno degradado
                            setDrawFilled(true)
                            fillDrawable = android.graphics.drawable.GradientDrawable(
                                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                intArrayOf(
                                    android.graphics.Color.parseColor("#3029B6F6"),
                                    android.graphics.Color.TRANSPARENT
                                )
                            )
                            
                            // Sin valores encima de los puntos
                            setDrawValues(false)
                        }
                        
                        data = LineData(dataSet)
                    }
                    
                    // Animar la carga
                    animateXY(1000, 1000)
                }
            }
        )
    }
}
