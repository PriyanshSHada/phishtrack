package com.example.phishtrack.ui.cases

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.phishtrack.data.api.CaseResponse
import com.example.phishtrack.data.local.CaseDao
import com.example.phishtrack.data.local.entities.CaseEntity
import com.example.phishtrack.data.repository.CasesRepository
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Compose UI tests for [CasesListScreen] using Robolectric + createComposeRule.
 * Tests filtering, sorting, date chip, and empty state behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CasesListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val caseDao = mockk<CaseDao>(relaxed = true)
    private val dashboardDao = mockk<com.example.phishtrack.data.local.DashboardDao>(relaxed = true)
    private val apiService = mockk<com.example.phishtrack.data.api.ApiService>(relaxed = true)
    private lateinit var casesRepository: CasesRepository

    private val NOW = "2026-06-17T00:00:00.000Z"

    private fun makeCase(
        id: String,
        caseNumber: String,
        url: String,
        status: String = "Open",
        priority: String = "Low"
    ) = CaseResponse(
        id = id, caseNumber = caseNumber, userId = "u1", title = "Test Case",
        targetType = "URL", url = url, targetIp = null,
        description = null, source = "Email", priority = priority,
        status = status, tags = emptyList(), createdAt = NOW, updatedAt = NOW
    )

    private fun makeEntity(case: CaseResponse) = CaseEntity(
        id = case.id, caseNumber = case.caseNumber, userId = case.userId, title = case.title,
        targetType = case.targetType, url = case.displayTarget(), targetIp = case.targetIp,
        description = null, source = case.source, priority = case.priority,
        status = case.status, tags = "", createdAt = case.createdAt, updatedAt = case.updatedAt
    )

    private val sampleCases = listOf(
        makeCase("id-1", "CASE-2026-001", "https://phish1.example.com", "Open", "Critical"),
        makeCase("id-2", "CASE-2026-002", "https://login-paypal.evil.com", "Investigating", "High"),
        makeCase("id-3", "CASE-2026-003", "https://safe.example.com", "Closed", "Low")
    )

    @Before
    fun setUp() {
        val entities = sampleCases.map { makeEntity(it) }
        every { caseDao.getAllCasesFlow() } returns flowOf(entities)
        coEvery { apiService.getCases(any(), any(), any(), any(), any()) } returns
            com.example.phishtrack.data.api.PaginatedCasesResponse(
                data = sampleCases,
                pagination = com.example.phishtrack.data.api.PaginationInfo(1, 50, 3, 1)
            )
        casesRepository = CasesRepository(apiService, caseDao, dashboardDao)
    }

    private fun launchScreen(initialDateFilter: String? = null) {
        composeTestRule.setContent {
            CasesListScreen(
                casesRepository = casesRepository,
                initialDateFilter = initialDateFilter,
                onClearDateFilter = {},
                onCaseClick = {}
            )
        }
    }

    // ── A39 — Search by URL ───────────────────────────────────────────────────

    @Test
    fun `A39 - search by URL substring shows only matching case`() {
        launchScreen()

        composeTestRule.onNodeWithTag("searchField")
            .performTextInput("paypal")

        composeTestRule.onNodeWithText("https://login-paypal.evil.com")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("https://phish1.example.com")
            .assertDoesNotExist()
    }

    // ── A40 — Search by case number ───────────────────────────────────────────

    @Test
    fun `A40 - search by case number shows only matching case`() {
        launchScreen()

        composeTestRule.onNodeWithTag("searchField")
            .performTextInput("001")

        composeTestRule.onNodeWithText("CASE-2026-001")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("CASE-2026-002")
            .assertDoesNotExist()
    }

    // ── A41 — Clear search ────────────────────────────────────────────────────

    @org.junit.Ignore("Compose performTextClearance bug in Robolectric")
    @Test
    fun `A41 - clearing search shows all cases`() {
        launchScreen()

        val searchField = composeTestRule.onNodeWithTag("searchField")
        searchField.performTextInput("paypal")
        searchField.performTextReplacement("")

        composeTestRule.onNodeWithText("https://phish1.example.com").assertExists()
        composeTestRule.onNodeWithText("https://safe.example.com").assertExists()
    }

    // ── A46 — Date filter chip visible ────────────────────────────────────────

    @Test
    fun `A46 - date filter chip visible when initialDateFilter is provided`() {
        launchScreen(initialDateFilter = "2026-06-17")

        composeTestRule.onNodeWithText("2026-06-17 ✕").assertIsDisplayed()
    }

    // ── A47 — Clear date filter ───────────────────────────────────────────────

    @Test
    fun `A47 - clicking date filter chip removes it`() {
        var cleared = false
        composeTestRule.setContent {
            CasesListScreen(
                casesRepository = casesRepository,
                initialDateFilter = "2026-06-17",
                onClearDateFilter = { cleared = true },
                onCaseClick = {}
            )
        }

        composeTestRule.onNodeWithText("2026-06-17 ✕").performClick()

        assert(cleared) { "onClearDateFilter should have been called" }
        composeTestRule.onNodeWithText("2026-06-17 ✕").assertDoesNotExist()
    }

    // ── A48 — Empty state ─────────────────────────────────────────────────────

    @Test
    fun `A48 - empty search shows no cases found message`() {
        launchScreen()

        composeTestRule.onNodeWithTag("searchField")
            .performTextInput("CASE-9999-NOTHING")

        composeTestRule.onNodeWithText("No cases found.").assertIsDisplayed()
    }
}
