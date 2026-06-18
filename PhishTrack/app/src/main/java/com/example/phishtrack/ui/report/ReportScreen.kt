package com.example.phishtrack.ui.report

import android.content.Intent
import android.graphics.BitmapFactory
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
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

    val caseDetailState by viewModel.caseDetailState.collectAsState()
    val custodyChainState by viewModel.custodyChainState.collectAsState()
    val generateReportState by viewModel.generateReportState.collectAsState()
    val deleteState by viewModel.deleteState.collectAsState()
    val updatingStatusState by viewModel.updatingStatusState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val uiEvent by viewModel.uiEvent.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFullscreenImage by remember { mutableStateOf(false) }

    LaunchedEffect(caseId) {
        viewModel.initialize(caseId)
    }

    LaunchedEffect(uiEvent) {
        uiEvent?.let { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = "Dismiss",
                        duration = SnackbarDuration.Short
                    )
                    viewModel.consumeUiEvent()
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
                        color = Color(0x00, 0xF5, 0xFF),
                        modifier = Modifier
                            .clickable { onBackClick() }
                            .padding(8.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0x0A, 0x0E, 0x1A),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Case",
                            tint = Color(0xFF, 0x55, 0x55)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0x0A, 0x0E, 0x1A)
    ) { paddingValues ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Case", color = Color.White) },
                text = { Text("Permanently delete this case and all its analysis data?", color = Color(0x88, 0x92, 0xB0)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            viewModel.deleteCase()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF, 0x3B, 0x3B))
                    ) { Text("DELETE", color = Color.White) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel", color = Color(0x88, 0x92, 0xB0))
                    }
                },
                containerColor = Color(0x14, 0x18, 0x29)
            )
        }

        when (caseDetailState) {
            is UiState.Success -> {
                val detail = (caseDetailState as UiState.Success).data
                val analysis = detail.analyses.firstOrNull()

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
                            
                            var bitmap by remember(screenshot) { mutableStateOf<ImageBitmap?>(null) }
                            
                            LaunchedEffect(screenshot) {
                                if (screenshot != null && screenshot.startsWith("data:image/png;base64,")) {
                                    withContext(Dispatchers.IO) {
                                        try {
                                            val base64Data = screenshot.substringAfter("base64,")
                                            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size).asImageBitmap()
                                        } catch (e: Exception) {}
                                    }
                                }
                            }

                            bitmap?.let {
                                Image(
                                    bitmap = it,
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
                        .padding(16.dp)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(8.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = "CASE: ${detail.caseNumber}", color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "Target URL: ${detail.url}", color = Color(0x88, 0x92, 0xB0), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "UPDATE STATUS", color = Color(0x88, 0x92, 0xB0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val statuses = listOf("Open", "Investigating", "Closed", "False_Positive")
                                statuses.forEach { s ->
                                    val isSelected = detail.status == s
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = if (isSelected) Color(0x00, 0xF5, 0xFF).copy(alpha = 0.2f) else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .border(1.dp, if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x2A, 0x35, 0x58), RoundedCornerShape(4.dp))
                                            .clickable {
                                                viewModel.updateCaseStatus(s)
                                            }
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected && updatingStatusState) {
                                            CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF), modifier = Modifier.size(10.dp), strokeWidth = 1.dp)
                                        } else {
                                            Text(text = s.replace("_", " "), color = if (isSelected) Color(0x00, 0xF5, 0xFF) else Color(0x88, 0x92, 0xB0), fontSize = 9.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(text = "FORENSIC EVALUATION SUMMARY", color = Color(0x88, 0x92, 0xB0), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = analysis?.aiSummary ?: "No summary available.",
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Justify,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x14, 0x18, 0x29))
                            .padding(16.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val techniques = analysis?.aiTechniques ?: emptyList()
                    if (techniques.isNotEmpty()) {
                        Text(text = "PHISHING TECHNIQUES DETECTED", color = Color(0x88, 0x92, 0xB0), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            techniques.forEach { tech ->
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .border(1.dp, Color(0xFF, 0x3B, 0x3B), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(text = tech, color = Color(0xFF, 0x3B, 0x3B), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    val score = analysis?.threatScore ?: 0
                    val ringColor = when {
                        score >= 70 -> Color(0xFF, 0x3B, 0x3B)
                        score >= 40 -> Color(0xFF, 0xA5, 0x00)
                        else -> Color(0x00, 0xFF, 0x88)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(110.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawArc(color = Color(0x2A, 0x35, 0x58), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 24f))
                                drawArc(color = ringColor, startAngle = -90f, sweepAngle = (score / 100f) * 360f, useCenter = false, style = Stroke(width = 24f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "$score", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text(text = "SCORE", color = Color(0x88, 0x92, 0xB0), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(32.dp))
                        Column {
                            Text(text = "Threat Severity", color = Color(0x88, 0x92, 0xB0), fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .background(ringColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                    .border(1.dp, ringColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(text = analysis?.severity ?: "Low", color = ringColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    Text(text = "FORENSIC ARTIFACT DETAILS", color = Color(0x88, 0x92, 0xB0), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    CollapsibleSection(title = "WHOIS Domain Lookup") {
                        val whois = analysis?.whoisData
                        if (whois != null) {
                            Text(text = "Registrar: ${whois.safeString("registrar") ?: "Unavailable"}", color = Color.White, fontSize = 13.sp)
                            Text(text = "Country: ${whois.safeString("country") ?: "Unavailable"}", color = Color.White, fontSize = 13.sp)
                            Text(text = "Creation Date: ${whois.safeString("creationDate") ?: "Unavailable"}", color = Color.White, fontSize = 13.sp)
                        } else {
                            Text(text = "No WHOIS data available.", color = Color(0x88, 0x92, 0xB0), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CollapsibleSection(title = "Network & SSL Info") {
                        val geo = analysis?.ipGeolocation
                        val ssl = analysis?.sslInfo
                        Text(text = "Resolved IP: ${geo.safeString("ip") ?: "Unavailable"}", color = Color.White, fontSize = 13.sp)
                        Text(text = "IP Location: ${geo.safeString("city") ?: "Unavailable"}, ${geo.safeString("country") ?: "Unavailable"}", color = Color.White, fontSize = 13.sp)
                        Text(text = "SSL Valid: ${ssl.safeBoolean("valid") == true}", color = Color.White, fontSize = 13.sp)
                        Text(text = "SSL Issuer: ${ssl.safeString("issuer") ?: "Unavailable"}", color = Color.White, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CollapsibleSection(title = "VirusTotal Scan") {
                        val vt = analysis?.virustotalResult
                        Text(text = "Engines flagged: ${vt.safeInt("maliciousCount") ?: 0} malicious matches.", color = Color.White, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CollapsibleSection(title = "Redirect Chain") {
                        val chain = analysis?.redirectChain ?: emptyList()
                        if (chain.isEmpty()) {
                            Text(text = "No redirects observed.", color = Color(0x88, 0x92, 0xB0), fontSize = 13.sp)
                        } else {
                            chain.forEachIndexed { index, url ->
                                Text(text = "${index + 1}. $url", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CollapsibleSection(title = "Evidence Screenshot") {
                        val screenshot = analysis?.pageScreenshot
                        if (screenshot != null && screenshot.startsWith("data:image/png;base64,")) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showFullscreenImage = true }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                var bitmap by remember(screenshot) { mutableStateOf<ImageBitmap?>(null) }
                                var isDecoding by remember(screenshot) { mutableStateOf(true) }
                                var decodeError by remember(screenshot) { mutableStateOf(false) }

                                LaunchedEffect(screenshot) {
                                    isDecoding = true
                                    try {
                                        withContext(Dispatchers.IO) {
                                            val base64Data = screenshot.substringAfter("base64,")
                                            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                            bitmap = bmp.asImageBitmap()
                                        }
                                    } catch (e: Exception) {
                                        decodeError = true
                                    } finally {
                                        isDecoding = false
                                    }
                                }

                                if (isDecoding) {
                                    CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
                                } else if (decodeError || bitmap == null) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.Warning, contentDescription = "Error", tint = Color(0xFF, 0x3B, 0x3B))
                                        Text("Screenshot unavailable", color = Color(0xFF, 0x3B, 0x3B), fontSize = 13.sp)
                                    }
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                            bitmap = bitmap!!,
                                            contentDescription = "Evidence Screenshot",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Tap to expand", color = Color(0x88, 0x92, 0xB0), fontSize = 11.sp)
                                    }
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth().padding(16.dp)
                            ) {
                                Icon(Icons.Default.ImageNotSupported, contentDescription = "Empty", tint = Color(0x88, 0x92, 0xB0))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "No screenshot captured.", color = Color(0x88, 0x92, 0xB0), fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    CollapsibleSection(title = "Chain of Custody Audit Trail") {
                        if (custodyChainState.isEmpty()) {
                            Text(
                                text = "No custody chain records found.",
                                color = Color(0x88, 0x92, 0xB0),
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        } else {
                            custodyChainState.forEachIndexed { index, event ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.width(24.dp)
                                    ) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0x00, 0xF5, 0xFF)))
                                        if (index < custodyChainState.size - 1) {
                                            Box(modifier = Modifier.width(2.dp).height(95.dp).background(Color(0x2A, 0x35, 0x58)))
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = event.action.uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(text = "Timestamp: ${event.timestamp}", color = Color(0x88, 0x92, 0xB0), fontSize = 11.sp)
                                        Text(text = "Analyst ID: ${event.userId}", color = Color(0x88, 0x92, 0xB0), fontSize = 11.sp)
                                        Spacer(modifier = Modifier.height(6.dp))

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(Color(0x0A, 0x0E, 0x1A))
                                                .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(6.dp))
                                                .padding(8.dp)
                                        ) {
                                            Text(text = "SHA-256 BEFORE:", color = Color(0x00, 0xF5, 0xFF), fontWeight = FontWeight.SemiBold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            Text(text = event.hashBefore ?: "N/A (INITIAL RECORD)", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 4.dp))
                                            Text(text = "SHA-256 AFTER:", color = Color(0x00, 0xFF, 0x88), fontWeight = FontWeight.SemiBold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                            Text(text = event.hashAfter ?: "N/A", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val isGenerating = generateReportState is UiState.Loading
                        val generateFailed = generateReportState is UiState.Error
                        
                        Button(
                            onClick = { viewModel.generateReport() },
                            enabled = !isGenerating,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x00, 0xF5, 0xFF)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(48.dp)
                        ) {
                            if (isGenerating) {
                                CircularProgressIndicator(color = Color(0x0A, 0x0E, 0x1A), strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Text(text = if (generateFailed) "RETRY" else "COMPILE", color = Color(0x0A, 0x0E, 0x1A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }

                        val latestReport = detail.reports.firstOrNull()
                        val isDownloading = downloadState is UiState.Loading
                        Button(
                            onClick = {
                                if (latestReport != null) {
                                    viewModel.downloadReport(latestReport.id)
                                } else {
                                    coroutineScope.launch { snackbarHostState.showSnackbar("Compile the report first") }
                                }
                            },
                            enabled = latestReport != null && !isDownloading,
                            colors = ButtonDefaults.buttonColors(containerColor = if (latestReport != null) Color(0x00, 0xFF, 0x88).copy(alpha = 0.15f) else Color(0x2A, 0x35, 0x58)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(48.dp).border(1.dp, if (latestReport != null) Color(0x00, 0xFF, 0x88) else Color(0x2A, 0x35, 0x58), RoundedCornerShape(6.dp))
                        ) {
                            if (isDownloading) {
                                CircularProgressIndicator(color = Color(0x00, 0xFF, 0x88), strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Text(text = "OPEN PDF", color = if (latestReport != null) Color(0x00, 0xFF, 0x88) else Color(0x55, 0x55, 0x55), fontWeight = FontWeight.Bold, fontSize = 11.sp)
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x14, 0x18, 0x29)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(48.dp).border(1.dp, Color(0x00, 0xF5, 0xFF), RoundedCornerShape(6.dp))
                        ) {
                            Text(text = "VERIFY", color = Color(0x00, 0xF5, 0xFF), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
                }
            }
            else -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Failed to load report details.", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadData() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x00, 0xF5, 0xFF)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RETRY", color = Color(0x0A, 0x0E, 0x1A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
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
            .background(Color(0x14, 0x18, 0x29))
            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(8.dp))
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
                tint = Color(0x88, 0x92, 0xB0)
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
