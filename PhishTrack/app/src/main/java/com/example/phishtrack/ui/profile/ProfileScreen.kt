package com.example.phishtrack.ui.profile

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phishtrack.data.api.UserProfile
import com.example.phishtrack.data.repository.AuthRepository
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.ui.auth.UiState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    authRepository: AuthRepository,
    casesRepository: CasesRepository,
    onLogoutClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var profileState by remember { mutableStateOf<UiState<UserProfile>>(UiState.Loading) }

    var biometricEnabled by remember { mutableStateOf(authRepository.isBiometricEnabled()) }
    var pinLockEnabled by remember { mutableStateOf(authRepository.isPinLockEnabled()) }
    var showPinDialog by remember { mutableStateOf(false) }
    var newPin by remember { mutableStateOf("") }
    
    val cases by casesRepository.cachedCasesFlow.collectAsState(initial = emptyList())

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false; newPin = "" },
            title = { Text("Set App PIN", color = Color.White) },
            text = {
                Column {
                    Text("Enter a 4-digit PIN to secure the app:", color = Color(0x88, 0x92, 0xB0), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0x00, 0xF5, 0xFF),
                            unfocusedBorderColor = Color(0x2A, 0x35, 0x58),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            authRepository.setPin(newPin)
                            Toast.makeText(context, "PIN set successfully", Toast.LENGTH_SHORT).show()
                            showPinDialog = false
                            newPin = ""
                        } else {
                            Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x00, 0xF5, 0xFF))
                ) { Text("Save", color = Color(0x0A, 0x0E, 0x1A)) }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; newPin = "" }) {
                    Text("Cancel", color = Color(0x88, 0x92, 0xB0))
                }
            },
            containerColor = Color(0x14, 0x18, 0x29)
        )
    }

    LaunchedEffect(Unit) {
        authRepository.getProfile().collect { result ->
            result.fold(
                onSuccess = { profileState = UiState.Success(it) },
                onFailure = { profileState = UiState.Error(it.message ?: "Failed to load profile") }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF020617))
                ))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Premium Analyst ID Card
        when (profileState) {
            is UiState.Success -> {
                val profile = (profileState as UiState.Success).data
                ProfileCard(profile)
            }
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0x14, 0x18, 0x29), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
                }
            }
            else -> {
                ProfileCard(UserProfile("N/A", "analyst@phishtrack.org", "Forensic Analyst", "SOC Operations", "analyst"))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Case Statistics Summary
        Text(
            text = "CASE STATISTICS",
            color = Color(0x88, 0x92, 0xB0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val totalCases = cases.size
            val closedCases = cases.count { it.status == "Closed" || it.status == "False_Positive" }
            val highRiskCases = cases.count { it.priority == "Critical" || it.priority == "High" }
            val highRiskRate = if (totalCases > 0) (highRiskCases * 100) / totalCases else 0

            StatCard(title = "Total Cases", value = totalCases.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "Closed", value = closedCases.toString(), modifier = Modifier.weight(1f))
            StatCard(title = "High Risk", value = "$highRiskRate%", valueColor = if (highRiskRate > 50) Color(0xFF, 0x3B, 0x3B) else Color(0x00, 0xF5, 0xFF), modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Settings Sections
        Text(
            text = "SECURITY SETTINGS",
            color = Color(0x88, 0x92, 0xB0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                // Biometrics toggle
                ListItem(
                    headlineContent = { Text("Biometric Authentication", color = Color.White, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Use fingerprint/face to unlock app", color = Color(0x88, 0x92, 0xB0)) },
                    leadingContent = { Icon(Icons.Default.Fingerprint, contentDescription = "Biometrics", tint = Color(0x00, 0xF5, 0xFF)) },
                    trailingContent = {
                        Switch(
                            checked = biometricEnabled,
                            onCheckedChange = { 
                                biometricEnabled = it 
                                authRepository.setBiometricEnabled(it)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Biometric auth ${if(it) "enabled" else "disabled"}")
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0x00, 0xF5, 0xFF))
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                Divider(color = Color(0x2A, 0x35, 0x58).copy(alpha = 0.5f), thickness = 1.dp)

                // PIN Lock toggle
                ListItem(
                    headlineContent = { Text("App PIN Lock", color = Color.White, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Require 4-digit PIN on launch", color = Color(0x88, 0x92, 0xB0)) },
                    leadingContent = { Icon(Icons.Default.Security, contentDescription = "PIN Lock", tint = Color(0x00, 0xF5, 0xFF)) },
                    trailingContent = {
                        Switch(
                            checked = pinLockEnabled,
                            onCheckedChange = { 
                                pinLockEnabled = it 
                                authRepository.setPinLockEnabled(it)
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("PIN Lock ${if(it) "enabled" else "disabled"}")
                                }
                                if (it) {
                                    showPinDialog = true
                                }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color(0x00, 0xF5, 0xFF))
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Data & Management
        Text(
            text = "DATA MANAGEMENT",
            color = Color(0x88, 0x92, 0xB0),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                // CSV Export
                ListItem(
                    headlineContent = { Text("Export Cases to CSV", color = Color.White, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("Generate database backup table", color = Color(0x88, 0x92, 0xB0)) },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = "CSV", tint = Color(0x00, 0xF5, 0xFF)) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = "Go", tint = Color(0x88, 0x92, 0xB0)) },
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            try {
                                casesRepository.refreshCases()
                                val cases = casesRepository.cachedCasesFlow.firstOrNull() ?: emptyList()
                                if (cases.isEmpty()) {
                                    snackbarHostState.showSnackbar("No cases to export")
                                } else {
                                    snackbarHostState.showSnackbar("Generating CSV export...")
                                    val csv = StringBuilder()
                                    csv.append("ID,Case Number,Target,Source,Priority,Status,Created At\n")
                                    cases.forEach { c ->
                                        val escapedTarget = c.displayTarget().replace(",", ";").replace("\n", " ")
                                        val escapedTitle = (c.title ?: "").replace(",", ";").replace("\n", " ")
                                        csv.append("${c.id},${c.caseNumber},\"$escapedTarget\",${c.source},${c.priority},${c.status},${c.createdAt}\n")
                                    }
                                    val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", java.util.Locale.getDefault()).format(java.util.Date())
                                    val file = File(context.cacheDir, "PhishTrack_Cases_$timestamp.csv")
                                    file.writeText(csv.toString())
                                    val uri = FileProvider.getUriForFile(
                                        context, "${context.packageName}.fileprovider", file
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/csv"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    snackbarHostState.showSnackbar("CSV ready: ${cases.size} cases exported")
                                    context.startActivity(Intent.createChooser(shareIntent, "Export CSV"))
                                }
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar("Export failed: ${e.message}")
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Logout Button
        Button(
            onClick = {
                authRepository.logout()
                onLogoutClick()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.15f)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(1.dp, Color(0xFF, 0x3B, 0x3B), RoundedCornerShape(8.dp))
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFF, 0x3B, 0x3B))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "LOGOUT ACCOUNT", color = Color(0xFF, 0x3B, 0x3B), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
    
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        }
    }

@Composable
fun StatCard(title: String, value: String, valueColor: Color = Color.White, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)),
        modifier = modifier.border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(10.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, color = Color(0x88, 0x92, 0xB0), fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, color = valueColor, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}
@Composable
fun ProfileCard(profile: UserProfile) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0x14, 0x18, 0x29), Color(0x1E, 0x24, 0x40))
                )
            )
            .border(2.dp, Color(0x00, 0xF5, 0xFF), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PHISHTRACK SOC CARD",
                    color = Color(0x00, 0xF5, 0xFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield",
                    tint = Color(0x00, 0xF5, 0xFF).copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }

            Column {
                Text(
                    text = profile.name ?: "Unknown Analyst",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.email,
                    color = Color(0x88, 0x92, 0xB0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Organization: ${profile.organization ?: "SOC Operations"}",
                    color = Color(0x88, 0x92, 0xB0),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ROLE: ${profile.role?.uppercase() ?: "ANALYST"}",
                    color = Color(0x00, 0xFF, 0x88),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "SECURE LOGGED",
                    color = Color(0x88, 0x92, 0xB0).copy(alpha = 0.4f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
