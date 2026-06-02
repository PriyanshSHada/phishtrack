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

@Composable
fun OtpVerifyScreen(
    email: String,
    viewModel: AuthViewModel,
    onVerificationSuccess: (token: String, userId: String) -> Unit,
    onBackToLogin: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf(60) }
    
    val context = LocalContext.current
    val otpVerifyState by viewModel.otpVerifyState
    val resendOtpState by viewModel.resendOtpState

    // Count-down timer
    LaunchedEffect(timeLeft) {
        if (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
    }

    LaunchedEffect(otpVerifyState) {
        when (otpVerifyState) {
            is UiState.Success -> {
                val data = (otpVerifyState as UiState.Success).data
                Toast.makeText(context, "Verification successful!", Toast.LENGTH_SHORT).show()
                viewModel.resetStates()
                onVerificationSuccess(data.token, data.user.id)
            }
            is UiState.Error -> {
                Toast.makeText(context, (otpVerifyState as UiState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetStates()
            }
            else -> {}
        }
    }

    LaunchedEffect(resendOtpState) {
        when (resendOtpState) {
            is UiState.Success -> {
                Toast.makeText(context, "OTP resent to your email", Toast.LENGTH_SHORT).show()
                viewModel.resetStates()
            }
            is UiState.Error -> {
                Toast.makeText(context, (resendOtpState as UiState.Error).message, Toast.LENGTH_LONG).show()
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

            // Timer & Resend Option
            if (timeLeft > 0) {
                Text(
                    text = "Resend OTP in 0:${timeLeft.toString().padStart(2, '0')}",
                    color = Color(0x88, 0x92, 0xB0),
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "Resend OTP Code",
                    color = Color(0x00, 0xF5, 0xFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable {
                        timeLeft = 60
                        viewModel.resendOtp(email)
                    }
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Verify Button
            if (otpVerifyState is UiState.Loading) {
                CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
            } else {
                Button(
                    onClick = {
                        if (otpCode.length < 6) {
                            Toast.makeText(context, "Please enter all 6 digits", Toast.LENGTH_SHORT).show()
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
                text = "Back to Login",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { onBackToLogin() }
            )
        }
    }
}
