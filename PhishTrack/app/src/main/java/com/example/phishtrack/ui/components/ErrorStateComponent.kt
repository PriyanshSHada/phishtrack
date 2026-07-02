package com.example.phishtrack.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorStateComponent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Unable to load this section",
    suggestion: String = message.toErrorSuggestion(),
    retryLabel: String = "Retry"
) {
    var visible by remember { mutableStateOf(false) }
    var retryPressed by remember { mutableStateOf(false) }
    val entryScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.96f,
        animationSpec = tween(durationMillis = 260),
        label = "errorStateEntryScale"
    )
    val retryScale by animateFloatAsState(
        targetValue = if (retryPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "errorStateRetryScale"
    )

    LaunchedEffect(Unit) {
        visible = true
    }
    LaunchedEffect(retryPressed) {
        if (retryPressed) {
            kotlinx.coroutines.delay(140)
            retryPressed = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .scale(entryScale)
            .background(Color(0x14, 0x18, 0x29), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error Icon",
                tint = Color(0xFF, 0x55, 0x55),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                color = Color(0xFF, 0x55, 0x55),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center
            )
            if (suggestion.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.24f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = suggestion,
                        color = Color(0xFF, 0xA3, 0xA3),
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    retryPressed = true
                    onRetry()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, Color(0xFF, 0x55, 0x55).copy(alpha = 0.65f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.scale(retryScale)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color(0xFF, 0x55, 0x55))
                Spacer(modifier = Modifier.width(8.dp))
                Text(retryLabel, color = Color(0xFF, 0x55, 0x55), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun String.toErrorSuggestion(): String {
    val lowerMessage = lowercase()
    return when {
        "network" in lowerMessage || "timeout" in lowerMessage ->
            "Check the connection and try again. If the issue persists, refresh from a stable network."
        "unauthorized" in lowerMessage || "token" in lowerMessage || "session" in lowerMessage ->
            "Your session may have expired. Retry first, then sign in again if this keeps happening."
        "server" in lowerMessage || "500" in lowerMessage ->
            "The service is having trouble responding. Retry in a moment."
        else ->
            "Retry the request. If it fails again, capture the message above for investigation."
    }
}
