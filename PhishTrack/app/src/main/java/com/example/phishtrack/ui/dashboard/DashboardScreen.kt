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
            .height(200.dp)
            .border(1.dp, Color(0x2A, 0x35, 0x58), RoundedCornerShape(12.dp))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0f, targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "pulse_scale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.9f, targetValue = 0f,
            animationSpec = infiniteRepeatable(animation = tween(1800, easing = LinearEasing), repeatMode = RepeatMode.Restart),
            label = "pulse_alpha"
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Draw lat/lon graticule grid
                val gridColor = Color(0x2A, 0x35, 0x58).copy(alpha = 0.6f)
                for (lat in listOf(-60f, -30f, 0f, 30f, 60f)) {
                    val y = ((90f - lat) / 180f) * h
                    drawLine(color = gridColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 0.8f)
                }
                for (lon in listOf(-120f, -60f, 0f, 60f, 120f)) {
                    val x = ((lon + 180f) / 360f) * w
                    drawLine(color = gridColor, start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 0.8f)
                }

                // Draw simplified continent blocks (geographic bounding boxes)
                val landColor = Color(0x1E, 0x2A, 0x45)
                fun geoRect(lon1: Float, lat1: Float, lon2: Float, lat2: Float) {
                    val x = ((lon1 + 180f) / 360f) * w
                    val y = ((90f - lat1) / 180f) * h
                    drawRect(color = landColor, topLeft = Offset(x, y),
                        size = Size(((lon2 + 180f) / 360f) * w - x, ((90f - lat2) / 180f) * h - y))
                }
                geoRect(-140f, 70f, -55f, 15f)   // North America
                geoRect(-82f, 12f, -34f, -55f)    // South America
                geoRect(-10f, 71f, 40f, 36f)      // Europe
                geoRect(-17f, 37f, 52f, -35f)     // Africa
                geoRect(40f, 75f, 145f, 10f)      // Asia
                geoRect(113f, -12f, 154f, -39f)   // Australia
                geoRect(-57f, 83f, -18f, 60f)     // Greenland

                // Plot real threat dots with pulsing rings
                locations.forEach { loc ->
                    if (loc.latitude != null && loc.longitude != null) {
                        val mx = ((loc.longitude + 180.0) / 360.0 * w).toFloat()
                        val my = ((90.0 - loc.latitude) / 180.0 * h).toFloat()
                        drawCircle(color = Color(0xFF, 0x3B, 0x3B), radius = 5f, center = Offset(mx, my))
                        drawCircle(color = Color(0xFF, 0x3B, 0x3B), radius = 5f + 18f * pulseScale,
                            center = Offset(mx, my), style = Stroke(width = 2f), alpha = pulseAlpha * 0.8f)
                    }
                }

                // Show ghost placeholder dots only when truly no data
                if (locations.isEmpty()) {
                    val ghosts = listOf(
                        Offset((((-100f + 180f) / 360f) * w), (((90f - 40f) / 180f) * h)),
                        Offset((((10f + 180f) / 360f) * w), (((90f - 51f) / 180f) * h)),
                        Offset((((120f + 180f) / 360f) * w), (((90f - 30f) / 180f) * h)),
                        Offset((((-50f + 180f) / 360f) * w), (((90f - (-15f)) / 180f) * h))
                    )
                    ghosts.forEach { p -> drawCircle(color = Color(0xFF, 0x3B, 0x3B).copy(alpha = 0.3f), radius = 5f, center = p) }
                }
            }

            // Status badge bottom-left
            Row(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).background(Color(0x00, 0xFF, 0x88), CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "LIVE: ${locations.size} THREAT${if (locations.size != 1) "S" else ""} TRACKED",
                    color = Color(0x00, 0xFF, 0x88), fontSize = 9.sp,
                    fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace
                )
            }
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
        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            if (weeklyData.isEmpty()) {
                // Empty state — no fake data
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No scan data this week yet.",
                        color = Color(0x88, 0x92, 0xB0),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            } else {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val maxVal = weeklyData.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
                    val barWidth = (w / weeklyData.size) - 16f

                    // Base line
                    drawLine(color = Color(0x2A, 0x35, 0x58), start = Offset(0f, h - 20f), end = Offset(w, h - 20f), strokeWidth = 2f)

                    weeklyData.forEachIndexed { i, data ->
                        val barHeight = ((h - 40f) * data.count) / maxVal
                        val rx = i * (w / weeklyData.size) + 8f
                        val ry = h - 20f - barHeight
                        drawRoundRect(
                            brush = Brush.verticalGradient(colors = listOf(Color(0x00, 0xF5, 0xFF), Color(0x4A, 0x9E, 0xFF))),
                            topLeft = Offset(rx, ry), size = Size(barWidth, barHeight),
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
