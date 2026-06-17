package com.example.phishtrack.data.repository

import app.cash.turbine.test
import com.example.phishtrack.data.api.*
import com.example.phishtrack.utils.TokenManager
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [AuthRepository] using MockK and Turbine.
 * No Android context required — TokenManager is fully mocked.
 */
class AuthRepositoryTest {

    private val apiService = mockk<ApiService>()
    private val tokenManager = mockk<TokenManager>(relaxed = true)
    private lateinit var repo: AuthRepository

    @Before
    fun setUp() {
        repo = AuthRepository(apiService, tokenManager)
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Test
    fun `A16 - login success with token saves token and email`() = runTest {
        val response = LoginResponse(
            token = "access-token-123",
            refreshToken = "refresh-token-123",
            user = UserProfile("uid-1", "user@example.com", name="analyst"),
            message = null,
            email = "user@example.com"
        )
        coEvery { apiService.login(any()) } returns response

        repo.login("user@example.com", "Password1!").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            awaitComplete()
        }

        verify { tokenManager.saveEmail("user@example.com") }
        verify { tokenManager.saveToken("access-token-123") }
        verify { tokenManager.saveRefreshToken("refresh-token-123") }
        verify { tokenManager.saveUserId("uid-1") }
    }

    @Test
    fun `A17 - login success with null token saves email but NOT token`() = runTest {
        val response = LoginResponse(
            token = null,
            refreshToken = null,
            user = null,
            message = "OTP sent to your email",
            email = "user@example.com"
        )
        coEvery { apiService.login(any()) } returns response

        repo.login("user@example.com", "Password1!").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            awaitComplete()
        }

        verify { tokenManager.saveEmail("user@example.com") }
        verify(exactly = 0) { tokenManager.saveToken(any()) }
    }

    @Test
    fun `A18 - login network error emits Result-failure`() = runTest {
        coEvery { apiService.login(any()) } throws RuntimeException("Network error")

        repo.login("user@example.com", "Password1!").test {
            val result = awaitItem()
            assertTrue(result.isFailure)
            assertEquals("Network error", result.exceptionOrNull()?.message)
            awaitComplete()
        }
    }

    // ── verifyOtp ─────────────────────────────────────────────────────────────

    @Test
    fun `A19 - verifyOtp success saves token and userId`() = runTest {
        val response = TokenResponse(
            token = "verified-token",
            refreshToken = "refresh-token-xyz",
            user = UserProfile("uid-2", "user@example.com", name="analyst")
        )
        coEvery { apiService.verifyOtp(any()) } returns response

        repo.verifyOtp("user@example.com", "123456").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            awaitComplete()
        }

        verify { tokenManager.saveToken("verified-token") }
        verify { tokenManager.saveRefreshToken("refresh-token-xyz") }
        verify { tokenManager.saveUserId("uid-2") }
    }

    @Test
    fun `A20 - verifyOtp with null user calls saveUserId with empty string (no NPE)`() = runTest {
        val response = TokenResponse(token = "token-no-user", refreshToken = null, user = null)
        coEvery { apiService.verifyOtp(any()) } returns response

        repo.verifyOtp("user@example.com", "123456").test {
            val result = awaitItem()
            assertTrue(result.isSuccess)
            awaitComplete()
        }

        verify { tokenManager.saveUserId("") }
    }

    // ── logout / isLoggedIn ───────────────────────────────────────────────────

    @Test
    fun `A21 - logout calls tokenManager clearToken`() {
        repo.logout()
        verify { tokenManager.clearToken() }
    }

    @Test
    fun `A22 - isLoggedIn returns true when token exists`() {
        every { tokenManager.getToken() } returns "some-valid-token"
        assertTrue(repo.isLoggedIn())
    }

    @Test
    fun `A23 - isLoggedIn returns false when no token`() {
        every { tokenManager.getToken() } returns null
        assertFalse(repo.isLoggedIn())
    }

    @Test
    fun `A23b - isLoggedIn returns false when token is empty string`() {
        every { tokenManager.getToken() } returns ""
        assertFalse(repo.isLoggedIn())
    }
}
