package com.example.phishtrack.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class TokenManager(context: Context) {
  
  private val masterKey = MasterKey.Builder(context)
      .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
      .build()

  private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
      context,
      "phishtrack_secure_prefs",
      masterKey,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
  )

  init {
      val oldPrefs = context.getSharedPreferences("phishtrack_prefs", Context.MODE_PRIVATE)
      if (oldPrefs.all.isNotEmpty()) {
          prefs.edit {
              oldPrefs.all.forEach { (key, value) ->
                  when (value) {
                      is String -> putString(key, value)
                      is Boolean -> putBoolean(key, value)
                      is Int -> putInt(key, value)
                      is Float -> putFloat(key, value)
                      is Long -> putLong(key, value)
                  }
              }
          }
          // Clear old unencrypted data after successful migration
          oldPrefs.edit().clear().apply()
      }
  }

  fun saveToken(token: String) {
    prefs.edit { putString("auth_token", token) }
  }

  fun getToken(): String? {
    return prefs.getString("auth_token", null)
  }

  fun saveRefreshToken(token: String) {
    prefs.edit { putString("refresh_token", token) }
  }

  fun getRefreshToken(): String? {
    return prefs.getString("refresh_token", null)
  }

  fun clearToken() {
    prefs.edit { 
        remove("auth_token")
        remove("refresh_token")
    }
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
