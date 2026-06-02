package com.example.phishtrack.ui.auth

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phishtrack.data.api.LoginResponse
import com.example.phishtrack.data.api.RegisterResponse
import com.example.phishtrack.data.api.TokenResponse
import com.example.phishtrack.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = mutableStateOf<UiState<LoginResponse>>(UiState.Idle)
    val loginState: State<UiState<LoginResponse>> = _loginState

    private val _registerState = mutableStateOf<UiState<RegisterResponse>>(UiState.Idle)
    val registerState: State<UiState<RegisterResponse>> = _registerState

    private val _otpVerifyState = mutableStateOf<UiState<TokenResponse>>(UiState.Idle)
    val otpVerifyState: State<UiState<TokenResponse>> = _otpVerifyState

    private val _resendOtpState = mutableStateOf<UiState<String>>(UiState.Idle)
    val resendOtpState: State<UiState<String>> = _resendOtpState

    fun login(email: String, pass: String) {
        _loginState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.login(email, pass).collect { result ->
                result.fold(
                    onSuccess = { _loginState.value = UiState.Success(it) },
                    onFailure = { _loginState.value = UiState.Error(it.message ?: "Login failed") }
                )
            }
        }
    }

    fun register(name: String, email: String, org: String?, pass: String) {
        _registerState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.register(name, email, org, pass).collect { result ->
                result.fold(
                    onSuccess = { _registerState.value = UiState.Success(it) },
                    onFailure = { _registerState.value = UiState.Error(it.message ?: "Registration failed") }
                )
            }
        }
    }

    fun verifyOtp(email: String, otp: String) {
        _otpVerifyState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.verifyOtp(email, otp).collect { result ->
                result.fold(
                    onSuccess = { _otpVerifyState.value = UiState.Success(it) },
                    onFailure = { _otpVerifyState.value = UiState.Error(it.message ?: "OTP Verification failed") }
                )
            }
        }
    }

    fun resendOtp(email: String) {
        _resendOtpState.value = UiState.Loading
        viewModelScope.launch {
            authRepository.resendOtp(email).collect { result ->
                result.fold(
                    onSuccess = { _resendOtpState.value = UiState.Success(it.message) },
                    onFailure = { _resendOtpState.value = UiState.Error(it.message ?: "Resend OTP failed") }
                )
            }
        }
    }

    fun isBiometricEnabled(): Boolean {
        return authRepository.isBiometricEnabled()
    }

    fun isLoggedIn(): Boolean {
        return authRepository.isLoggedIn()
    }

    fun resetStates() {
        _loginState.value = UiState.Idle
        _registerState.value = UiState.Idle
        _otpVerifyState.value = UiState.Idle
        _resendOtpState.value = UiState.Idle
    }
}

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
