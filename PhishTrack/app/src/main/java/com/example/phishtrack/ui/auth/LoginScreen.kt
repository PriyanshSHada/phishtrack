package com.example.phishtrack.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.phishtrack.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToSignUp: () -> Unit,
    onLoginSuccess: (email: String, token: String?, userId: String?) -> Unit,
    onBiometricClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val loginState by viewModel.loginState

    val showBiometricPrompt = {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Biometric authentication cancelled/failed")
                        }
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onBiometricClick()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Biometric recognition failed. Try again.")
                        }
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock PhishTrack")
                .setSubtitle("Confirm fingerprint/face to log in")
                .setNegativeButtonText("Use Password")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("Biometric login is not supported on this device.")
            }
        }
    }

    LaunchedEffect(loginState) {
        when (loginState) {
            is UiState.Success -> {
                val data = (loginState as UiState.Success).data
                viewModel.resetStates()
                // Normal login redirects to OTP verification
                onLoginSuccess(email, null, null)
            }
            is UiState.Error -> {
                val message = (loginState as UiState.Error).message
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
                viewModel.resetStates()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
            ))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // Icon Header
            Text(
                text = "🛡️",
                fontSize = 54.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PhishTrack",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Text(
                text = "Secure Analyst Login",
                color = Color(0x88, 0x92, 0xB0), // #8892B0
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Email Input with error highlighting
            OutlinedTextField(
                value = email,
                onValueChange = { 
                    email = it
                    emailError = ""
                },
                label = { Text("Email Address") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0x88, 0x92, 0xB0)) },
                isError = emailError.isNotEmpty(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (emailError.isEmpty()) Color(0x00, 0xF5, 0xFF) else MaterialTheme.colorScheme.error,
                    unfocusedBorderColor = if (emailError.isEmpty()) Color(0x2A, 0x35, 0x58) else MaterialTheme.colorScheme.error,
                    focusedLabelColor = Color(0x00, 0xF5, 0xFF),
                    unfocusedLabelColor = Color(0x88, 0x92, 0xB0),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (emailError.isNotEmpty()) {
                Text(emailError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password Input with error highlighting
            OutlinedTextField(
                value = password,
                onValueChange = { 
                    password = it
                    passwordError = ""
                },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = Color(0x88, 0x92, 0xB0)) },
                trailingIcon = {
                    val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(icon, contentDescription = "Toggle password visibility", tint = Color(0x88, 0x92, 0xB0))
                    }
                },
                isError = passwordError.isNotEmpty(),
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (passwordError.isEmpty()) Color(0x00, 0xF5, 0xFF) else MaterialTheme.colorScheme.error,
                    unfocusedBorderColor = if (passwordError.isEmpty()) Color(0x2A, 0x35, 0x58) else MaterialTheme.colorScheme.error,
                    focusedLabelColor = Color(0x00, 0xF5, 0xFF),
                    unfocusedLabelColor = Color(0x88, 0x92, 0xB0),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (passwordError.isNotEmpty()) {
                Text(passwordError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Submit Button
            if (loginState is UiState.Loading) {
                CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
            } else {
                Button(
                    onClick = {
                        var isValid = true
                        if (email.trim().isEmpty()) {
                            emailError = "Email is required"
                            isValid = false
                        } else if (!email.contains("@")) {
                            emailError = "Please enter a valid email"
                            isValid = false
                        }
                        if (password.trim().isEmpty()) {
                            passwordError = "Password is required"
                            isValid = false
                        } else if (password.length < 6) {
                            passwordError = "Password must be at least 6 characters"
                            isValid = false
                        }
                        
                        if (isValid) {
                            viewModel.login(email.trim(), password.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x00, 0xF5, 0xFF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "LOGIN",
                        color = Color(0x0A, 0x0E, 0x1A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (viewModel.isBiometricEnabled()) {
                            showBiometricPrompt()
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Biometric login is not enabled in settings.")
                            }
                        }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🔑", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Biometric Login",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Navigation Link
            Text(
                text = "Don't have an account? Sign Up",
                color = Color(0x00, 0xF5, 0xFF),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateToSignUp() }
            )

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
