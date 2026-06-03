package com.example.phishtrack.data.api

import com.google.gson.JsonObject

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
    val analyst_id: String?
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val message: String?,
    val email: String?,
    val token: String?,
    val user: UserProfile?
)

data class VerifyOtpRequest(
    val email: String,
    val otp: String
)

data class TokenResponse(
    val token: String,
    val user: UserProfile
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
    val analyst_id: String? = null
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
    val case_number: String,
    val userId: String,
    val url: String,
    val description: String?,
    val source: String,
    val priority: String,
    val status: String, // Open, Investigating, Closed
    val tags: List<String>,
    val created_at: String,
    val updated_at: String
)

data class CaseDetailResponse(
    val id: String,
    val case_number: String,
    val userId: String,
    val url: String,
    val description: String?,
    val source: String,
    val priority: String,
    val status: String,
    val tags: List<String>,
    val created_at: String,
    val updated_at: String,
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
    val threat_score: Int?,
    val severity: String?,
    val whois_data: JsonObject?,
    val ip_geolocation: JsonObject?,
    val ssl_info: JsonObject?,
    val redirect_chain: List<String>,
    val virustotal_result: JsonObject?,
    val page_screenshot: String?, // Base64 representation
    val page_source_hash: String?,
    val ai_summary: String?,
    val ai_indicators: List<String>,
    val ai_techniques: List<String>,
    val analyzed_at: String?
)

// --- Reports ---
data class ReportResponse(
    val id: String,
    val caseId: String,
    val version: Int,
    val pdf_url: String?,
    val digital_signature: String?,
    val generatedById: String,
    val generated_at: String,
    val is_tampered: Boolean
)

data class VerifyReportResponse(
    val valid: Boolean,
    val details: VerifyDetails
)

data class VerifyDetails(
    val hmac_valid: Boolean,
    val file_exists: Boolean,
    val file_hash_valid: Boolean,
    val stored_hash: String?,
    val computed_hash: String?
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
    val threat_score: Int?
)

data class WeeklyGraphData(
    val date: String, // "2026-05-27", "2026-05-28", etc.
    val count: Int
)

// --- Audit & Custody ---
data class AuditLogResponse(
    val id: String,
    val userId: String?,
    val caseId: String?,
    val action: String,
    val ip_address: String?,
    val device_id: String?,
    val timestamp: String,
    val metadata: JsonObject?
)

data class ChainOfCustodyResponse(
    val id: String,
    val caseId: String,
    val userId: String,
    val action: String,
    val timestamp: String,
    val hash_before: String?,
    val hash_after: String?
)
