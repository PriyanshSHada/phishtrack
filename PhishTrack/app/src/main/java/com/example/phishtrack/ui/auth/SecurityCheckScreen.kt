package com.example.phishtrack.ui.auth

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
        if (savedPin == null) {
            // Force setup, do nothing
        } else if (!isBiometricEnabled && !isPinEnabled) {
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

        if (savedPin == null) {
            // --- ENFORCE PIN SETUP ON FIRST LOGIN ---
            Text(
                text = "Set Up Your Security PIN",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter a 4-digit PIN to protect your account.",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = pinInput,
                onValueChange = { value ->
                    if (value.length <= 4 && value.all { it.isDigit() }) {
                        pinInput = value
                    }
                },
                label = { Text("Create 4-digit PIN", color = Color(0x88, 0x92, 0xB0)) },
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

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (pinInput.length == 4) {
                        authRepository.setPin(pinInput)
                        authRepository.setPinLockEnabled(true)
                        onSuccess()
                    } else {
                        errorMessage = "PIN must be exactly 4 digits"
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x00, 0xF5, 0xFF)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save PIN & Continue", color = Color(0x0A, 0x0E, 0x1A), fontWeight = FontWeight.Bold)
            }
        } else {
            // --- STANDARD UNLOCK ---
            if (isPinEnabled || savedPin != null) {
            OutlinedTextField(
                value = pinInput,
                onValueChange = { value ->
                    if (value.length <= 4) {
                        pinInput = value
                        if (value.length == 4) {
                            if (value == savedPin) {
                                onSuccess()
                            } else {
                                errorMessage = "Incorrect PIN"
                                pinInput = ""
                            }
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
        } // End of else block
    }
}
