# Ejemplo de flujo de notificaciones push con Firebase Cloud Messaging (FCM)

1. Configura Firebase en tu proyecto y descarga `google-services.json`.
2. Agrega las dependencias de FCM en tu `build.gradle`.
3. Crea un servicio para recibir mensajes:

```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Maneja la notificación aquí
        remoteMessage.notification?.let {
            // Mostrar notificación local
        }
    }
}
```

4. Declara el servicio en tu `AndroidManifest.xml`:

```xml
<service
    android:name=".MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

5. Envía notificaciones desde tu backend usando la API de FCM.

---

Consulta la documentación oficial de Firebase para detalles avanzados.
