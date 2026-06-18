package com.example.phishtrack.ui.components

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorStateComponent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
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
                text = "Oops! Something went wrong",
                color = Color(0xFF, 0x55, 0x55),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color(0xFF, 0x55, 0x55))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry", color = Color(0xFF, 0x55, 0x55))
            }
        }
    }
}
