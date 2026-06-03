package com.example.phishtrack.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phishtrack.data.api.CaseResponse
import com.example.phishtrack.data.api.StatsResponse
import com.example.phishtrack.data.api.ThreatLocation
import com.example.phishtrack.data.api.WeeklyGraphData
import com.example.phishtrack.ui.auth.UiState

@Composable
fun DashboardScreen(
    analystName: String,
    viewModel: DashboardViewModel,
    onNewCaseClick: () -> Unit,
    onCaseClick: (String) -> Unit,
    onBottomNavClick: (String) -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.loadDashboardData()
    }

    val statsState by viewModel.statsState
    val recentCasesState by viewModel.recentCasesState
    val threatMapState by viewModel.threatMapState
    val weeklyGraphState by viewModel.weeklyGraphState

    Scaffold(
        containerColor = Color(0x0A, 0x0E, 0x1A), // #0A0E1A
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNewCaseClick,
                containerColor = Color(0x00, 0xF5, 0xFF),
                contentColor = Color(0x0A, 0x0E, 0x1A),
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Case")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, $analystName 👋",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        text = "PhishTrack Security Operations Center",
                        color = Color(0x88, 0x92, 0xB0),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Metrics Cards Row
            when (statsState) {
                is UiState.Success -> {
                    val stats = (statsState as UiState.Success).data
                    MetricsGrid(stats)
                }
                is UiState.Loading -> {
                    MetricsGridPlaceholder()
                }
                else -> {
                    MetricsGrid(StatsResponse(0, 0, 0, 0))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Interactive Cyber Threat Radar Map
            Text(
                text = "GLOBAL THREAT RADAR MAP",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val locations = when (threatMapState) {
                is UiState.Success -> (threatMapState as UiState.Success).data
                else -> emptyList()
            }
            ThreatRadarMapCard(locations)

            Spacer(modifier = Modifier.height(24.dp))

            // Weekly Graph Chart
            Text(
                text = "WEEKLY SCAN ANALYTICS",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            val weeklyData = when (weeklyGraphState) {
                is UiState.Success -> (weeklyGraphState as UiState.Success).data
                else -> emptyList()
            }
            WeeklyGraphCard(weeklyData)

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Cases
            Text(
                text = "RECENT INVESTIGATIVE CASES",
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            when (recentCasesState) {
                is UiState.Success -> {
                    val cases = (recentCasesState as UiState.Success).data
                    if (cases.isEmpty()) {
                        EmptyCasesPlaceholder()
                    } else {
                        cases.forEach { case ->
                            CaseItemCard(case = case, onClick = { onCaseClick(case.id) })
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0x00, 0xF5, 0xFF))
                    }
                }
                else -> {
                    EmptyCasesPlaceholder()
                }
            }

            Spacer(modifier = Modifier.height(80.dp)) // padding for floating button / nav
        }
    }
}

@Composable
fun MetricsGrid(stats: StatsResponse) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(title = "Users", value = stats.users.toString(), color = Color(0x4A, 0x9E, 0xFF), modifier = Modifier.weight(1f))
        MetricCard(title = "Cases", value = stats.cases.toString(), color = Color(0x00, 0xF5, 0xFF), modifier = Modifier.weight(1f))
        MetricCard(title = "Scans", value = stats.analyses.toString(), color = Color(0x00, 0xFF, 0x88), modifier = Modifier.weight(1f))
        MetricCard(title = "Reports", value = stats.reports.toString(), color = Color(0xFF, 0x3B, 0x3B), modifier = Modifier.weight(1f))
    }
}

