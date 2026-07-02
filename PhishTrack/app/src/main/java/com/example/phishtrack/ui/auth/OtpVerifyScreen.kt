package com.example.phishtrack.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OtpVerifyScreen(
    email: String,
    viewModel: AuthViewModel,
    onVerificationSuccess: (token: String, userId: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var timeLeft by remember { mutableIntStateOf(60) }
    var resendCount by remember { mutableIntStateOf(0) }
    var isResendingOtp by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val otpVerifyState by viewModel.otpVerifyState
    val resendOtpState by viewModel.resendOtpState

    // Count-down timer — single coroutine, not one per tick
    // resendCount is used as key so the countdown restarts after each OTP resend
    LaunchedEffect(resendCount) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    LaunchedEffect(otpVerifyState) {
        when (otpVerifyState) {
            is UiState.Success -> {
                val data = (otpVerifyState as UiState.Success).data
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✓ Verification successful!")
                }
                viewModel.resetStates()
                onVerificationSuccess(data.token, data.user?.id ?: "")
            }
            is UiState.Error -> {
                val message = (otpVerifyState as UiState.Error).message
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✗ $message")
                }
                viewModel.resetStates()
            }
            else -> {}
        }
    }

    LaunchedEffect(resendOtpState) {
        when (resendOtpState) {
            is UiState.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✓ OTP resent to your email!")
                }
                isResendingOtp = false
                viewModel.resetStates()
            }
            is UiState.Error -> {
                val message = (resendOtpState as UiState.Error).message
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✗ $message")
                }
                isResendingOtp = false
                viewModel.resetStates()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x0A, 0x0E, 0x1A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "📩",
                fontSize = 54.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Check Your Email",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            val maskedEmail = remember(email) {
                val parts = email.split("@")
                if (parts.size == 2) {
                    val name = parts[0]
                    val domain = parts[1]
                    val maskedName = if (name.length > 3) "${name.take(3)}***" else "$name***"
                    "$maskedName@$domain"
                } else email
            }

            Text(
                text = "We sent a 6-digit verification code to\n$maskedEmail",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Custom 6-digit OTP input boxes overlaying a transparent BasicTextField
            BasicTextField(
                value = otpCode,
                onValueChange = {
                    if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                        otpCode = it
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                decorationBox = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        repeat(6) { index ->
                            val char = when {
                                index >= otpCode.length -> ""
                                else -> otpCode[index].toString()
                            }
                            val isActive = index == otpCode.length

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .background(Color(0x1E, 0x24, 0x40), RoundedCornerShape(8.dp))
                                    .border(
                                        width = 2.dp,
                                        color = if (isActive) Color(0x00, 0xF5, 0xFF) else Color(0x2A, 0x35, 0x58),
                                        shape = RoundedCornerShape(8.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Timer & Resend Option with visual feedback
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x1E, 0x24, 0x40)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    if (timeLeft > 0) {
                        Text(
                            text = "🕐 Resend OTP in 0:${timeLeft.toString().padStart(2, '0')}",
                            color = Color(0x88, 0x92, 0xB0),
                            fontSize = 14.sp
                        )
                    } else {
                        if (isResendingOtp) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sending OTP...",
                                    color = Color(0x00, 0xF5, 0xFF),
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Text(
                                text = "🔄 Resend OTP Code",
                                color = Color(0x00, 0xF5, 0xFF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    isResendingOtp = true
                                    timeLeft = 60
                                    resendCount++
                                    viewModel.resendOtp(email)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Verify Button
            if (otpVerifyState is UiState.Loading) {
                CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
            } else {
                Button(
                    onClick = {
                        if (otpCode.length < 6) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please enter all 6 digits")
                            }
                        } else {
                            viewModel.verifyOtp(email, otpCode)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x00, 0xF5, 0xFF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "VERIFY",
                        color = Color(0x0A, 0x0E, 0x1A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to Login
            Text(
                text = "← Back to Login",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onBackToLogin() }
            )
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
