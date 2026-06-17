package com.example.phishtrack.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue

fun Modifier.shimmerEffect(): Modifier = composed {
    var size = androidx.compose.ui.geometry.Size.Zero
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")
    val startOffsetX by transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    background(
        brush = Brush.linearGradient(
            colors = listOf(
                Color(0x2A, 0x35, 0x58),
                Color(0x3A, 0x45, 0x68),
                Color(0x2A, 0x35, 0x58)
            ),
            start = Offset(startOffsetX, 0f),
            end = Offset(startOffsetX + 500f, 500f)
        )
    )
}
