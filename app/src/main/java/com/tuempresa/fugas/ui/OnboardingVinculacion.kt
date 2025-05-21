package com.tuempresa.fugas.ui

import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.datastore.OnboardingDataStore
import com.tuempresa.fugas.domain.IAInferenceManager
import com.tuempresa.fugas.model.LinkDeviceRequest
import com.tuempresa.fugas.repository.SensorRepository
import com.tuempresa.fugas.util.Resource
import kotlinx.coroutines.delay
import org.koin.androidx.compose.get

/**
 * OnboardingVinculacion muestra una guía animada y accesible para vincular el dispositivo tras el primer login.
 */
@Composable
fun OnboardingVinculacion(
    onFinish: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    val context = LocalContext.current
    // Inyección de repositorios y datastore
    val repo = get<SensorRepository>()
    val onboardingStore = get<OnboardingDataStore>()
    val steps = listOf(
        stringResource(R.string.onboarding_welcome),
        stringResource(R.string.onboarding_intro),
        stringResource(R.string.onboarding_step1),
        stringResource(R.string.onboarding_step2),
        stringResource(R.string.onboarding_step3)
    )
    var isLinking by remember { mutableStateOf(false) }
    var linked by remember { mutableStateOf(false) }
    var linkError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FugasColors.SurfaceDark),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = FugasColors.CardBackground)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = FugasColors.PrimaryGreen
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = steps[step],
                    style = MaterialTheme.typography.headlineSmall,
                    color = FugasColors.PrimaryGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(32.dp))
                AnimatedVisibility(
                    visible = step == 0,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(tween(300))
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_dialog_info),
                        contentDescription = "Información de bienvenida",
                        tint = FugasColors.PrimaryGreen,
                        modifier = Modifier.size(64.dp)
                    )
                }
                AnimatedVisibility(
                    visible = step == 1,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(tween(300))
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_add),
                        contentDescription = "Agregar dispositivo",
                        tint = FugasColors.ChartBlue,
                        modifier = Modifier.size(64.dp)
                    )
                }
                AnimatedVisibility(
                    visible = step == 2,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(tween(300))
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_menu_upload),
                        contentDescription = "Subir datos",
                        tint = FugasColors.ChartPurple,
                        modifier = Modifier.size(64.dp)
                    )
                }
                AnimatedVisibility(
                    visible = step == 3,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(tween(300))
                ) {
                    Icon(
                        painter = painterResource(android.R.drawable.ic_input_add),
                        contentDescription = "Iniciar vinculación",
                        tint = FugasColors.ChartOrange,
                        modifier = Modifier.size(64.dp)
                    )
                }
                AnimatedVisibility(
                    visible = step == 4 && !linked,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(tween(300))
                ) {
                    Button(
                        onClick = {
                            linkError = null
                            isLinking = true
                        },
                        enabled = !isLinking
                    ) {
                        Text(stringResource(R.string.onboarding_link))
                    }
                }
                AnimatedVisibility(
                    visible = isLinking && !linked,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(tween(300))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = FugasColors.PrimaryGreen)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.onboarding_linking))
                    }
                }
                AnimatedVisibility(
                    visible = linked,
                    enter = fadeIn(tween(600)),
                    exit = fadeOut(tween(300))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(android.R.drawable.checkbox_on_background),
                            contentDescription = "Vinculación exitosa",
                            tint = FugasColors.PrimaryGreen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.onboarding_success), color = FugasColors.PrimaryGreen)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onFinish) { Text(stringResource(R.string.onboarding_continue)) }
                    }
                }
                if (!isLinking && !linked) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Row {
                        if (step > 0) {
                            TextButton(onClick = { step-- }) { Text(stringResource(R.string.onboarding_back)) }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        if (step < steps.lastIndex) {
                            Button(onClick = { step++ }) { Text(stringResource(R.string.onboarding_next)) }
                        }
                    }
                }
            }
        }
    }
    // Efecto de vinculación real
    if (isLinking && !linked) {
        LaunchedEffect(isLinking) {
            // Obtener ID único del dispositivo
            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            // Llamada al backend
            when (val res = repo.linkDevice(LinkDeviceRequest(deviceId))) {
                is Resource.Success -> {
                    // Persistir estado de vinculado
                    onboardingStore.setDeviceLinked(true)
                    linked = true
                    // Mostrar confirmación y recargar IA
                    Toast.makeText(context, "Dispositivo vinculado exitosamente", Toast.LENGTH_SHORT).show()
                    IAInferenceManager.reloadModel(context)
                    // Avanzar al siguiente paso
                    step = steps.lastIndex
                }
                is Resource.Error -> {
                    linkError = res.message ?: "Error desconocido"
                }
            }
            isLinking = false
        }
    }
    // Mostrar error de vinculación si ocurre
    linkError?.let { err ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Text(
                text = err,
                color = FugasColors.AlertRed,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { linkError = null; isLinking = true }) {
                Text(stringResource(R.string.onboarding_retry))
            }
        }
    }
    // Efecto al terminar vinculación y presionar continuar
    LaunchedEffect(linked) {
        if (linked) {
            // Esperar un momento para UI energizarse
            delay(500)
            onFinish()
        }
    }
}
// Fin de OnboardingVinculacion
