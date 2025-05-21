package com.tuempresa.fugas

import android.app.Application
import com.tuempresa.fugas.di.appModule
import com.tuempresa.fugas.domain.FugaDetector
import com.tuempresa.fugas.domain.IAInferenceManager
import com.tuempresa.fugas.model.SensorData
import com.tuempresa.fugas.ui.mostrarNotificacionLocal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import androidx.compose.runtime.remember
import com.tuempresa.fugas.model.AlertData
import com.tuempresa.fugas.ui.AlertHistoryScreen
import com.tuempresa.fugas.ui.AlertDetailDialog
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.tuempresa.fugas.data.AppDatabase
import com.tuempresa.fugas.data.AlertStatusDao

class FugasApp : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var alertStatusDao: AlertStatusDao
        private set

    /**
     * Sobrecarga el método init para usar los nuevos sistemas avanzados
     */
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@FugasApp)
            modules(appModule)
        }
        
        // Inicializar IA avanzada con todos los modelos
        IAInferenceManager.init(this)
        // Inicializar gestor de modelos IA y cargar metadata
        com.tuempresa.fugas.domain.ModelUpdater2.init(this)
        
        // Verificar actualizaciones automáticas de modelos
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val updateInfo = com.tuempresa.fugas.domain.ModelUpdater2.checkForUpdates(
                    context = this@FugasApp,
                    serverUrl = com.tuempresa.fugas.domain.ModelUpdater2.DEFAULT_SERVER_URL
                )
                
                if (updateInfo.updateAvailable && updateInfo.isRequired) {
                    // Descargar automáticamente actualizaciones críticas
                    val success = ModelUpdater2.downloadModel(
                        context = this@FugasApp,
                        url = updateInfo.updateUrl
                    )
                    
                    if (success) {
                        // Recargar modelo
                        IAInferenceManager.reloadModel(this@FugasApp)
                    }
                }
            } catch (e: Exception) {
                // Fallar silenciosamente, se usará modelo local
            }
        }
        
        // Observador global para alertas de fuga
        FugaAlertManager.init(this)

        // Inicializar base de datos Room
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "fugas-db"
        ).build()
        alertStatusDao = database.alertStatusDao()
    }
}

@Composable
fun FugasApp() {
    val context = LocalContext.current
    // Inyección de datastore para onboarding
    val onboardingStore = get<OnboardingDataStore>()
    val isDeviceLinked by onboardingStore.isDeviceLinked.collectAsState(initial = false)

    // Mostrar pantalla de vinculación si no está vinculado
    if (!isDeviceLinked) {
        OnboardingVinculacion(onFinish = { /* se guarda internamente */ })
        return
    }
    LaunchedEffect(Unit) {
        // Inicializa Crashlytics y Analytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        FirebaseAnalytics.getInstance(context)
    }
    // ...existing code...
    var showAlertHistory by remember { mutableStateOf(false) }
    var selectedAlert by remember { mutableStateOf<AlertData?>(null) }
    val alertHistory = remember { mutableStateListOf<AlertData>() } // Cargar desde almacenamiento real

    // ...existing code...
    if (showAlertHistory) {
        AlertHistoryScreen(alerts = alertHistory, onAlertClick = {
            selectedAlert = it
        })
    }
    if (selectedAlert != null) {
        AlertDetailDialog(alert = selectedAlert!!, onDismiss = { selectedAlert = null })
    }
    // ...existing code...
}
