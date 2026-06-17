package com.example.phishtrack.data.api

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

// --- Authentication ---
data class RegisterRequest(
    val email: String,
    val name: String,
    val password: String,
    val organization: String? = null
)

data class RegisterResponse(
    val id: String,
    val email: String,
    val name: String?,
    @SerializedName("analyst_id") val analystId: String?
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String?,
    val email: String?,
    val token: String?,
    val refreshToken: String?,
    val user: UserProfile?
)

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)

data class TokenResponse(
    val token: String,
    val refreshToken: String?,
    val user: UserProfile?
)

data class ResendOtpRequest(
    val email: String
)

data class MessageResponse(
    val message: String
)

data class UserProfile(
    val id: String,
    val email: String,
    val name: String? = null,
    val organization: String? = null,
    val role: String? = null,
    @SerializedName("analyst_id") val analystId: String? = null
)

// --- Cases ---
data class CreateCaseRequest(
    val url: String,
    val description: String? = null,
    val source: String, // WhatsApp, Email, SMS, Other
    val priority: String, // Low, Medium, High, Critical
    val tags: List<String> = emptyList()
)

data class CaseResponse(
    val id: String,
    @SerializedName("case_number") val caseNumber: String,
    val userId: String,
    val url: String,
    val description: String?,
    val source: String,
    val priority: String,
    val status: String, // Open, Investigating, Closed
    val tags: List<String>,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class PaginationInfo(
    val page: Int,
    val limit: Int,
    val total: Int,
    val pages: Int
)

data class PaginatedCasesResponse(
    val data: List<CaseResponse>,
    val pagination: PaginationInfo
)

data class CaseDetailResponse(
    val id: String,
    @SerializedName("case_number") val caseNumber: String,
    val userId: String,
    val url: String,
    val description: String?,
    val source: String,
    val priority: String,
    val status: String,
    val tags: List<String>,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    val analyses: List<AnalysisResponse> = emptyList(),
    val reports: List<ReportResponse> = emptyList(),
    val auditLogs: List<AuditLogResponse> = emptyList()
)

data class UpdateCaseRequest(
    val status: String? = null,
    val priority: String? = null,
    val description: String? = null
)

data class TimelineEvent(
    val id: String,
    val type: String, // "creation", "audit", "report"
    val at: String,
    val title: String,
    val description: String?
)

// --- Analysis ---
data class RunAnalysisRequest(
    val caseId: String
)

data class AnalysisResponse(
    val id: String,
    val caseId: String,
    @SerializedName("threat_score") val threatScore: Int?,
    val severity: String?,
    @SerializedName("whois_data") val whoisData: JsonObject?,
    @SerializedName("ip_geolocation") val ipGeolocation: JsonObject?,
    @SerializedName("ssl_info") val sslInfo: JsonObject?,
    @SerializedName("redirect_chain") val redirectChain: List<String>,
    @SerializedName("virustotal_result") val virustotalResult: JsonObject?,
    @SerializedName("page_screenshot") val pageScreenshot: String?, // Base64 representation
    @SerializedName("page_source_hash") val pageSourceHash: String?,
    @SerializedName("ai_summary") val aiSummary: String?,
    @SerializedName("ai_indicators") val aiIndicators: List<String>,
    @SerializedName("ai_techniques") val aiTechniques: List<String>,
    @SerializedName("analyzed_at") val analyzedAt: String?
)

// --- Reports ---
data class ReportResponse(
    val id: String,
    val caseId: String,
    val version: Int,
    @SerializedName("pdf_url") val pdfUrl: String?,
    @SerializedName("digital_signature") val digitalSignature: String?,
    val generatedById: String,
    @SerializedName("generated_at") val generatedAt: String,
    @SerializedName("is_tampered") val isTampered: Boolean
)

data class VerifyReportResponse(
    val valid: Boolean,
    val details: VerifyDetails
)

data class VerifyDetails(
    @SerializedName("hmac_valid") val hmacValid: Boolean,
    @SerializedName("file_exists") val fileExists: Boolean,
    @SerializedName("file_hash_valid") val fileHashValid: Boolean,
    @SerializedName("stored_hash") val storedHash: String?,
    @SerializedName("computed_hash") val computedHash: String?
)

// --- Dashboard ---
data class StatsResponse(
    val users: Int,
    val cases: Int,
    val analyses: Int,
    val reports: Int
)

data class ThreatLocation(
    val ip: String?,
    val country: String?,
    val city: String?,
    val latitude: Double?,
    val longitude: Double?,
    @SerializedName("threat_score") val threatScore: Int?,
    val severity: String? = null,
    val caseId: String? = null,
    @SerializedName("case_number") val caseNumber: String? = null,
    val url: String? = null,
    val priority: String? = null,
    @SerializedName("ai_summary") val aiSummary: String? = null,
    @SerializedName("ai_indicators") val aiIndicators: List<String> = emptyList(),
    val isp: String? = null
)

data class WeeklyGraphData(
    val date: String, // "2026-05-27", "2026-05-28", etc.
    val count: Int
)

data class WeeklyDashboardResponse(
    val currentWeek: List<WeeklyGraphData>,
    val totalThisWeek: Int,
    val totalLastWeek: Int
)

// --- Audit & Custody ---
data class AuditLogResponse(
    val id: String,
    val userId: String?,
    val caseId: String?,
    val action: String,
    @SerializedName("ip_address") val ipAddress: String?,
    @SerializedName("device_id") val deviceId: String?,
    val timestamp: String,
    val metadata: JsonObject?
)

data class ChainOfCustodyResponse(
    val id: String,
    val caseId: String,
    val userId: String,
    val action: String,
    val timestamp: String,
    @SerializedName("hash_before") val hashBefore: String?,
    @SerializedName("hash_after") val hashAfter: String?
)

// --- Config ---
data class VersionResponse(
    val minVersion: String,
    val latestVersion: String
)
