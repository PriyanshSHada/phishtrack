package com.example.phishtrack.ui.analysis

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phishtrack.data.api.AnalysisResponse
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.ui.auth.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AnalysisLoadingScreen(
    caseId: String,
    casesRepository: CasesRepository,
    onAnalysisComplete: (caseId: String) -> Unit,
    onBackOnError: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var analysisResult by remember { mutableStateOf<UiState<AnalysisResponse>>(UiState.Loading) }

    // Simulated progress steps
    var step1Done by remember { mutableStateOf(false) }
    var step2Done by remember { mutableStateOf(false) }
    var step3Done by remember { mutableStateOf(false) }
    var step4Done by remember { mutableStateOf(false) }
    var step5Done by remember { mutableStateOf(false) }

    // Calculate progress percentage
    val progressPercentage = remember(step1Done, step2Done, step3Done, step4Done, step5Done) {
        val completedSteps = listOf(step1Done, step2Done, step3Done, step4Done, step5Done).count { it }
        (completedSteps * 20)
    }

    // Start API request to run analysis
    LaunchedEffect(caseId) {
        casesRepository.runAnalysis(caseId).collect { result ->
            result.fold(
                onSuccess = {
                    analysisResult = UiState.Success(it)
                },
                onFailure = {
                    analysisResult = UiState.Error(it.message ?: "Analysis execution failed")
                }
            )
        }
    }

    // Step animation timings
    LaunchedEffect(Unit) {
        delay(1200)
        step1Done = true
        delay(1500)
        step2Done = true
        delay(1500)
        step3Done = true
        delay(1200)
        step4Done = true
    }

    // Handle completed state — navigate as soon as analysis finishes OR step4 completes
    LaunchedEffect(analysisResult, step4Done) {
        if (analysisResult is UiState.Success) {
            if (!step5Done) {
                step5Done = true
                delay(1000)
            }
            onAnalysisComplete(caseId)
        } else if (analysisResult is UiState.Error) {
            val message = (analysisResult as UiState.Error).message
            coroutineScope.launch {
                snackbarHostState.showSnackbar("✗ $message")
            }
            delay(1500)
            onBackOnError()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x0A, 0x0E, 0x1A))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Rotating scanner circle
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer(
                        scaleX = pulseScale,
                        scaleY = pulseScale
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = Color(0x00, 0xF5, 0xFF).copy(alpha = 0.15f),
                        radius = size.minDimension / 2
                    )
                }

                CircularProgressIndicator(
                    modifier = Modifier
                        .size(130.dp)
                        .graphicsLayer(rotationZ = rotation),
                    color = Color(0x00, 0xF5, 0xFF),
                    strokeWidth = 3.dp
                )

                Text(
                    text = "🔎",
                    fontSize = 40.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Progress percentage display
            Text(
                text = "$progressPercentage%",
                color = Color(0x00, 0xF5, 0xFF),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "SANDBOX INVESTIGATION RUNNING",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Isolated browser check & threat engine scan in progress",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Step Progress Checklist
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x14, 0x18, 0x29))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProgressStep(title = "✦ Initialize Puppeteer Browser Sandbox", isDone = step1Done)
                ProgressStep(title = "✦ Fetch Page Content & Screenshot Evidence", isDone = step2Done)
                ProgressStep(title = "✦ Trace Redirect Chain & WHOIS Data", isDone = step3Done)
                ProgressStep(title = "✦ Scan Threat Records (VirusTotal)", isDone = step4Done)
                ProgressStep(title = "✦ Synthesize PhishTrack AI Analysis", isDone = step5Done)
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun ProgressStep(title: String, isDone: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_indicator")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDone) Color(0x00, 0xFF, 0x88).copy(alpha = 0.05f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp)
            )
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDone) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Step Completed",
                tint = Color(0x00, 0xFF, 0x88),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0x00, 0xF5, 0xFF).copy(alpha = pulseAlpha)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0x00, 0xF5, 0xFF),
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = if (isDone) Color.White else Color(0x88, 0x92, 0xB0),
            fontSize = 13.sp,
            fontWeight = if (isDone) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