@Composable
fun MetricsGridPlaceholder() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(4) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
                    .background(Color(0x14, 0x18, 0x29), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x2A, 0x35, 0x58).copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            )
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)), // Glassmorphism
        modifier = modifier
            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = title,
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = color,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun ThreatRadarMapCard(locations: List<ThreatLocation>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(12.dp))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "radar")
        val radarRadius by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "radius"
        )
        val radarAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "alpha"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            // Cyber radar custom drawing
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val center = Offset(w / 2, h / 2)

                // Grid background lines
                val gridStroke = Stroke(width = 1f)
                val gridColor = Color(0x2A, 0x35, 0x58).copy(alpha = 0.5f)
                
                // Draw Concentric Radar Circles
                drawCircle(color = gridColor, radius = h * 0.2f, center = center, style = gridStroke)
                drawCircle(color = gridColor, radius = h * 0.4f, center = center, style = gridStroke)
                drawCircle(color = gridColor, radius = h * 0.6f, center = center, style = gridStroke)
                drawCircle(color = gridColor, radius = h * 0.8f, center = center, style = gridStroke)

                // Crosshairs
                drawLine(color = gridColor, start = Offset(0f, h / 2), end = Offset(w, h / 2), strokeWidth = 1f)
                drawLine(color = gridColor, start = Offset(w / 2, 0f), end = Offset(w / 2, h), strokeWidth = 1f)

                // Radar Sweeper Pulse
                drawCircle(
                    color = Color(0x00, 0xF5, 0xFF),
                    radius = h * 0.8f * radarRadius,
                    center = center,
                    style = Stroke(width = 4f),
                    alpha = radarAlpha * 0.6f
                )

                // Dynamic Threat Markers
                // If location lists are available, render them. If empty, draw mock threat nodes.
                val markers = if (locations.isNotEmpty()) {
                    locations.mapNotNull { loc ->
                        if (loc.latitude != null && loc.longitude != null) {
                            // Map geo coordinates into canvas size bounded offset
                            val mx = ((loc.longitude + 180) / 360) * w
                            val my = ((90 - loc.latitude) / 180) * h
                            Offset(mx.toFloat(), my.toFloat())
                        } else null
                    }
                } else {
                    listOf(
                        Offset(w * 0.25f, h * 0.35f),
                        Offset(w * 0.7f, h * 0.25f),
                        Offset(w * 0.45f, h * 0.65f),
                        Offset(w * 0.8f, h * 0.7f)
                    )
                }

                markers.forEach { point ->
                    drawCircle(
                        color = Color(0xFF, 0x3B, 0x3B), // Red alert dot
                        radius = 6f,
                        center = point
                    )
                    drawCircle(
                        color = Color(0xFF, 0x3B, 0x3B),
                        radius = 16f * radarRadius,
                        center = point,
                        style = Stroke(width = 2f),
                        alpha = radarAlpha
                    )
                }
            }
            
            // Map Metadata label overlay
            Text(
                text = "STATUS: ACTIVE MONITORING",
                color = Color(0x00, 0xFF, 0x88), // Green Success
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            )
        }
    }
}

@Composable
fun WeeklyGraphCard(weeklyData: List<WeeklyGraphData>) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw base line
                drawLine(
                    color = Color(0x2A, 0x35, 0x58),
                    start = Offset(0f, h - 20f),
                    end = Offset(w, h - 20f),
                    strokeWidth = 2f
                )

                if (weeklyData.isEmpty()) {
                    // Fallback mock graph drawing
                    val mockCounts = listOf(4, 7, 2, 8, 5, 12, 6, 9)
                    val barWidth = (w / mockCounts.size) - 16f
                    val maxVal = mockCounts.maxOrNull() ?: 1

                    mockCounts.forEachIndexed { i, count ->
                        val barHeight = ((h - 40f) * count) / maxVal
                        val rx = i * (w / mockCounts.size) + 8f
                        val ry = h - 20f - barHeight

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x00, 0xF5, 0xFF), Color(0x4A, 0x9E, 0xFF))
                            ),
                            topLeft = Offset(rx, ry),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                } else {
                    val maxVal = weeklyData.maxOfOrNull { it.count } ?: 1
                    val barWidth = (w / weeklyData.size) - 16f

                    weeklyData.forEachIndexed { i, data ->
                        val barHeight = ((h - 40f) * data.count) / maxVal
                        val rx = i * (w / weeklyData.size) + 8f
                        val ry = h - 20f - barHeight

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0x00, 0xF5, 0xFF), Color(0x4A, 0x9E, 0xFF))
                            ),
                            topLeft = Offset(rx, ry),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CaseItemCard(case: CaseResponse, onClick: () -> Unit) {
    val severityColor = when (case.priority) {
        "Critical" -> Color(0xFF, 0x3B, 0x3B)
        "High" -> Color(0xFF, 0xA5, 0x00)
        "Medium" -> Color(0xFF, 0xD7, 0x00)
        else -> Color(0x4A, 0x9E, 0xFF)
    }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x14, 0x18, 0x29)),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = case.case_number,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                
                // Priority Badge
                Box(
                    modifier = Modifier
                        .background(severityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, severityColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = case.priority,
                        color = severityColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // URL Label
            Text(
                text = case.url,
                color = Color(0x88, 0x92, 0xB0),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = case.status,
                    color = when (case.status) {
                        "Open" -> Color(0x00, 0xF5, 0xFF)
                        "Closed" -> Color(0x00, 0xFF, 0x88)
                        else -> Color(0xFF, 0xA5, 0x00)
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = case.created_at.split("T").firstOrNull() ?: "",
                    color = Color(0x88, 0x92, 0xB0).copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun EmptyCasesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(Color(0x14, 0x18, 0x29), RoundedCornerShape(8.dp))
            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No cases found in this database.",
            color = Color(0x88, 0x92, 0xB0),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
