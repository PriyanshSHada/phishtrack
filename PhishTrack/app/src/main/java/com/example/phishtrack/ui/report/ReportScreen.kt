package com.example.phishtrack.ui.report

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import com.example.phishtrack.theme.LocalExtendedColors
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.phishtrack.data.api.CaseDetailResponse
import com.example.phishtrack.ui.auth.UiState
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private fun JsonObject?.safeString(key: String): String? =
    try { this?.get(key)?.takeIf { !it.isJsonNull }?.asString } catch (_: Exception) { null }

private fun JsonObject?.safeBoolean(key: String): Boolean? =
    try { this?.get(key)?.takeIf { !it.isJsonNull }?.asBoolean } catch (_: Exception) { null }

private fun JsonObject?.safeInt(key: String): Int? =
    try { this?.get(key)?.takeIf { !it.isJsonNull }?.asInt } catch (_: Exception) { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    caseId: String,
    viewModel: ReportViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val caseDetailState by viewModel.caseDetailState.collectAsStateWithLifecycle()
    val custodyChainState by viewModel.custodyChainState.collectAsStateWithLifecycle()
    val generateReportState by viewModel.generateReportState.collectAsStateWithLifecycle()
    val deleteState by viewModel.deleteState.collectAsStateWithLifecycle()
    val updatingStatusState by viewModel.updatingStatusState.collectAsStateWithLifecycle()
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullscreenImage by remember { mutableStateOf(false) }

    LaunchedEffect(caseId) {
        viewModel.initialize(caseId)
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Handle download intent when download is successful
    LaunchedEffect(downloadState) {
        if (downloadState is UiState.Success) {
            val bytes = (downloadState as UiState.Success<ByteArray>).data
            try {
                val detail = (caseDetailState as? UiState.Success)?.data
                val latestReport = detail?.reports?.firstOrNull()
                if (latestReport != null) {
                    val outputFile = File(context.cacheDir, "report_${latestReport.id}.pdf")
                    withContext(Dispatchers.IO) {
                        outputFile.writeBytes(bytes)
                    }
                    val uri = FileProvider.getUriForFile(
                        context, "${context.packageName}.fileprovider", outputFile
                    )
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(intent)
                }
            } catch (e: android.content.ActivityNotFoundException) {
                snackbarHostState.showSnackbar("No PDF viewer found. Opening Play Store...")
                try {
                    val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pdf+viewer"))
                    context.startActivity(playStoreIntent)
                } catch (playStoreEx: Exception) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/search?q=pdf+viewer"))
                    context.startActivity(browserIntent)
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to open PDF: ${e.message}")
            } finally {
                viewModel.resetDownloadState()
            }
        }
    }

    if (deleteState is UiState.Success) {
        LaunchedEffect(Unit) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "FORENSIC ANALYSIS REPORT",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    Text(
                        " ⬅ ",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onBackClick() }
                            .padding(8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Case",
                            tint = LocalExtendedColors.current.errorLight
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Case", color = Color.White) },
                text = { Text("Permanently delete this case and all its analysis data?", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteCase()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("DELETE", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = MaterialTheme.colorScheme.surface
            )
        }

        when (caseDetailState) {
            is UiState.Success -> {
                val detail = (caseDetailState as UiState.Success).data
                val analysis = detail.analyses?.firstOrNull()
                val latestReport = detail.reports?.firstOrNull()

                // Fullscreen Image Dialog
                if (showFullscreenImage) {
                    Dialog(
                        onDismissRequest = { showFullscreenImage = false },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black)
                        ) {
                            var scale by remember { mutableStateOf(1f) }
                            var offset by remember { mutableStateOf(Offset.Zero) }
                            val screenshot = analysis?.pageScreenshot

                            var imageBytes by remember(screenshot) { mutableStateOf<ByteArray?>(null) }

                            LaunchedEffect(screenshot) {
                                if (screenshot != null && screenshot.startsWith("data:image/png;base64,")) {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val base64Data = screenshot.substringAfter("base64,")
                                            imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                        } catch (e: Exception) {
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Failed to decode screenshot") }
                                        }
                                    }
                                }
                            }

                            imageBytes?.let {
                                AsyncImage(
                                    model = it,
                                    contentDescription = "Fullscreen Screenshot",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            detectTransformGestures { _, pan, zoom, _ ->
                                                scale = (scale * zoom).coerceIn(1f, 5f)
                                                val maxX = (size.width * (scale - 1)) / 2
                                                val maxY = (size.height * (scale - 1)) / 2
                                                offset = Offset(
                                                    x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                                    y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                                )
                                            }
                                        }
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        )
                                )
                            }

                            IconButton(
                                onClick = { showFullscreenImage = false },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    // ── Section 1: Case Identity Card ──
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Case number (subdued) + title (prominent)
                            Text(
                                text = detail.caseNumber ?: "—",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = detail.title ?: "Untitled Case",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 26.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = detail.displayTarget(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 2
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Priority + Source chips
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val priorityColor = when (detail.priority) {
                                    "Critical" -> MaterialTheme.colorScheme.error
                                    "High" -> LocalExtendedColors.current.warning
                                    "Medium" -> LocalExtendedColors.current.mediumPriority
                                    else -> LocalExtendedColors.current.info
                                }
                                Box(
                                    modifier = Modifier
                                        .background(priorityColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                        .border(1.dp, priorityColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(detail.priority ?: "Low", color = priorityColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(detail.source ?: "Unknown", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Status update row
                            Text(
                                text = "UPDATE STATUS",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val statuses = listOf("Open", "Investigating", "Closed", "False_Positive")
                                statuses.forEach { s ->
                                    val isSelected = detail.status == s
                                    val statusColor = when (s) {
                                        "Open" -> MaterialTheme.colorScheme.primary
                                        "Investigating" -> LocalExtendedColors.current.warning
                                        "Closed" -> LocalExtendedColors.current.success
                                        else -> MaterialTheme.colorScheme.error
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isSelected) statusColor.copy(alpha = 0.15f) else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (isSelected) statusColor else MaterialTheme.colorScheme.surfaceVariant,
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { viewModel.updateCaseStatus(s) }
                                            .padding(horizontal = 4.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected && updatingStatusState) {
                                            CircularProgressIndicator(
                                                color = statusColor,
                                                modifier = Modifier.size(10.dp),
                                                strokeWidth = 1.dp
                                            )
                                        } else {
                                            Text(
                                                text = s.replace("_", " "),
                                                color = if (isSelected) statusColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Section 2: Threat Score Card ──
                    val score = analysis?.threatScore ?: 0
                    val ringColor = when {
                        score >= 70 -> MaterialTheme.colorScheme.error
                        score >= 40 -> LocalExtendedColors.current.warning
                        else -> LocalExtendedColors.current.success
                    }
                    val severityLabel = analysis?.severity ?: if (analysis == null) "Pending" else "Low"
                    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, ringColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(130.dp)) {
                                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                    drawArc(
                                        color = surfaceVariantColor,
                                        startAngle = 0f, sweepAngle = 360f,
                                        useCenter = false, style = Stroke(width = 28f)
                                    )
                                    drawArc(
                                        color = ringColor,
                                        startAngle = -90f,
                                        sweepAngle = (score / 100f) * 360f,
                                        useCenter = false, style = Stroke(width = 28f)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "$score",
                                        color = Color.White,
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "/ 100",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(24.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "THREAT ASSESSMENT",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                // Severity chip
                                Box(
                                    modifier = Modifier
                                        .background(ringColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, ringColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = severityLabel.uppercase(),
                                        color = ringColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Verdict chip
                                val verdict = analysis?.verdict ?: "Pending Analysis"
                                val verdictColor = when {
                                    verdict.contains("Confirmed") || verdict.contains("Malware") || verdict.contains("Credential") -> MaterialTheme.colorScheme.error
                                    verdict.contains("Likely") || verdict.contains("Suspicious") -> LocalExtendedColors.current.warning
                                    verdict == "Benign" -> LocalExtendedColors.current.success
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "⚖  $verdict",
                                        color = verdictColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // Confidence indicator
                                val confidence = analysis?.confidence ?: 50
                                Text(
                                    text = "AI Confidence: $confidence%",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val vtCount = analysis?.virustotalResult.safeInt("maliciousCount") ?: 0
                                Text(
                                    text = "🛡  $vtCount engine${if (vtCount != 1) "s" else ""} flagged",
                                    color = if (vtCount > 0) MaterialTheme.colorScheme.error else LocalExtendedColors.current.success,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val sslValid = analysis?.sslInfo.safeBoolean("valid")
                                Text(
                                    text = if (sslValid == true) "🔒  SSL certificate valid" else "⚠  SSL issue detected",
                                    color = if (sslValid == true) LocalExtendedColors.current.success else LocalExtendedColors.current.warning,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Section 2b: Brand Impersonation Alert ──
                    val brandImpersonated = analysis?.brandImpersonated
                    if (!brandImpersonated.isNullOrEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎭", fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "BRAND IMPERSONATION DETECTED",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = brandImpersonated,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Users may be deceived into entering credentials",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Section 3: Phishing Techniques ──
                    val techniques = analysis?.aiTechniques ?: emptyList()
                    if (techniques.isNotEmpty()) {
                        Text(
                            text = "PHISHING TECHNIQUES DETECTED",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            techniques.forEach { tech ->
                                Box(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(text = tech, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Section 3b: AI Threat Indicators ──
                    val indicators = analysis?.aiIndicators ?: emptyList()
                    if (indicators.isNotEmpty()) {
                        Text(
                            text = "THREAT INDICATORS",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            indicators.forEach { indicator ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.07f))
                                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "!",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        modifier = Modifier.width(16.dp)
                                    )
                                    Text(
                                        text = indicator,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Section 3c: MITRE ATT&CK Mapping ──
                    val mitreTechniques = analysis?.mitreTechniques
                    if (mitreTechniques != null && mitreTechniques.size() > 0) {
                        Text(
                            text = "MITRE ATT&CK MAPPING",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, Color(0xFF7C3AED).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            // Table header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF7C3AED).copy(alpha = 0.15f))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text("ID", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(70.dp))
                                Text("Technique", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.weight(1f))
                                Text("Tactic", color = Color(0xFF7C3AED), fontWeight = FontWeight.Bold, fontSize = 10.sp, modifier = Modifier.width(90.dp))
                            }
                            mitreTechniques.forEachIndexed { index, element ->
                                var obj: com.google.gson.JsonObject? = null
                                var tid = "?"
                                var name = "Unknown"
                                var tactic = "?"
                                try {
                                    obj = element.asJsonObject
                                    tid = obj.get("id")?.asString ?: "?"
                                    name = obj.get("name")?.asString ?: "Unknown"
                                    tactic = obj.get("tactic")?.asString ?: "?"
                                } catch (e: Exception) {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Failed to parse AI tags") }
                                }

                                if (obj != null) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(if (index % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.background.copy(alpha = 0.3f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(tid, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(70.dp))
                                        Text(name, color = Color.White, fontSize = 11.sp, modifier = Modifier.weight(1f), lineHeight = 16.sp)
                                        Text(tactic, color = Color(0xFFAB8BFF), fontSize = 10.sp, modifier = Modifier.width(90.dp))
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Section 4: AI Summary ──
                    val aiSummary = analysis?.aiSummary
                    if (!aiSummary.isNullOrEmpty()) {
                        Text(
                            text = "AI FORENSIC EVALUATION",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                            Text(
                                text = aiSummary,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    // ── Section 5: Forensic Evidence (Collapsible) ──
                    Text(
                        text = "FORENSIC EVIDENCE",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    CollapsibleSection(title = "🌐  WHOIS Domain Lookup") {
                        val whois = analysis?.whoisData
                        if (whois != null) {
                            InfoRow("Registrar", whois.safeString("registrar") ?: "Unavailable")
                            InfoRow("Country", whois.safeString("country") ?: "Unavailable")
                            InfoRow("Domain Age", whois.safeInt("ageDays")?.let { "$it days" } ?: "Unavailable")
                            InfoRow("Created", whois.safeString("creationDate")?.take(10) ?: "Unavailable")
                            InfoRow("Expires", whois.safeString("expiryDate")?.take(10) ?: "Unavailable")
                            val suspicious = whois.safeBoolean("isSuspiciousAge")
                            if (suspicious == true) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("⚠️  Domain registered < 30 days ago — high risk indicator", color = MaterialTheme.colorScheme.error, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
                            Text("No WHOIS data available.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CollapsibleSection(title = "🔒  Network & SSL") {
                        val geo = analysis?.ipGeolocation
                        val ssl = analysis?.sslInfo
                        InfoRow("Resolved IP", geo.safeString("ip") ?: "Unavailable")
                        InfoRow("Location", "${geo.safeString("city") ?: "?"}, ${geo.safeString("country") ?: "?"}")
                        InfoRow("ISP", geo.safeString("isp") ?: "Unknown")
                        Spacer(modifier = Modifier.height(6.dp))
                        val sslIsValid = ssl.safeBoolean("valid")
                        InfoRow("SSL Status", if (sslIsValid == true) "✅  Valid" else "❌  Invalid / Missing")
                        if (sslIsValid == true) {
                            InfoRow("Issuer", ssl.safeString("issuer") ?: "Unknown")
                            InfoRow("Valid Until", ssl.safeString("validTo")?.take(10) ?: "Unknown")
                        } else {
                            val sslError = ssl.safeString("error")
                            if (!sslError.isNullOrEmpty()) InfoRow("Error", sslError)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CollapsibleSection(title = "🛡️  VirusTotal Scan") {
                        val vt = analysis?.virustotalResult
                        val malCount = vt.safeInt("maliciousCount") ?: 0
                        val totalEngines = vt.safeInt("totalEngines") ?: 0
                        InfoRow("Malicious Detections", "$malCount engine${if (malCount != 1) "s" else ""}")
                        if (totalEngines > 0) InfoRow("Engines Scanned", totalEngines.toString())
                        val permalink = vt.safeString("permalink")
                        if (!permalink.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "View full report →",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(permalink))
                                    context.startActivity(intent)
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CollapsibleSection(title = "🔗  Redirect Chain") {
                        val chain = analysis?.redirectChain ?: emptyList()
                        if (chain.isEmpty()) {
                            Text("No redirects observed.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        } else {
                            chain.forEachIndexed { index, url ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "${index + 1}.",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Text(
                                        text = url,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 16.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CollapsibleSection(title = "📸  Evidence Screenshot") {
                        val screenshot = analysis?.pageScreenshot
                        if (screenshot != null && screenshot.startsWith("data:image/png;base64,")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFullscreenImage = true }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                var imageBytes by remember(screenshot) { mutableStateOf<ByteArray?>(null) }
                                var isDecoding by remember(screenshot) { mutableStateOf(true) }
                                var decodeError by remember(screenshot) { mutableStateOf(false) }

                                LaunchedEffect(screenshot) {
                                    isDecoding = true
                                    try {
                                        withContext(Dispatchers.IO) {
                                            val base64Data = screenshot.substringAfter("base64,")
                                            imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                                        }
                                    } catch (e: Exception) {
                                        decodeError = true
                                    } finally {
                                        isDecoding = false
                                    }
                                }

                                if (isDecoding) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                } else if (decodeError || imageBytes == null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                        Text("Screenshot unavailable", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        SubcomposeAsyncImage(
                                            model = imageBytes,
                                            contentDescription = "Evidence Screenshot",
                                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)),
                                            loading = {
                                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                            },
                                            error = {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                                    Text("Failed to load image", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                                                }
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text("Tap to expand", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Icon(Icons.Default.ImageNotSupported, contentDescription = "Empty", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No screenshot captured.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    CollapsibleSection(title = "🔐  Chain of Custody") {
                        if (custodyChainState.isEmpty()) {
                            Text(
                                text = "No custody chain records found.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            custodyChainState.forEachIndexed { index, event ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(24.dp)
                                    ) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                        if (index < custodyChainState.size - 1) {
                                            Box(modifier = Modifier.width(2.dp).height(100.dp).background(MaterialTheme.colorScheme.surfaceVariant))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = event.action.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = event.timestamp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.background)
                                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(6.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text(text = "SHA-256 BEFORE:", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            Text(text = event.hashBefore ?: "N/A (INITIAL RECORD)", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 4.dp))
                                            Text(text = "SHA-256 AFTER:", color = LocalExtendedColors.current.success, fontWeight = FontWeight.SemiBold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            Text(text = event.hashAfter ?: "N/A", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // ── Section 6: Action Buttons ──
                    val isGenerating = generateReportState is UiState.Loading
                    val generateFailed = generateReportState is UiState.Error
                    val isDownloading = downloadState is UiState.Loading

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.generateReport() },
                            enabled = !isGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.background,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = if (generateFailed) "RETRY" else "COMPILE PDF",
                                    color = MaterialTheme.colorScheme.background,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (latestReport != null) {
                                    viewModel.downloadReport(latestReport.id)
                                } else {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Compile the report first") }
                                }
                            },
                            enabled = latestReport != null && !isDownloading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (latestReport != null) LocalExtendedColors.current.success.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .border(
                                    1.dp,
                                    if (latestReport != null) LocalExtendedColors.current.success else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(color = LocalExtendedColors.current.success, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Text(
                                    text = "OPEN PDF",
                                    color = if (latestReport != null) LocalExtendedColors.current.success else Color(0x55, 0x55, 0x55),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (latestReport == null) {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Compile the report first") }
                                } else {
                                    viewModel.verifyReport(latestReport.id)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        ) {
                            Text(text = "VERIFY", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            else -> {
                val errorMessage = if (caseDetailState is UiState.Error) (caseDetailState as UiState.Error).message else "Unknown error"
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Failed to load report details.", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color.Red,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadData() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RETRY", color = MaterialTheme.colorScheme.background, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.6f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun CollapsibleSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = "Expand/Collapse",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                content()
            }
        }
    }
}
