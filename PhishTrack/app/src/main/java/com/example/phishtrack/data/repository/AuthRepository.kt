package com.example.phishtrack.data.repository

import com.example.phishtrack.data.api.*
import com.example.phishtrack.utils.TokenManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) {
    fun login(email: String, password: String): Flow<Result<LoginResponse>> = flow {
        try {
            val response = apiService.login(LoginRequest(email, password))
            tokenManager.saveEmail(email)
            if (response.token != null) {
                tokenManager.saveToken(response.token)
                if (response.refreshToken != null) {
                    tokenManager.saveRefreshToken(response.refreshToken)
                }
                tokenManager.saveUserId(response.user?.id ?: "")
            }
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun verifyOtp(email: String, otp: String): Flow<Result<TokenResponse>> = flow {
        try {
            val response = apiService.verifyOtp(VerifyOtpRequest(email, otp))
            tokenManager.saveToken(response.token)
            if (response.refreshToken != null) {
                tokenManager.saveRefreshToken(response.refreshToken)
            }
            tokenManager.saveUserId(response.user?.id ?: "")
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun register(name: String, email: String, org: String?, pass: String): Flow<Result<RegisterResponse>> = flow {
        try {
            val response = apiService.register(RegisterRequest(email, name, pass, org))
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun resendOtp(email: String): Flow<Result<MessageResponse>> = flow {
        try {
            val response = apiService.resendOtp(ResendOtpRequest(email))
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getProfile(): Flow<Result<UserProfile>> = flow {
        try {
            val response = apiService.me()
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun logout() {
        tokenManager.clearToken()
    }

    fun isLoggedIn(): Boolean {
        return !tokenManager.getToken().isNullOrEmpty()
    }

    fun setBiometricEnabled(enabled: Boolean) {
        tokenManager.setBiometricEnabled(enabled)
    }

    fun isBiometricEnabled(): Boolean {
        return tokenManager.isBiometricEnabled()
    }

    fun setPinLockEnabled(enabled: Boolean) {
        tokenManager.setPinLockEnabled(enabled)
    }

    fun isPinLockEnabled(): Boolean {
        return tokenManager.isPinLockEnabled()
    }

    fun setPin(pin: String) {
        tokenManager.setPin(pin)
    }

    fun getPin(): String? {
        return tokenManager.getPin()
    }
}
