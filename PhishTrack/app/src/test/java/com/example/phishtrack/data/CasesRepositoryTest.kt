package com.example.phishtrack.data.repository

import app.cash.turbine.test
import com.example.phishtrack.data.api.*
import com.example.phishtrack.data.local.CaseDao
import com.example.phishtrack.data.local.entities.CaseEntity
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

import com.example.phishtrack.data.local.DashboardDao
import com.example.phishtrack.data.local.entities.DashboardCacheEntity
import com.google.gson.Gson

/**
 * Unit tests for [CasesRepository] — covers refresh, create, update, delete, and tags round-trip.
 */
class CasesRepositoryTest {

    private val apiService = mockk<ApiService>()
    private val caseDao = mockk<CaseDao>(relaxed = true)
    private val dashboardDao = mockk<DashboardDao>(relaxed = true)
    private lateinit var repo: CasesRepository

    private val NOW = "2026-06-17T00:00:00.000Z"

    private fun fakeEntity(id: String = "id-1", tags: String = "") = CaseEntity(
        id = id, caseNumber = "CASE-2026-001", userId = "u1", url = "https://example.com",
        description = null, source = "Email", priority = "High", status = "Open",
        tags = tags, createdAt = NOW, updatedAt = NOW
    )

    private fun fakeResponse(id: String = "id-1", tags: List<String> = emptyList()) = CaseResponse(
        id = id, caseNumber = "CASE-2026-001", userId = "u1", url = "https://example.com",
        description = null, source = "Email", priority = "High", status = "Open",
        tags = tags, createdAt = NOW, updatedAt = NOW
    )

    @Before
    fun setUp() {
        every { caseDao.getAllCasesFlow() } returns flowOf(emptyList())
        repo = CasesRepository(apiService, caseDao, dashboardDao)
    }

    // ── refreshCases ──────────────────────────────────────────────────────────

    @Test
    fun `A24 - refreshCases page 1 clears then inserts into DAO`() = runTest {
        val paginated = PaginatedCasesResponse(
            data = listOf(fakeResponse()),
            pagination = PaginationInfo(total = 1, page = 1, limit = 50, pages = 1)
        )
        coEvery { apiService.getCases(any(), any(), any(), any(), any()) } returns paginated

        val result = repo.refreshCases(page = 1)

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { caseDao.clearAllCases() }
        coVerify(exactly = 1) { caseDao.insertCases(any()) }
    }

    @Test
    fun `A25 - refreshCases page 2 does NOT clear, only inserts`() = runTest {
        val paginated = PaginatedCasesResponse(
            data = listOf(fakeResponse()),
            pagination = PaginationInfo(total = 2, page = 2, limit = 1, pages = 2)
        )
        coEvery { apiService.getCases(any(), any(), any(), any(), any()) } returns paginated

        repo.refreshCases(page = 2)

        coVerify(exactly = 0) { caseDao.clearAllCases() }
        coVerify(exactly = 1) { caseDao.insertCases(any()) }
    }

    @Test
    fun `A26 - refreshCases network failure returns Result-failure and does not touch DAO`() = runTest {
        coEvery { apiService.getCases(any(), any(), any(), any(), any()) } throws RuntimeException("timeout")

        val result = repo.refreshCases()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { caseDao.clearAllCases() }
        coVerify(exactly = 0) { caseDao.insertCases(any()) }
    }

    // ── createCase ────────────────────────────────────────────────────────────

    @Test
    fun `A27 - createCase success inserts entity and emits Result-success`() = runTest {
        val response = fakeResponse(tags = listOf("phishing"))
        coEvery { apiService.createCase(any()) } returns response

        repo.createCase("https://example.com", null, "Email", "High", listOf("phishing")).test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            awaitComplete()
        }

