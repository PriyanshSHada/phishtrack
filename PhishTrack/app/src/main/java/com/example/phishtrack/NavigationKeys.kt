package com.example.phishtrack

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Splash : NavKey
@Serializable data object Login : NavKey
@Serializable data object SignUp : NavKey
@Serializable data class OtpVerify(val email: String) : NavKey
@Serializable data object Main : NavKey
@Serializable data object NewCase : NavKey
@Serializable data class AnalysisLoading(val caseId: String) : NavKey
@Serializable data class Report(val caseId: String) : NavKey
@Serializable data object SecurityCheck : NavKey
@Serializable data class CaseDetail(val caseId: String) : NavKey
@Serializable data class UpdateRequired(val updateUrl: String) : NavKey
