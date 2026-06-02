package com.example.phishtrack.ui.auth

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.phishtrack.data.repository.AuthRepository

@Composable
fun SecurityCheckScreen(
    authRepository: AuthRepository,
    onSuccess: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val isBiometricEnabled = authRepository.isBiometricEnabled()
    val isPinEnabled = authRepository.isPinLockEnabled()
    val savedPin = authRepository.getPin()

    var pinInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasPromptedBiometric by remember { mutableStateOf(false) }

    // Biometric Logic
    val showBiometricPrompt = {
        val fragmentActivity = context as? FragmentActivity
        if (fragmentActivity != null) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(fragmentActivity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        errorMessage = "Biometric error: $errString"
                    }

                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        errorMessage = "Biometric recognition failed."
                    }
                })

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock PhishTrack")
                .setSubtitle("Confirm your identity to continue")
                .setNegativeButtonText("Use PIN / Cancel")
                .build()

            biometricPrompt.authenticate(promptInfo)
        } else {
            errorMessage = "Biometric authentication is not supported here."
        }
    }

    LaunchedEffect(Unit) {
        if (!isBiometricEnabled && !isPinEnabled) {
            onSuccess() // No security enabled, pass through
        } else if (isBiometricEnabled && !hasPromptedBiometric) {
            hasPromptedBiometric = true
            showBiometricPrompt()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x0A, 0x0E, 0x1A))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Lock",
            tint = Color(0x00, 0xF5, 0xFF),
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "APP LOCKED",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Please authenticate to access your dashboard",
            color = Color(0x88, 0x92, 0xB0),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (isPinEnabled) {
            OutlinedTextField(
                value = pinInput,
                onValueChange = { 
                    if (it.length <= 4) pinInput = it 
                    if (pinInput.length == 4) {
                        if (pinInput == savedPin) {
                            onSuccess()
                        } else {
                            errorMessage = "Incorrect PIN"
                            pinInput = ""
                        }
                    }
                },
                label = { Text("Enter 4-digit PIN", color = Color(0x88, 0x92, 0xB0)) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0x00, 0xF5, 0xFF),
                    unfocusedBorderColor = Color(0x2A, 0x35, 0x58),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isBiometricEnabled) {
            Button(
                onClick = showBiometricPrompt,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x14, 0x18, 0x29)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = "Fingerprint", tint = Color(0x00, 0xF5, 0xFF))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Unlock with Biometrics", color = Color.White)
            }
        }

        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = it, color = Color(0xFF, 0x55, 0x55), fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        TextButton(onClick = onLogout) {
            Text("Logout & Clear Data", color = Color(0xFF, 0x55, 0x55))
        }
    }
}
