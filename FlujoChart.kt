package com.tuempresa.fugas.ui

import android.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.tuempresa.fugas.model.SensorData

@Composable
fun FlujoChart(datos: List<SensorData>) {
    AndroidView(factory = { context ->
        LineChart(context).apply {
            val entries = datos.mapIndexed { idx, d -> Entry(idx.toFloat(), d.flujo) }
            val dataSet = LineDataSet(entries, "Flujo (L/min)").apply {
                color = Color.BLUE
                valueTextColor = Color.BLACK
                setDrawCircles(false)
            }
            data = LineData(dataSet)
            description = Description().apply { text = "Flujo en tiempo" }
            setTouchEnabled(true)
            setPinchZoom(true)
        }
    })
}
