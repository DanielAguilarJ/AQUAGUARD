package com.tuempresa.fugas.ui

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.tuempresa.fugas.MainActivity

fun mostrarNotificacionLocal(context: Context, titulo: String, mensaje: String) {
    val channelId = "fugas_alertas"
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Alertas de Fugas", NotificationManager.IMPORTANCE_HIGH)
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(channelId) == null) {
            manager.createNotificationChannel(channel)
        }
    }
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra("openAlert", true)
        putExtra("alertMessage", mensaje)
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_alert)
        .setContentTitle(titulo)
        .setContentText(mensaje)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
        .setContentIntent(pendingIntent)
        .addAction(
            android.R.drawable.ic_menu_view,
            "Ver detalles",
            pendingIntent // usar PendingIntent para abrir detalles
        )
    with(NotificationManagerCompat.from(context)) {
        notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
