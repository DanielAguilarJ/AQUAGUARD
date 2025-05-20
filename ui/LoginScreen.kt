package com.tuempresa.fugas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tuempresa.fugas.ui.theme.FugasColors
import com.tuempresa.fugas.ui.components.GradientButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onGoogleLogin: (Intent) -> Unit,
    isLoading: Boolean = false,
    error: String? = null
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(isLoading) }
    var errorMsg by remember { mutableStateOf(error) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? Activity
    val auth = remember { FirebaseAuth.getInstance() }

    fun doLogin() {
        loading = true
        errorMsg = null
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                loading = false
                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    errorMsg = task.exception?.localizedMessage ?: "Error de autenticación"
                }
            }
    }

    fun doGoogleLogin() {
        if (activity == null) return
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("YOUR_WEB_CLIENT_ID") // Reemplaza por tu clientId
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        val signInIntent = googleSignInClient.signInIntent
        onGoogleLogin(signInIntent)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            modifier = Modifier.padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = FugasColors.CardBackground)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Iniciar sesión", style = MaterialTheme.typography.headlineMedium, color = FugasColors.PrimaryGreen)
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                GradientButton(
                    onClick = { doLogin() },
                    enabled = !loading && email.isNotBlank() && password.isNotBlank()
                ) {
                    if (loading) CircularProgressIndicator(modifier = Modifier.size(18.dp), color = FugasColors.PrimaryGreen, strokeWidth = 2.dp)
                    else Text("Entrar")
                }
                Spacer(modifier = Modifier.height(16.dp))
                GradientButton(
                    onClick = { doGoogleLogin() },
                    startColor = FugasColors.ChartBlue,
                    endColor = FugasColors.PrimaryGreen
                ) {
                    Text("Entrar con Google")
                }
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(errorMsg!!, color = FugasColors.AlertRed)
                }
            }
        }
    }
}
