package com.example.phishtrack.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.phishtrack.data.api.WeeklyGraphData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.window.Dialog
import java.time.Month
import java.time.YearMonth

data class HeatmapDay(
    val date: String,
    val count: Int,
    val isToday: Boolean
)

fun buildHeatmapData(
    weeklyData: List<WeeklyGraphData>,
    selectedMonth: Int?,
    selectedYear: Int?
): List<HeatmapDay> {
    val countMap = weeklyData.associate { it.date to it.count }
    val today = LocalDate.now()
    val todayStr = today.toString()
    val result = mutableListOf<HeatmapDay>()

    if (selectedMonth != null && selectedYear != null) {
        val date = LocalDate.of(selectedYear, selectedMonth, 1)
        val maxDays = date.lengthOfMonth()
        
        val firstDayOfWeek = date.dayOfWeek.value
        val paddingDays = firstDayOfWeek - 1 
        
        for (i in 0 until paddingDays) {
            result.add(HeatmapDay("", -1, false))
        }

        for (i in 1..maxDays) {
            val current = LocalDate.of(selectedYear, selectedMonth, i)
            val dateStr = current.toString()
            result.add(
                HeatmapDay(
                    date = dateStr,
                    count = countMap[dateStr] ?: 0,
                    isToday = (dateStr == todayStr)
                )
            )
        }
    } else {
        for (i in 27 downTo 0) {
            val date = today.minusDays(i.toLong())
            val dateStr = date.toString()
            result.add(
                HeatmapDay(
                    date = dateStr,
                    count = countMap[dateStr] ?: 0,
                    isToday = (i == 0)
                )
            )
        }
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
    selectedMonth: Int?,
    selectedYear: Int?,
    onMonthYearSelected: (Int?, Int?) -> Unit,
    modifier: Modifier = Modifier,
    onDateSelected: (String) -> Unit
) {
    var showMonthYearPicker by remember { mutableStateOf(false) }

    val heatmapDays = remember(weeklyData, selectedMonth, selectedYear) {
        buildHeatmapData(weeklyData, selectedMonth, selectedYear)
    }
    var selectedDay by remember(heatmapDays) { mutableStateOf<HeatmapDay?>(heatmapDays.lastOrNull { it.count != -1 }) }

    val totalScans = remember(heatmapDays) { heatmapDays.filter { it.count != -1 }.sumOf { it.count } }
    val activeDays = remember(heatmapDays) { heatmapDays.filter { it.count != -1 }.count { it.count > 0 } }
    val currentStreak = remember(heatmapDays) {
        var streak = 0
        for (day in heatmapDays.reversed()) {
            if (day.count == -1) continue
            if (day.count > 0) streak++ else break
        }
        streak
    }

    if (showMonthYearPicker) {
        MonthYearPickerDialog(
            currentMonth = selectedMonth,
            currentYear = selectedYear,
            onDismissRequest = { showMonthYearPicker = false },
            onConfirm = { m, y ->
                showMonthYearPicker = false
                onMonthYearSelected(m, y)
            }
        )
    }

    Column(modifier = modifier) {

        // Section title
        Text(
            text = "WEEKLY CASE ANALYTICS",
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
                            text = "Case Heatmap",
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showMonthYearPicker = true }
                                .padding(vertical = 4.dp)
                        ) {
                            val headerText = if (selectedMonth != null && selectedYear != null) {
                                "${Month.of(selectedMonth).getDisplayName(TextStyle.FULL, Locale.getDefault())} $selectedYear"
                            } else {
                                "Last 28 days"
                            }
                            Text(
                                text = headerText,
                                color = Color(0xFF8892B0),
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Month",
                                tint = Color(0xFF00F5FF),
                                modifier = Modifier.size(12.dp)
                            )
                        }
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
                            text = "$totalScans cases",
                            color = Color(0xFF00F5FF),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Day labels row
                val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
                val startDayOfWeek = remember(heatmapDays, selectedMonth) {
                    if (selectedMonth != null) {
                        0 // Fixed Mon-Sun for calendar view
                    } else if (heatmapDays.isNotEmpty()) {
                        val firstDateStr = heatmapDays.firstOrNull { it.count != -1 }?.date
                        if (firstDateStr != null && firstDateStr.isNotEmpty()) {
                            val firstDate = java.time.LocalDate.parse(firstDateStr)
                            firstDate.dayOfWeek.value - 1
                        } else 0
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

                // Heatmap grid
                val weeks = heatmapDays.chunked(7)
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        week.forEach { day ->
                            if (day.count == -1) {
                                Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                            } else {
                                val isSelected = selectedDay?.date == day.date
                                val cellColor by animateColorAsState(
                                    targetValue = if (isSelected) Color(0xFF00F5FF) else heatmapColor(day.count),
                                    animationSpec = tween(durationMillis = 180),
                                    label = "heatmapCellColor"
                                )
                                val cellScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.08f else 1f,
                                    animationSpec = tween(durationMillis = 180),
                                    label = "heatmapCellScale"
                                )
                                val textColor = heatmapTextColor(day.count)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .scale(cellScale)
                                        .background(
                                            color = cellColor,
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .pointerInput(day) {
                                            detectTapGestures(
                                                onTap = {
                                                    selectedDay = day
                                                    onDateSelected(day.date)
                                                },
                                                onLongPress = {
                                                    selectedDay = day
                                                }
                                            )
                                        }
                                        .then(
                                            if (isSelected || day.isToday)
                                                Modifier.border(
                                                    width = if (isSelected) 2.dp else 1.5.dp,
                                                    color = if (isSelected) Color.White else Color(0xFF00F5FF),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                            else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val dayOfMonth = day.date.substringAfterLast("-").toIntOrNull()?.toString() ?: ""
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = dayOfMonth,
                                            color = if (isSelected) Color(0xFF0D1120).copy(alpha = 0.7f) else Color(0xFF8892B0).copy(alpha = 0.6f),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(start = 4.dp, top = 2.dp)
                                        )
                                        if (day.count > 0) {
                                            Text(
                                                text = day.count.toString(),
                                                color = if (isSelected) Color(0xFF0D1120) else textColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
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

                AnimatedVisibility(
                    visible = selectedDay != null,
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(120))
                ) {
                    selectedDay?.let { day ->
                        HeatmapSelectionDetails(day = day)
                    }
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
            // Total cases
            StatMiniCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Search,
                iconTint = Color(0xFF00F5FF),
                value = totalScans.toString(),
                label = "total cases"
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
private fun HeatmapSelectionDetails(day: HeatmapDay) {
    val parsedDate = remember(day.date) {
        runCatching { LocalDate.parse(day.date) }.getOrNull()
    }
    val title = remember(day.date) {
        parsedDate?.let {
            val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
            "${it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())}, ${it.format(formatter)}"
        } ?: day.date
    }
    val activityLabel = when {
        day.count == 0 -> "No cases recorded"
        day.count == 1 -> "1 case recorded"
        else -> "${day.count} cases recorded"
    }
    val statusColor = when {
        day.count == 0 -> Color(0xFF8892B0)
        day.count <= 2 -> Color(0xFF00B8CC)
        day.count <= 5 -> Color(0xFF00D4E8)
        else -> Color(0xFF00F5FF)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D1120), RoundedCornerShape(8.dp))
            .border(1.dp, statusColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color(0xFFE2E8F0),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (day.isToday) "Today" else activityLabel,
                color = Color(0xFF8892B0),
                fontSize = 10.sp
            )
        }
        Box(
            modifier = Modifier
                .background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = activityLabel,
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
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

@Composable
fun MonthYearPickerDialog(
    currentMonth: Int?,
    currentYear: Int?,
    onDismissRequest: () -> Unit,
    onConfirm: (Int?, Int?) -> Unit
) {
    val actualYear = LocalDate.now().year
    val actualMonth = LocalDate.now().monthValue

    val initialYear = currentYear ?: actualYear
    val initialMonth = currentMonth ?: actualMonth
    
    var selectedYear by remember { mutableStateOf(initialYear) }
    var selectedMonth by remember { mutableStateOf(initialMonth) }

    Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = Modifier
                .background(Color(0xFF141829), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF1E2540), RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Select Month & Year",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Year Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "<",
                        color = Color(0xFF00F5FF),
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clickable { selectedYear-- }
                            .padding(8.dp)
                    )
                    Text(
                        text = selectedYear.toString(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = ">",
                        color = if (selectedYear < actualYear) Color(0xFF00F5FF) else Color(0xFF1E2540),
                        fontSize = 24.sp,
                        modifier = Modifier
                            .then(if (selectedYear < actualYear) Modifier.clickable { selectedYear++ } else Modifier)
                            .padding(8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Months Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(200.dp)
                ) {
                    items(12) { index ->
                        val month = index + 1
                        val isSelected = month == selectedMonth
                        val isFutureMonth = selectedYear == actualYear && month > actualMonth
                        
                        val monthName = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.getDefault())
                        
                        val bgColor = when {
                            isSelected -> Color(0xFF00F5FF)
                            isFutureMonth -> Color(0xFF141829)
                            else -> Color(0xFF1E2540)
                        }
                        
                        val textColor = when {
                            isSelected -> Color(0xFF0D1120)
                            isFutureMonth -> Color(0xFF1E2540)
                            else -> Color.White
                        }

                        Box(
                            modifier = Modifier
                                .background(color = bgColor, shape = RoundedCornerShape(8.dp))
                                .then(if (isFutureMonth) Modifier else Modifier.clickable { selectedMonth = month })
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = monthName,
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Reset",
                        color = Color(0xFF8892B0),
                        modifier = Modifier
                            .clickable { onConfirm(null, null) }
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "OK",
                        color = Color(0xFF00F5FF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onConfirm(selectedMonth, selectedYear) }
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}
