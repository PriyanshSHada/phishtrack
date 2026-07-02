package com.example.phishtrack.ui.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var org by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val registerState by viewModel.registerState

    LaunchedEffect(registerState) {
        when (registerState) {
            is UiState.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✓ Registration successful! Redirecting to login...")
                }
                viewModel.resetStates()
                onRegisterSuccess()
            }
            is UiState.Error -> {
                val message = (registerState as UiState.Error).message
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("✗ $message")
                }
                viewModel.resetStates()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x0A, 0x0E, 0x1A))
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo Header
            Text(
                text = "⚡",
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
                text = "Register Analyst Account",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name with error highlighting
            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    nameError = ""
                },
                label = { Text("Full Name") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = Color(0x88, 0x92, 0xB0)) },
                isError = nameError.isNotEmpty(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (nameError.isEmpty()) Color(0x00, 0xF5, 0xFF) else MaterialTheme.colorScheme.error,
                    unfocusedBorderColor = if (nameError.isEmpty()) Color(0x2A, 0x35, 0x58) else MaterialTheme.colorScheme.error,
                    focusedLabelColor = Color(0x00, 0xF5, 0xFF),
                    unfocusedLabelColor = Color(0x88, 0x92, 0xB0),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    errorBorderColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (nameError.isNotEmpty()) {
                Text(nameError, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email with error highlighting
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

            // Organization
            OutlinedTextField(
                value = org,
                onValueChange = { org = it },
                label = { Text("Organization (Optional)") },
                leadingIcon = { Icon(Icons.Default.Business, contentDescription = "Organization", tint = Color(0x88, 0x92, 0xB0)) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0x00, 0xF5, 0xFF),
                    unfocusedBorderColor = Color(0x2A, 0x35, 0x58),
                    focusedLabelColor = Color(0x00, 0xF5, 0xFF),
                    unfocusedLabelColor = Color(0x88, 0x92, 0xB0),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password with error highlighting
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

            // Password strength hint
            Text(
                text = "💡 Minimum 8 characters for security",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp).align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Register Button
            if (registerState is UiState.Loading) {
                CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
            } else {
                Button(
                    onClick = {
                        var isValid = true
                        if (name.trim().isEmpty()) {
                            nameError = "Name is required"
                            isValid = false
                        }
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
                        } else if (password.length < 8) {
                            passwordError = "Password must be at least 8 characters"
                            isValid = false
                        }
                        
                        if (isValid) {
                            viewModel.register(name.trim(), email.trim(), org.trim().ifEmpty { null }, password.trim())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x00, 0xF5, 0xFF)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "REGISTER",
                        color = Color(0x0A, 0x0E, 0x1A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Link to Login
            Text(
                text = "Already have an account? Login",
                color = Color(0x00, 0xF5, 0xFF),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onNavigateToLogin() }
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
