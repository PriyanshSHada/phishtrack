package com.example.phishtrack.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class TokenManager(context: Context) {
  private val prefs: SharedPreferences = context.getSharedPreferences("phishtrack_prefs", Context.MODE_PRIVATE)

  fun saveToken(token: String) {
    prefs.edit { putString("auth_token", token) }
  }

  fun getToken(): String? {
    return prefs.getString("auth_token", null)
  }

  fun clearToken() {
    prefs.edit { remove("auth_token") }
  }

  fun saveEmail(email: String) {
    prefs.edit { putString("user_email", email) }
  }

  fun getEmail(): String? {
    return prefs.getString("user_email", null)
  }

  fun saveUserId(userId: String) {
    prefs.edit { putString("user_id", userId) }
  }

  fun getUserId(): String? {
    return prefs.getString("user_id", null)
  }

  fun setBiometricEnabled(enabled: Boolean) {
    prefs.edit { putBoolean("biometric_enabled", enabled) }
  }

  fun isBiometricEnabled(): Boolean {
    return prefs.getBoolean("biometric_enabled", false)
  }

  fun setPinLockEnabled(enabled: Boolean) {
    prefs.edit { putBoolean("pin_lock_enabled", enabled) }
  }

  fun isPinLockEnabled(): Boolean {
    return prefs.getBoolean("pin_lock_enabled", false)
  }

  fun setPin(pin: String) {
    prefs.edit { putString("app_pin", pin) }
  }

  fun getPin(): String? {
    return prefs.getString("app_pin", null)
  }
}
