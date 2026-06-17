package com.example.phishtrack.data.api

import okhttp3.ResponseBody
import retrofit2.http.*

interface ApiService {
  // --- Config ---
  @GET("api/config/version")
  suspend fun getVersion(): VersionResponse

  // --- Auth ---
  @POST("api/auth/register")
  suspend fun register(@Body req: RegisterRequest): RegisterResponse

  @POST("api/auth/login")
  suspend fun login(@Body req: LoginRequest): LoginResponse

  @POST("api/auth/verify-otp")
  suspend fun verifyOtp(@Body req: VerifyOtpRequest): TokenResponse

  @POST("api/auth/resend-otp")
  suspend fun resendOtp(@Body req: ResendOtpRequest): MessageResponse

  @GET("api/auth/me")
  suspend fun me(): UserProfile

  // --- Cases ---
  @GET("api/cases")
  suspend fun getCases(
      @Query("status") status: String? = null,
      @Query("priority") priority: String? = null,
      @Query("date") date: String? = null,
      @Query("page") page: Int? = null,
      @Query("limit") limit: Int? = null
  ): PaginatedCasesResponse

  @GET("api/cases")
  suspend fun getCasesLegacy(
      @Query("status") status: String? = null,
      @Query("priority") priority: String? = null,
      @Query("date") date: String? = null
  ): List<CaseResponse>

  @POST("api/cases")
  suspend fun createCase(@Body req: CreateCaseRequest): CaseResponse

  @GET("api/cases/{id}")
  suspend fun getCaseById(@Path("id") id: String): CaseDetailResponse

  @PUT("api/cases/{id}")
  suspend fun updateCase(@Path("id") id: String, @Body req: UpdateCaseRequest): CaseResponse

  @DELETE("api/cases/{id}")
  suspend fun deleteCase(@Path("id") id: String): MessageResponse

  @GET("api/cases/{id}/timeline")
  suspend fun getCaseTimeline(@Path("id") id: String): List<TimelineEvent>

  // --- Analysis ---
  @POST("api/analysis/run")
  suspend fun runAnalysis(@Body req: RunAnalysisRequest): AnalysisResponse

  @GET("api/analysis/{caseId}")
  suspend fun getAnalysis(@Path("caseId") caseId: String): AnalysisResponse

  // --- Reports ---
  @POST("api/reports/generate/{caseId}")
  suspend fun generateReport(@Path("caseId") caseId: String): ReportResponse

  @GET("api/reports/case/{caseId}")
  suspend fun getReportsByCase(@Path("caseId") caseId: String): List<ReportResponse>

  @GET("api/reports/{id}")
  suspend fun getReportById(@Path("id") id: String): ReportResponse

  @GET("api/reports/{id}/verify")
  suspend fun verifyReport(@Path("id") id: String): VerifyReportResponse

  @GET("api/reports/{id}/pdf")
  @Streaming
  suspend fun downloadReportPdf(@Path("id") id: String): ResponseBody

  // --- Dashboard ---
  @GET("api/dashboard/stats")
  suspend fun getDashboardStats(): StatsResponse

  @GET("api/dashboard/recent")
  suspend fun getRecentCases(): List<CaseResponse>

  @GET("api/dashboard/threat-map")
  suspend fun getThreatMap(): List<ThreatLocation>

  @GET("api/dashboard/weekly")
  suspend fun getWeeklyGraph(): WeeklyDashboardResponse

  // --- Audit ---
  @GET("api/audit/logs")
  suspend fun getAuditLogs(): List<AuditLogResponse>

  @GET("api/audit/custody/{caseId}")
  suspend fun getCustodyChain(@Path("caseId") caseId: String): List<ChainOfCustodyResponse>
}
