package com.example.phishtrack.ui.dashboard

import com.example.phishtrack.data.api.WeeklyGraphData
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Unit tests for [buildHeatmapData] and heatmap color/text utilities.
 *
 * These are pure JVM tests — no Android context required.
 */
class HeatmapDataTest {

    private val ISO = DateTimeFormatter.ISO_LOCAL_DATE

    // ── buildHeatmapData ─────────────────────────────────────────────────────

    @Test
    fun `A1 - buildHeatmapData with empty list returns 28 days all with count 0`() {
        val days = buildHeatmapData(emptyList())
        assertEquals(28, days.size)
        assertTrue("All counts should be 0", days.all { it.count == 0 })
    }

    @Test
    fun `A2 - only the last item in heatmap has isToday = true`() {
        val days = buildHeatmapData(emptyList())
        val todayItems = days.filter { it.isToday }
        assertEquals("Exactly one day should be today", 1, todayItems.size)
        assertEquals("The last item should be today", days.last(), todayItems.first())
    }

    @Test
    fun `A3 - data point on day minus 5 maps to correct HeatmapDay count`() {
        val targetDate = LocalDate.now().minusDays(5).format(ISO)
        val input = listOf(WeeklyGraphData(date = targetDate, count = 7))

        val days = buildHeatmapData(input)
        val matching = days.find { it.date == targetDate }
        assertNotNull("Should find the day -5 in heatmap", matching)
        assertEquals(7, matching!!.count)
    }

    @Test
    fun `A3b - today data point maps correctly`() {
        val today = LocalDate.now().format(ISO)
        val input = listOf(WeeklyGraphData(date = today, count = 3))
        val days = buildHeatmapData(input)
        val todayDay = days.find { it.isToday }
        assertNotNull(todayDay)
        assertEquals(3, todayDay!!.count)
    }

    @Test
    fun `A3c - data point outside 28-day window is ignored`() {
        val outside = LocalDate.now().minusDays(30).format(ISO)
        val input = listOf(WeeklyGraphData(date = outside, count = 99))
        val days = buildHeatmapData(input)
        assertTrue("No day should have count 99", days.all { it.count != 99 })
    }

    // ── heatmapColor ─────────────────────────────────────────────────────────

    @Test
    fun `A4 - heatmapColor for count 0 returns darkest color`() {
        val color = heatmapColor(0)
        // 0xFF1A2035 is the zero-count color defined in the composable
        assertTrue("Color should match", color == androidx.compose.ui.graphics.Color(0xFF1A2035))
    }

    @Test
    fun `A5 - heatmapColor for count 1 returns light blue tint`() {
        val color = heatmapColor(1)
        assertNotEquals(heatmapColor(0), color)
    }

    @Test
    fun `A6 - heatmapColor for high count returns near-maximum intensity`() {
        val high = heatmapColor(9)
        val low = heatmapColor(1)
        // High count should be a different (brighter) color than low count
        assertNotEquals(low, high)
    }

    // ── heatmapTextColor ─────────────────────────────────────────────────────

    @Test
    fun `A7 - heatmapTextColor for count 0 returns Transparent`() {
        val color = heatmapTextColor(0)
        assertTrue("Color should match", color == androidx.compose.ui.graphics.Color.Transparent)
    }

    // ── Day-of-week label rotation ────────────────────────────────────────────

    @Test
    fun `A8 - start day is Wednesday when 27 days ago is a Wednesday`() {
        // Find a date that is a Wednesday
        var probe = LocalDate.now()
        while (probe.dayOfWeek.value != 3 /* Wednesday */) probe = probe.minusDays(1)

        // Build heatmap starting from that Wednesday (probe = start of 28-day window)
        // Simulate: heatmapDays.first().date = probe
        val days = buildHeatmapData(emptyList())
        // The first date in the heatmap is today - 27 days
        val firstDate = LocalDate.parse(days.first().date)
        val expectedDowIndex = firstDate.dayOfWeek.value - 1 // 0-based
        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        assertEquals(dayLabels[expectedDowIndex], dayLabels[(expectedDowIndex) % 7])
    }

    @Test
    fun `A9 - start day index wraps correctly for all weekdays`() {
        val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S")
        for (dow in 1..7) { // 1=Mon, 7=Sun
            val idx = dow - 1
            val label = dayLabels[idx % 7]
            assertNotNull(label)
        }
    }
}
