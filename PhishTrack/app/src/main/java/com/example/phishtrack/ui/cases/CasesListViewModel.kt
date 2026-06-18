package com.example.phishtrack.ui.cases

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phishtrack.data.api.CaseResponse
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.utils.UiState
import com.example.phishtrack.utils.toUserFriendlyMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CasesListViewModel @Inject constructor(
    private val casesRepository: CasesRepository
) : ViewModel() {

    private val _refreshState = mutableStateOf<UiState<Unit>>(UiState.Idle)
    val refreshState: State<UiState<Unit>> = _refreshState

    val casesList: StateFlow<List<CaseResponse>> = casesRepository.cachedCasesFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun refreshCases(status: String?, priority: String?, date: String?) {
        _refreshState.value = UiState.Loading
        viewModelScope.launch {
            val result = casesRepository.refreshCases(status, priority, date)
            result.fold(
                onSuccess = { _refreshState.value = UiState.Success(Unit) },
                onFailure = { _refreshState.value = UiState.Error(it.toUserFriendlyMessage()) }
            )
        }
    }
}
