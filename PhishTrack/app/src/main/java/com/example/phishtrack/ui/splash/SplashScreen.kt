package com.example.phishtrack.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phishtrack.utils.TokenManager
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    tokenManager: TokenManager,
    onNavigateNext: (isLoggedIn: Boolean) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    LaunchedEffect(Unit) {
        delay(2500)
        val token = tokenManager.getToken()
        onNavigateNext(!token.isNullOrEmpty())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x0A, 0x0E, 0x1A)), // #0A0E1A Deep Navy
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pulse Animated Shield Logo
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer glowing circle
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0x00, 0xF5, 0xFF), // Cyan
                        radius = size.minDimension / 2.3f,
                        style = Stroke(width = 2f),
                        alpha = glowAlpha * 0.3f
                    )
                }

                // Cyber Shield Vector Shape
                Canvas(modifier = Modifier.size(80.dp)) {
                    val path = Path().apply {
                        moveTo(size.width / 2, 4f)
                        lineTo(size.width - 4f, 16f)
                        quadraticTo(size.width - 4f, size.height * 0.6f, size.width / 2, size.height - 4f)
                        quadraticTo(4f, size.height * 0.6f, 4f, 16f)
                        close()
                    }
                    // Draw outer border
                    drawPath(
                        path = path,
                        color = Color(0x00, 0xF5, 0xFF),
                        style = Stroke(width = 6f)
                    )
                    // Draw inner accent lines
                    val innerPath = Path().apply {
                        moveTo(size.width / 2, 16f)
                        lineTo(size.width - 16f, 24f)
                        quadraticTo(size.width - 16f, size.height * 0.55f, size.width / 2, size.height - 16f)
                        quadraticTo(16f, size.height * 0.55f, 16f, 24f)
                        close()
                    }
                    drawPath(
                        path = innerPath,
                        color = Color(0x00, 0xF5, 0xFF).copy(alpha = 0.5f),
                        style = Stroke(width = 3f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = "PhishTrack",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Forensic Link Analyzer",
                color = Color(0x88, 0x92, 0xB0), // #8892B0 Muted Gray-Blue
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp
            )
        }
    }
}
