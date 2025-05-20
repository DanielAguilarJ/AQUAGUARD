package com.tuempresa.fugas.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.ui.theme.FugasColors
import com.tuempresa.fugas.ui.LoginScreen
import com.tuempresa.fugas.ui.OnboardingVinculacion
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.compose.runtime.saveable.rememberSaveable
import com.tuempresa.fugas.datastore.OnboardingDataStore
import kotlinx.coroutines.launch

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("home") }
    val isLoggedIn = remember { mutableStateOf(FirebaseAuth.getInstance().currentUser != null) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var loginLoading by remember { mutableStateOf(false) }
    val onboardingDataStore: OnboardingDataStore = org.koin.androidx.compose.get()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val isDeviceLinkedFlow = onboardingDataStore.isDeviceLinked.collectAsState(initial = false)
    var showOnboarding by remember { mutableStateOf(!isDeviceLinkedFlow.value) }
    val auth = FirebaseAuth.getInstance()
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        loginLoading = false
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnCompleteListener { t ->
                if (t.isSuccessful) {
                    isLoggedIn.value = true
                } else {
                    loginError = t.exception?.localizedMessage ?: "Error autenticando con Google"
                }
            }
        } catch (e: Exception) {
            loginError = e.localizedMessage ?: "Error autenticando con Google"
        }
    }
    if (!isLoggedIn.value) {
        LoginScreen(
            onLoginSuccess = { isLoggedIn.value = true },
            onGoogleLogin = { intent ->
                loginLoading = true
                loginError = null
                googleLauncher.launch(intent)
            },
            isLoading = loginLoading,
            error = loginError
        )
        return
    }
    if (showOnboarding) {
        OnboardingVinculacion(onFinish = {
            scope.launch {
                onboardingDataStore.setDeviceLinked(true)
                showOnboarding = false
            }
        })
        return
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .shadow(8.dp)
                    .height(64.dp),
                containerColor = FugasColors.SurfaceDark,
                contentColor = FugasColors.TextPrimary
            ) {
                NavigationItem(
                    selected = currentScreen == "home",
                    icon = if (currentScreen == "home") Icons.Filled.Assessment else Icons.Outlined.Home,
                    label = "Monitoreo",
                    onClick = { currentScreen = "home" }
                )
                
                NavigationItem(
                    selected = currentScreen == "config",
                    icon = if (currentScreen == "config") Icons.Filled.Notifications else Icons.Outlined.Settings,
                    label = "Configuración",
                    onClick = { currentScreen = "config" }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(FugasColors.DarkBackground)
                .padding(bottom = padding.calculateBottomPadding())
        ) {
            when (currentScreen) {
                "home" -> HomeScreen()
                "config" -> ConfiguracionScreen()
            }
        }
    }
}

@Composable
private fun NavigationItem(
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = FugasColors.PrimaryGreen,
            selectedTextColor = FugasColors.PrimaryGreen,
            unselectedIconColor = FugasColors.TextSecondary,
            unselectedTextColor = FugasColors.TextSecondary,
            indicatorColor = FugasColors.DarkBackground
        )
    )
}
