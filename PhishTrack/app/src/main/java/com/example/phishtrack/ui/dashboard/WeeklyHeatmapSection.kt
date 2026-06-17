package com.example.phishtrack.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phishtrack.data.api.WeeklyGraphData
import java.time.LocalDate

data class HeatmapDay(
    val date: String,
    val count: Int,
    val isToday: Boolean
)

fun buildHeatmapData(
    weeklyData: List<WeeklyGraphData>
): List<HeatmapDay> {
    val countMap = weeklyData.associate { it.date to it.count }
    val today = LocalDate.now()
    val result = mutableListOf<HeatmapDay>()

    for (i in 27 downTo 0) {
        val date = today.minusDays(i.toLong())
        val dateStr = date.toString() // "2026-06-03"
        result.add(
            HeatmapDay(
                date = dateStr,
                count = countMap[dateStr] ?: 0,
                isToday = (i == 0)
            )
        )
    }
    return result
}

fun heatmapColor(count: Int): Color {
    return when {
        count == 0  -> Color(0xFF1A2035)  // empty dark cell
        count <= 2  -> Color(0xFF0A3040)  // low
        count <= 5  -> Color(0xFF0A4A5A)  // medium
        count <= 8  -> Color(0xFF006475)  // high
        else        -> Color(0xFF008FA0)  // very high
    }
}

fun heatmapTextColor(count: Int): Color {
    return when {
        count == 0  -> Color.Transparent
        count <= 2  -> Color(0xFF00B8CC)
        count <= 5  -> Color(0xFF00D4E8)
        else        -> Color(0xFF00F5FF)
    }
}

@Composable
fun WeeklyHeatmapSection(
    weeklyData: List<WeeklyGraphData>,
    modifier: Modifier = Modifier,
    onDateSelected: (String) -> Unit
) {
    val heatmapDays = remember(weeklyData) {
        buildHeatmapData(weeklyData)
    }

    val totalScans = remember(heatmapDays) { heatmapDays.sumOf { it.count } }
    val activeDays = remember(heatmapDays) { heatmapDays.count { it.count > 0 } }
    val currentStreak = remember(heatmapDays) {
        var streak = 0
        for (day in heatmapDays.reversed()) {
            if (day.count > 0) streak++ else break
        }
        streak
    }

    Column(modifier = modifier) {

        // Section title
        Text(
            text = "WEEKLY SCAN ANALYTICS",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8892B0),
            letterSpacing = 1.2.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Main heatmap card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF141829),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(16.dp)
        ) {
            Column {

                // Card header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Scan Heatmap",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Last 28 days",
                            color = Color(0xFF8892B0),
                            fontSize = 11.sp
                        )
                    }
                    // Total pill
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFF00F5FF).copy(alpha = 0.12f),
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "$totalScans scans",
                            color = Color(0xFF00F5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Day labels row — start from the actual day-of-week of 'today minus 27 days'
                // so the column headers align with the data in the grid below.
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                val startDayOfWeek = remember(heatmapDays) {
                    if (heatmapDays.isNotEmpty()) {
                        // Parse the first date and get its ISO day-of-week (1=Mon … 7=Sun)
                        val firstDate = java.time.LocalDate.parse(heatmapDays.first().date)
                        firstDate.dayOfWeek.value - 1 // 0-based index into dayLabels
                    } else 0
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (i in 0 until 7) {
                        val label = dayLabels[(startDayOfWeek + i) % 7]
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            color = Color(0xFF8892B0),
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Heatmap grid — 4 rows x 7 columns = 28 days
                val weeks = heatmapDays.chunked(7)
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { day ->
                            val cellColor = heatmapColor(day.count)
                            val textColor = heatmapTextColor(day.count)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(
                                        color = cellColor,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onDateSelected(day.date) }
                                    .then(
                                        if (day.isToday)
                                            Modifier.border(
                                                width = 1.5.dp,
                                                color = Color(0xFF00F5FF),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (day.count > 0) {
                                    Text(
                                        text = day.count.toString(),
                                        color = textColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        // Fill remaining cells if last week is incomplete
                        repeat(7 - week.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Legend
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Less",
                        color = Color(0xFF8892B0),
                        fontSize = 10.sp
                    )
                    listOf(0, 1, 3, 6, 10).forEach { count ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(
                                    color = heatmapColor(count),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                    Text(
                        text = "More",
                        color = Color(0xFF8892B0),
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Streak stats row — 3 cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Day streak
            StatMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFireDepartment,
                iconTint = Color(0xFF00F5FF),
                value = currentStreak.toString(),
                label = "day streak"
            )
            // Total scans
            StatMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Search,
                iconTint = Color(0xFF00F5FF),
                value = totalScans.toString(),
                label = "total scans"
            )
            // Active days
            StatMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CalendarToday,
                iconTint = Color(0xFF00F5FF),
                value = activeDays.toString(),
                label = "active days"
            )
        }
    }
}

@Composable
fun StatMiniCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFF0D1120),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 0.5.dp,
                color = Color(0xFF1E2540),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(10.dp)
    ) {
        Column {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color(0xFF00F5FF),
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )
            Text(
                text = label,
                color = Color(0xFF8892B0),
                fontSize = 10.sp
            )
        }
    }
}
