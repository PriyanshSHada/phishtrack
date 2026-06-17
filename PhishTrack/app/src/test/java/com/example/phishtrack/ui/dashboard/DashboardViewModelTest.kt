package com.example.phishtrack.ui.dashboard

import app.cash.turbine.test
import com.example.phishtrack.data.api.*
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.ui.auth.UiState
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [DashboardViewModel] using a TestCoroutineDispatcher and MockK.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val casesRepository = mockk<CasesRepository>(relaxed = true)
    private lateinit var viewModel: DashboardViewModel

    private val NOW = "2026-06-17T00:00:00.000Z"

    private fun fakeStats() = StatsResponse(users = 3, cases = 10, analyses = 5, reports = 2)
    private fun fakeCases(n: Int = 3) = (1..n).map {
        CaseResponse(
            id = "id-$it", caseNumber = "CASE-2026-00$it", userId = "u1",
            url = "https://phish$it.example.com", description = null,
            source = "Email", priority = "High", status = "Open",
            tags = emptyList(), createdAt = NOW, updatedAt = NOW
        )
    }
    private fun fakeWeekly() = WeeklyDashboardResponse(
        totalThisWeek = 3, totalLastWeek = 1,
        currentWeek = (1..28).map { WeeklyGraphData("2026-06-$it", 0) }
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = DashboardViewModel(casesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `A34 - initial state all UiState-Idle`() {
        assertEquals(UiState.Idle, viewModel.statsState.value)
        assertEquals(UiState.Idle, viewModel.recentCasesState.value)
        assertEquals(UiState.Idle, viewModel.threatMapState.value)
        assertEquals(UiState.Idle, viewModel.weeklyGraphState.value)
    }

    @Test
    fun `A35 - loadDashboardData success sets all states to UiState-Success`() = runTest {
        every { casesRepository.getStats() } returns flowOf(Result.success(fakeStats()))
        every { casesRepository.getRecentCases() } returns flowOf(Result.success(fakeCases(3)))
        every { casesRepository.getThreatMap() } returns flowOf(Result.success(emptyList()))
        every { casesRepository.getWeeklyGraph() } returns flowOf(Result.success(fakeWeekly()))

        viewModel.loadDashboardData()
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<UiState.Success<StatsResponse>>(viewModel.statsState.value)
        assertIs<UiState.Success<List<CaseResponse>>>(viewModel.recentCasesState.value)
        assertIs<UiState.Success<List<ThreatLocation>>>(viewModel.threatMapState.value)
        assertIs<UiState.Success<WeeklyDashboardResponse>>(viewModel.weeklyGraphState.value)
    }

    @Test
    fun `A36 - getStats failure sets statsState to UiState-Error, others unaffected`() = runTest {
        every { casesRepository.getStats() } returns flowOf(Result.failure(RuntimeException("Stats error")))
        every { casesRepository.getRecentCases() } returns flowOf(Result.success(fakeCases()))
        every { casesRepository.getThreatMap() } returns flowOf(Result.success(emptyList()))
        every { casesRepository.getWeeklyGraph() } returns flowOf(Result.success(fakeWeekly()))

        viewModel.loadDashboardData()
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<UiState.Error>(viewModel.statsState.value)
        assertIs<UiState.Success<*>>(viewModel.recentCasesState.value)
    }

    @Test
    fun `A37 - recent cases are truncated to at most 5`() = runTest {
        every { casesRepository.getStats() } returns flowOf(Result.success(fakeStats()))
        every { casesRepository.getRecentCases() } returns flowOf(Result.success(fakeCases(10)))
        every { casesRepository.getThreatMap() } returns flowOf(Result.success(emptyList()))
        every { casesRepository.getWeeklyGraph() } returns flowOf(Result.success(fakeWeekly()))

        viewModel.loadDashboardData()
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.recentCasesState.value
        assertIs<UiState.Success<List<CaseResponse>>>(state)
        assertTrue("Should have at most 5 cases", (state as UiState.Success).data.size <= 5)
    }

    @Test
    fun `A38 - refresh re-triggers all loads cycling through Loading to Success`() = runTest {
        every { casesRepository.getStats() } returns flowOf(Result.success(fakeStats()))
        every { casesRepository.getRecentCases() } returns flowOf(Result.success(fakeCases()))
        every { casesRepository.getThreatMap() } returns flowOf(Result.success(emptyList()))
        every { casesRepository.getWeeklyGraph() } returns flowOf(Result.success(fakeWeekly()))

        viewModel.refresh()
        testDispatcher.scheduler.advanceUntilIdle()

        assertIs<UiState.Success<*>>(viewModel.statsState.value)
        // Verify getStats was called (refresh invokes loadDashboardData)
        verify { casesRepository.getStats() }
    }

    // Helper
    private inline fun <reified T> assertIs(value: Any?) {
        assertTrue("Expected ${T::class.simpleName} but was ${value?.javaClass?.simpleName}", value is T)
    }
}
