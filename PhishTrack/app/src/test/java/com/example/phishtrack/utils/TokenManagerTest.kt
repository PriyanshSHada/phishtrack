package com.example.phishtrack.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [TokenManager] using Robolectric so we have a real SharedPreferences context.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TokenManagerTest {

    private lateinit var tokenManager: TokenManager

    @Before
    fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        // Use a fresh prefs file per test via clearing
        val prefs = ctx.getSharedPreferences("phishtrack_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        tokenManager = TokenManager(ctx)
    }

    // ── Token ──────────────────────────────────────────────────────────────────

    @Test
    fun `A10 - saveToken then getToken returns saved token`() {
        tokenManager.saveToken("test-jwt-token-abc123")
        assertEquals("test-jwt-token-abc123", tokenManager.getToken())
    }

    @Test
    fun `A11 - clearToken then getToken returns null`() {
        tokenManager.saveToken("token-to-clear")
        tokenManager.clearToken()
        assertNull(tokenManager.getToken())
    }

    // ── Email ──────────────────────────────────────────────────────────────────

    @Test
    fun `A12 - saveEmail and getEmail round-trip`() {
        tokenManager.saveEmail("user@example.com")
        assertEquals("user@example.com", tokenManager.getEmail())
    }

    // ── PIN ────────────────────────────────────────────────────────────────────

    @Test
    fun `A13 - setPin and getPin returns the set PIN`() {
        tokenManager.setPin("1234")
        assertEquals("1234", tokenManager.getPin())
    }

    @Test
    fun `A14 - setPinLockEnabled true then isPinLockEnabled returns true`() {
        tokenManager.setPinLockEnabled(true)
        assertTrue(tokenManager.isPinLockEnabled())
    }

    @Test
    fun `A14b - setPinLockEnabled false then isPinLockEnabled returns false`() {
        tokenManager.setPinLockEnabled(false)
        assertFalse(tokenManager.isPinLockEnabled())
    }

    // ── Biometrics ────────────────────────────────────────────────────────────

    @Test
    fun `A15 - setBiometricEnabled false then isBiometricEnabled returns false`() {
        tokenManager.setBiometricEnabled(false)
        assertFalse(tokenManager.isBiometricEnabled())
    }

    @Test
    fun `A15b - setBiometricEnabled true then isBiometricEnabled returns true`() {
        tokenManager.setBiometricEnabled(true)
        assertTrue(tokenManager.isBiometricEnabled())
    }

    // ── User ID ────────────────────────────────────────────────────────────────

    @Test
    fun `saveUserId and getUserId round-trip`() {
        tokenManager.saveUserId("user-uuid-001")
        assertEquals("user-uuid-001", tokenManager.getUserId())
    }

    @Test
    fun `getToken returns null when nothing saved`() {
        assertNull(tokenManager.getToken())
    }
}
