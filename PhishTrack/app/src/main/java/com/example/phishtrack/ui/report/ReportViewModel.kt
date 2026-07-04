package com.example.phishtrack.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phishtrack.data.api.CaseDetailResponse
import com.example.phishtrack.data.api.ChainOfCustodyResponse
import com.example.phishtrack.data.repository.CasesRepository
import com.example.phishtrack.ui.auth.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val casesRepository: CasesRepository
) : ViewModel() {

    private val _caseDetailState = MutableStateFlow<UiState<CaseDetailResponse>>(UiState.Loading)
    val caseDetailState: StateFlow<UiState<CaseDetailResponse>> = _caseDetailState.asStateFlow()

    private val _custodyChainState = MutableStateFlow<List<ChainOfCustodyResponse>>(emptyList())
    val custodyChainState: StateFlow<List<ChainOfCustodyResponse>> = _custodyChainState.asStateFlow()

    private val _generateReportState = MutableStateFlow<UiState<com.example.phishtrack.data.api.ReportResponse>>(UiState.Idle)
    val generateReportState: StateFlow<UiState<com.example.phishtrack.data.api.ReportResponse>> = _generateReportState.asStateFlow()

    private val _deleteState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val deleteState: StateFlow<UiState<Boolean>> = _deleteState.asStateFlow()

    private val _updatingStatusState = MutableStateFlow<Boolean>(false)
    val updatingStatusState: StateFlow<Boolean> = _updatingStatusState.asStateFlow()

    private val _verifyState = MutableStateFlow<UiState<Boolean>>(UiState.Idle)
    val verifyState: StateFlow<UiState<Boolean>> = _verifyState.asStateFlow()
    
    private val _downloadState = MutableStateFlow<UiState<ByteArray>>(UiState.Idle)
    val downloadState: StateFlow<UiState<ByteArray>> = _downloadState.asStateFlow()

    // Transient UI Events (Snackbars, Toasts) via Channel
    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var currentCaseId: String? = null

    fun initialize(caseId: String) {
        if (currentCaseId != caseId) {
            currentCaseId = caseId
        }
        loadData()
    }

    fun loadData() {
        val caseId = currentCaseId ?: return
        
        _caseDetailState.value = UiState.Loading
        viewModelScope.launch {
            casesRepository.getCaseDetail(caseId).collect { result ->
                result.fold(
                    onSuccess = { _caseDetailState.value = UiState.Success(it) },
                    onFailure = { _caseDetailState.value = UiState.Error(it.message ?: "Failed to load report") }
                )
            }
        }

        viewModelScope.launch {
            casesRepository.getCustodyChain(caseId).collect { result ->
                result.onSuccess { _custodyChainState.value = it }
            }
        }
    }

    fun updateCaseStatus(status: String) {
        val caseId = currentCaseId ?: return
        if (_updatingStatusState.value) return
        
        _updatingStatusState.value = true
        viewModelScope.launch {
            casesRepository.updateCase(caseId, status = status, priority = null, desc = null).collect { result ->
                result.fold(
                    onSuccess = { 
                        loadData() // Refresh details
                    },
                    onFailure = { err ->
                        _uiEvent.send(UiEvent.ShowSnackbar("Failed to update status: ${err.message}"))
                    }
                )
                _updatingStatusState.value = false
            }
        }
    }

    fun setRetentionPolicy(autoDelete: Boolean) {
        val caseId = currentCaseId ?: return
        viewModelScope.launch {
            casesRepository.setRetentionPolicy(caseId, autoDelete).collect { result ->
                result.fold(
                    onSuccess = {
                        loadData() // Refresh details to get new autoDeleteAt
                    },
                    onFailure = { err ->
                        _uiEvent.send(UiEvent.ShowSnackbar("Failed to update retention: ${err.message}"))
                    }
                )
            }
        }
    }

    fun deleteCase() {
        val caseId = currentCaseId ?: return
        _deleteState.value = UiState.Loading
        viewModelScope.launch {
            casesRepository.deleteCase(caseId).collect { result ->
                result.fold(
                    onSuccess = {
                        _deleteState.value = UiState.Success(true)
                    },
                    onFailure = { err ->
                        _deleteState.value = UiState.Error(err.message ?: "Delete failed")
                        _uiEvent.send(UiEvent.ShowSnackbar("Delete failed: ${err.message}"))
                    }
                )
            }
        }
    }

    fun generateReport() {
        val caseId = currentCaseId ?: return
        if (_generateReportState.value is UiState.Loading) return

        _generateReportState.value = UiState.Loading
        viewModelScope.launch {
            casesRepository.generateReport(caseId).collect { result ->
                result.fold(
                    onSuccess = {
                        _generateReportState.value = UiState.Success(it)
                        loadData() // refresh to get new PDF link
                        _uiEvent.send(UiEvent.ShowSnackbar("Report compiled! Signature saved."))
                    },
                    onFailure = { err ->
                        _generateReportState.value = UiState.Error(err.message ?: "Failed to generate report")
                        _uiEvent.send(UiEvent.ShowSnackbar("PDF Error: ${err.message}"))
                    }
                )
            }
        }
    }

    fun downloadReport(reportId: String) {
        if (_downloadState.value is UiState.Loading) return
        
        _downloadState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val bytes = casesRepository.downloadReportBytes(reportId)
                _downloadState.value = UiState.Success(bytes)
            } catch (e: Exception) {
                _downloadState.value = UiState.Error(e.message ?: "Download failed")
                _uiEvent.send(UiEvent.ShowSnackbar("Download error: ${e.message}"))
            }
        }
    }

    fun verifyReport(reportId: String) {
        _verifyState.value = UiState.Loading
        viewModelScope.launch {
            casesRepository.verifyReport(reportId).collect { result ->
                result.fold(
                    onSuccess = {
                        _verifyState.value = UiState.Success(it.valid)
                        val statusMsg = if (it.valid) "✅ Report verified!" else "⚠️ Tamper detected!"
                        _uiEvent.send(UiEvent.ShowSnackbar(statusMsg))
                    },
                    onFailure = { err ->
                        _verifyState.value = UiState.Error(err.message ?: "Verify error")
                        _uiEvent.send(UiEvent.ShowSnackbar("Verify error: ${err.message}"))
                    }
                )
            }
        }
    }


    fun resetDownloadState() {
        _downloadState.value = UiState.Idle
    }

    fun resetGenerateReportState() {
        _generateReportState.value = UiState.Idle
    }
}

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
}