        coVerify(exactly = 1) { caseDao.insertCase(any()) }
    }

    @Test
    fun `A28 - createCase network failure does not touch DAO and emits Result-failure`() = runTest {
        coEvery { apiService.createCase(any()) } throws RuntimeException("Server error")

        repo.createCase("https://example.com", null, "Email", "Low", emptyList()).test {
            val result = awaitItem()
            assertTrue(result.isFailure)
            awaitComplete()
        }

        coVerify(exactly = 0) { caseDao.insertCase(any()) }
    }

    // ── updateCase ────────────────────────────────────────────────────────────

    @Test
    fun `A29 - updateCase with local cache entry calls DAO updateCase`() = runTest {
        val response = fakeResponse()
        coEvery { apiService.updateCase(any(), any()) } returns response
        coEvery { caseDao.getCaseById("id-1") } returns fakeEntity()

        repo.updateCase("id-1", "Closed", null, null).test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            awaitComplete()
        }

        coVerify(exactly = 1) { caseDao.updateCase(any()) }
    }

    @Test
    fun `A30 - updateCase without local cache entry still emits Result-success (no DAO update)`() = runTest {
        val response = fakeResponse()
        coEvery { apiService.updateCase(any(), any()) } returns response
        coEvery { caseDao.getCaseById("id-missing") } returns null

        repo.updateCase("id-missing", "Closed", null, null).test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            awaitComplete()
        }

        coVerify(exactly = 0) { caseDao.updateCase(any()) }
    }

    // ── deleteCase ────────────────────────────────────────────────────────────

    @Test
    fun `A31 - deleteCase success calls deleteCaseById`() = runTest {
        coEvery { apiService.deleteCase("id-1") } returns MessageResponse("Deleted")

        repo.deleteCase("id-1").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            awaitComplete()
        }

        coVerify(exactly = 1) { caseDao.deleteCaseById("id-1") }
    }

    // ── tags round-trip ───────────────────────────────────────────────────────

    @Test
    fun `A32 - tags join then split round-trip preserves values`() = runTest {
        val tags = listOf("phishing", "urgent")
        val entity = fakeEntity(tags = tags.joinToString(","))
        every { caseDao.getAllCasesFlow() } returns flowOf(listOf(entity))

        // Re-create repo to pick up the new flow
        val repoWithTags = CasesRepository(apiService, caseDao, dashboardDao)
        repoWithTags.cachedCasesFlow.test {
            val cases = awaitItem()
            assertEquals(tags, cases.first().tags)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `A33 - empty tags string becomes empty list (not list with one empty string)`() = runTest {
        val entity = fakeEntity(tags = "")
        every { caseDao.getAllCasesFlow() } returns flowOf(listOf(entity))

        val repoEmpty = CasesRepository(apiService, caseDao, dashboardDao)
        repoEmpty.cachedCasesFlow.test {
            val cases = awaitItem()
            assertTrue(cases.first().tags.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Dashboard Caching ─────────────────────────────────────────────────────

    @Test
    fun `A34 - getStats successful network fetch populates DashboardDao`() = runTest {
        val response = StatsResponse(users = 10, cases = 20, analyses = 30, reports = 40)
        coEvery { dashboardDao.getCacheById("stats") } returns null
        coEvery { apiService.getDashboardStats() } returns response

        repo.getStats().test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            assertEquals(10, result.getOrNull()?.users)
            awaitComplete()
        }

        coVerify(exactly = 1) { dashboardDao.insertCache(match { it.id == "stats" }) }
    }

    @Test
    fun `A35 - getStats network failure gracefully falls back to DashboardDao cache`() = runTest {
        val cachedResponse = StatsResponse(users = 5, cases = 5, analyses = 5, reports = 5)
        val cacheEntity = DashboardCacheEntity("stats", Gson().toJson(cachedResponse), 0L)
        
        coEvery { dashboardDao.getCacheById("stats") } returns cacheEntity
        coEvery { apiService.getDashboardStats() } throws RuntimeException("Network Offline")

        repo.getStats().test {
            // First emission is from cache
            val firstEmission = awaitItem()
            assertTrue(firstEmission.isSuccess)
            assertEquals(5, firstEmission.getOrNull()?.users)
            
            // Should not emit failure since we already had cache
            awaitComplete()
        }
        
        // Ensure no new cache was inserted due to failure
        coVerify(exactly = 0) { dashboardDao.insertCache(any()) }
    }
}
