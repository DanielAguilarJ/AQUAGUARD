# Dependencias recomendadas para build.gradle (nivel módulo)

```
dependencies {
    implementation "androidx.core:core-ktx:1.12.0"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.1"
    implementation "androidx.activity:activity-compose:1.8.2"
    implementation "androidx.compose.ui:ui:1.5.0"
    implementation "androidx.compose.material:material:1.5.0"
    implementation "androidx.compose.ui:ui-tooling-preview:1.5.0"
    implementation "androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1"
    implementation "com.squareup.retrofit2:retrofit:2.9.0"
    implementation "com.squareup.retrofit2:converter-gson:2.9.0"
    implementation "com.github.PhilJay:MPAndroidChart:v3.1.0"
    implementation "com.google.firebase:firebase-messaging:23.1.2"
    testImplementation "junit:junit:4.13.2"
    testImplementation "io.mockk:mockk:1.13.5"
}
```

# build.gradle (nivel proyecto)

Asegúrate de tener Google y Maven Central en los repositorios:

```
allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

# Configuración Firebase

1. Ve a https://console.firebase.google.com/ y crea un proyecto.
2. Descarga el archivo `google-services.json` y colócalo en `app/`.
3. Agrega al build.gradle (nivel módulo):
   `apply plugin: 'com.google.gms.google-services'`
4. Agrega al build.gradle (nivel proyecto):
   `classpath 'com.google.gms:google-services:4.4.1'` en `dependencies`.

# Notas
- Personaliza los endpoints de Retrofit según tu backend.
- Usa MPAndroidChart para mostrar gráficas de los datos.
- Configura notificaciones push en Firebase Console.
