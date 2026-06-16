package com.example.phishtrack.ui.newcase

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewCaseScreen(
    onBackClick: () -> Unit,
    onSubmitCase: (url: String, description: String?, source: String, priority: String, tags: List<String>) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf("Email") }
    var selectedPriority by remember { mutableStateOf("High") }
    var tagsInput by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "NEW PHISHING CASE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    Text(
                        " ⬅ ",
                        fontSize = 20.sp,
                        color = Color(0x00, 0xF5, 0xFF),
                        modifier = Modifier
                            .clickable { onBackClick() }
                            .padding(8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0x0A, 0x0E, 0x1A),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0x0A, 0x0E, 0x1A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(20.dp)
        ) {
            Text(
                text = "INVESTIGATION DETAILS",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // URL input with Paste button
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Target Phishing URL") },
                placeholder = { Text("https://example-scam-site.com") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = "Link", tint = Color(0x88, 0x92, 0xB0)) },
                trailingIcon = {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = clipboard.primaryClip
                        if (clip != null && clip.itemCount > 0) {
                            val text = clip.getItemAt(0).text
                            if (!text.isNullOrEmpty()) {
                                url = text.toString()
                                Toast.makeText(context, "Pasted from clipboard", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = Color(0x00, 0xF5, 0xFF))
                    }
                },
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

            Spacer(modifier = Modifier.height(20.dp))

            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Case Description / Context") },
                placeholder = { Text("Describe suspicious indicators, brand spoofing target, etc.") },
                minLines = 3,
                maxLines = 5,
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

            Spacer(modifier = Modifier.height(24.dp))

            // Source Selector Chips
            Text(
                text = "SOURCE CHANNEL",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val sources = listOf("Email", "WhatsApp", "SMS", "Other")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sources.forEach { source ->
                    val isSelected = selectedSource == source
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) Color(0x00, 0xF5, 0xFF).copy(alpha = 0.15f) else Color(0x14, 0x18, 0x29),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x2A, 0x35, 0x58),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedSource = source }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = source,
                            color = if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x88, 0x92, 0xB0),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Priority Selector Chips
            Text(
                text = "CASE PRIORITY LEVEL",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val priorities = listOf("Low", "Medium", "High", "Critical")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                priorities.forEach { priority ->
                    val isSelected = selectedPriority == priority
                    val accentColor = when (priority) {
                        "Critical" -> Color(0xFF, 0x3B, 0x3B)
                        "High" -> Color(0xFF, 0xA5, 0x00)
                        "Medium" -> Color(0xFF, 0xD7, 0x00)
                        else -> Color(0x4A, 0x9E, 0xFF)
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = if (isSelected) accentColor.copy(alpha = 0.15f) else Color(0x14, 0x18, 0x29),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) accentColor else Color(0x2A, 0x35, 0x58),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedPriority = priority }
                            .padding(horizontal = 8.dp, vertical = 10.dp)
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = priority,
                            color = if (isSelected) accentColor else Color(0x88, 0x92, 0xB0),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tags
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("Tags (Comma Separated)") },
                placeholder = { Text("paypal, login, credential-harvesting") },
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

            Spacer(modifier = Modifier.height(40.dp))

            // Submit Button
            val isValidUrl = url.trim().isNotEmpty() && (url.trim().startsWith("http://") || url.trim().startsWith("https://"))
            
            Button(
                onClick = {
                    if (isValidUrl && !isSubmitting) {
                        isSubmitting = true
                        val tagsList = if (tagsInput.trim().isEmpty()) emptyList() else tagsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onSubmitCase(url.trim(), description.trim().ifEmpty { null }, selectedSource, selectedPriority, tagsList)
                    }
                },
                enabled = isValidUrl && !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x00, 0xF5, 0xFF),
                    disabledContainerColor = Color(0x2A, 0x35, 0x58)
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        color = Color(0x0A, 0x0E, 0x1A),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = "ANALYZE LINK SAFELY",
                        color = Color(0x0A, 0x0E, 0x1A),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}
