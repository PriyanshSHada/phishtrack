package com.example.phishtrack.ui.dashboard

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phishtrack.data.api.CaseResponse
import com.example.phishtrack.data.api.StatsResponse
import com.example.phishtrack.data.api.ThreatLocation
import com.example.phishtrack.data.api.WeeklyDashboardResponse
import com.example.phishtrack.data.api.WeeklyGraphData
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.utils.UiState
import com.example.phishtrack.utils.toUserFriendlyMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val casesRepository: CasesRepository
) : ViewModel() {

    // Start as Idle (not Loading) — spinners only show after the user reaches the screen
    private val _statsState = mutableStateOf<UiState<StatsResponse>>(UiState.Idle)
    val statsState: State<UiState<StatsResponse>> = _statsState

    private val _recentCasesState = mutableStateOf<UiState<List<CaseResponse>>>(UiState.Idle)
    val recentCasesState: State<UiState<List<CaseResponse>>> = _recentCasesState

    private val _threatMapState = mutableStateOf<UiState<List<ThreatLocation>>>(UiState.Idle)
    val threatMapState: State<UiState<List<ThreatLocation>>> = _threatMapState

    private val _weeklyGraphState = mutableStateOf<UiState<WeeklyDashboardResponse>>(UiState.Idle)
    val weeklyGraphState: State<UiState<WeeklyDashboardResponse>> = _weeklyGraphState

    fun loadDashboardData() {
        _statsState.value = UiState.Loading
        _recentCasesState.value = UiState.Loading
        _threatMapState.value = UiState.Loading
        _weeklyGraphState.value = UiState.Loading

        viewModelScope.launch {
            casesRepository.getStats().collect { result ->
                result.fold(
                    onSuccess = { _statsState.value = UiState.Success(it) },
                    onFailure = { _statsState.value = UiState.Error(it.toUserFriendlyMessage()) }
                )
            }
        }

        viewModelScope.launch {
            casesRepository.getRecentCases().collect { result ->
                result.fold(
                    onSuccess = { _recentCasesState.value = UiState.Success(it.take(5)) },
                    onFailure = { _recentCasesState.value = UiState.Error(it.toUserFriendlyMessage()) }
                )
            }
        }

        viewModelScope.launch {
            casesRepository.getThreatMap().collect { result ->
                result.fold(
                    onSuccess = { _threatMapState.value = UiState.Success(it) },
                    onFailure = { _threatMapState.value = UiState.Error(it.toUserFriendlyMessage()) }
                )
            }
        }

        viewModelScope.launch {
            casesRepository.getWeeklyGraph().collect { result ->
                result.fold(
                    onSuccess = { _weeklyGraphState.value = UiState.Success(it) },
                    onFailure = { _weeklyGraphState.value = UiState.Error(it.toUserFriendlyMessage()) }
                )
            }
        }
    }

    /** Public alias for pull-to-refresh or manual reload from the UI */
    fun refresh() = loadDashboardData()
}
