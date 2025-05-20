package com.tuempresa.fugas

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tuempresa.fugas.ui.mostrarNotificacionLocal

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            mostrarNotificacionLocal(
                context = applicationContext,
                titulo = it.title ?: "Alerta de Fuga",
                mensaje = it.body ?: "Se ha detectado una fuga en el sistema."
            )
        }
    }
}
