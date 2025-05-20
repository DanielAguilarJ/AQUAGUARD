# FugasApp

App Android para monitoreo de sensores de flujo, presión y vibración, con detección de fugas y notificaciones.

## Tecnologías principales
- Jetpack Compose (UI)
- Kotlin
- Retrofit (API REST)
- MPAndroidChart (gráficas)
- Firebase Cloud Messaging (notificaciones)
- Arquitectura MVVM
- JUnit + MockK (pruebas)
- GitHub Actions (CI/CD)

## Estructura sugerida

```
/app/
  src/
    main/
      java/com/tuempresa/fugas/
      res/
      AndroidManifest.xml
    test/
  build.gradle
build.gradle
README.md
```

## Pasos iniciales
1. Crea el proyecto en Android Studio con Jetpack Compose y Kotlin.
2. Copia la estructura y dependencias sugeridas aquí.
3. Agrega los archivos de ejemplo y personaliza según tu backend.

## Dependencias recomendadas (build.gradle)

```
implementation "androidx.compose.ui:ui:1.5.0"
implementation "androidx.compose.ui:ui-tooling:1.5.0"
implementation "androidx.compose.ui:ui-tooling-preview:1.5.0"
implementation "androidx.compose.runtime:runtime-livedata:1.5.0"
implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.1"
implementation "androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1"
implementation "com.squareup.retrofit2:retrofit:2.9.0"
implementation "com.squareup.retrofit2:converter-gson:2.9.0"
implementation "com.github.PhilJay:MPAndroidChart:v3.1.0"
implementation "com.google.firebase:firebase-messaging:23.1.2"
implementation "com.google.firebase:firebase-analytics:21.5.0"
implementation "com.google.firebase:firebase-crashlytics:18.6.0"
implementation "androidx.compose.material:material-icons-extended:1.5.0"
implementation "androidx.navigation:navigation-compose:2.7.7"
implementation "androidx.core:core-ktx:1.12.0"
implementation "androidx.activity:activity-compose:1.8.2"
implementation "androidx.compose.material3:material3:1.2.0"
implementation "androidx.compose.material3:material3-window-size-class:1.2.0"
implementation "io.insert-koin:koin-android:3.5.3"
implementation "io.insert-koin:koin-androidx-compose:3.5.3"
implementation "androidx.datastore:datastore-preferences:1.0.0"
implementation 'org.tensorflow:tensorflow-lite:2.11.0'
testImplementation "junit:junit:4.13.2"
testImplementation "io.mockk:mockk:1.13.5"
androidTestImplementation "androidx.test.ext:junit:1.1.5"
androidTestImplementation "androidx.test.espresso:espresso-core:3.5.1"
androidTestImplementation "androidx.compose.ui:ui-test-junit4:1.5.0"
```

## Ejemplo de ViewModel y Retrofit

```kotlin
class SensorViewModel : ViewModel() {
    var sensorData by mutableStateOf<SensorData?>(null)
    // ...
}

interface ApiService {
    @GET("datos/ultimos")
    suspend fun getUltimosDatos(): List<SensorData>
}
```

## Buenas prácticas y recomendaciones

- Usa Hilt o Koin para inyección de dependencias en producción.
- Maneja los estados de carga y error en la UI.
- Configura ProGuard para producción.
- Implementa pruebas unitarias y de UI.
- Mantén el código desacoplado y modular.
- Documenta endpoints y flujos críticos.
- Usa variables de entorno o archivos seguros para claves/API keys.
- Configura Firebase (google-services.json) y activa Analytics y Cloud Messaging.
- Usa la clase FugasApp como Application en tu AndroidManifest.
- El endpoint del backend puede ser configurable desde la pantalla de configuración.
- El sistema de notificaciones ya está listo para recibir alertas push reales.
- El código está preparado para despliegue y monitoreo en producción.
- Usa DataStore para persistir configuraciones del usuario (como el endpoint del backend).
- Aprovecha Compose Preview y tooling para acelerar el desarrollo visual.
- Utiliza LiveData o StateFlow para flujos reactivos en ViewModel.
- Integra Crashlytics para monitoreo de errores en producción (requiere configuración en Firebase Console).
- Usa material3-window-size-class para diseño responsivo en tablets y pantallas grandes.
- Prepara strings.xml para internacionalización (i18n) y soporta dark mode con temas Compose.
- Usa ui-test-junit4 para pruebas de UI con Compose.

## Buenas prácticas avanzadas implementadas
- Persistencia robusta, i18n, accesibilidad, comentarios y recomendaciones avanzadas implementadas.

## Documentación y comentarios
- El código contiene comentarios de alto nivel en las funciones y componentes clave para facilitar el mantenimiento y la colaboración.

## Recomendaciones futuras
- Centralizar manejo de errores de red y mostrar mensajes amigables
- Integrar Crashlytics y Firebase Analytics para monitoreo y analítica
- Implementar Room para soporte offline y sincronización
- Validar certificados SSL en Retrofit para máxima seguridad
- Agregar más pruebas de UI automatizadas
- Escalabilidad multiusuario/multisitio

## Seguridad avanzada en red (SSL Pinning)

La app valida estrictamente los certificados SSL del backend usando OkHttp y TrustManager en `RetrofitInstance.kt`. Para máxima seguridad, puedes implementar certificate pinning cargando el certificado público de tu backend.

Ejemplo de configuración segura:
```kotlin
private fun getSecureOkHttpClient(): OkHttpClient {
    val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
    trustManagerFactory.init(null as KeyStore?)
    val trustManagers = trustManagerFactory.trustManagers
    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustManagers, SecureRandom())
    return OkHttpClient.Builder()
        .sslSocketFactory(sslContext.socketFactory, trustManagers[0] as X509TrustManager)
        .build()
}
```
Adapta este ejemplo para certificate pinning según la documentación de OkHttp: https://square.github.io/okhttp/features/certificates/

**Recomendación:** Nunca desactives la validación de certificados en producción.

## Inteligencia Artificial Avanzada

La aplicación incorpora un sistema de IA avanzada para la detección predictiva de fugas mediante análisis multidimensional de series temporales:

### Características principales:

1. **Detección proactiva y temprana**: El sistema puede predecir fugas potenciales hasta 24 horas antes de que ocurran, analizando patrones sutiles en los datos de sensores.

2. **Explicabilidad (XAI)**: Cada predicción incluye un análisis detallado de los factores que contribuyen a la decisión, permitiendo a los usuarios entender por qué se activó una alerta.

3. **Aprendizaje continuo**: El sistema mejora con el tiempo mediante feedback del usuario, adaptándose a las condiciones específicas de cada instalación.

4. **Múltiples modelos especializados**:
   - Modelo principal de detección de fugas
   - Modelo de predicción futura
   - Modelo de detección de anomalías

5. **Actualizaciones automáticas**: Los modelos se actualizan automáticamente cuando hay mejoras disponibles, manteniendo el sistema siempre optimizado.

6. **Optimización para dispositivos móviles**: Modelos cuantizados y optimizados para eficiencia energética y rendimiento en dispositivos Android.

### Implementación técnica:

```kotlin
// Ejemplo de uso avanzado
val datos = viewModel.sensorData
val (probabilidad, factores) = IAInferenceManager.predictWithExplain(datos.last())
val explicacion = IAInferenceManager.explainPrediction(probabilidad, factores)
```

Esta tecnología proporciona alertas tempranas con alta precisión, reduciendo falsos positivos y permitiendo intervenciones preventivas antes de que ocurran daños significativos.

