package com.example.phishtrack.data.repository

import com.example.phishtrack.data.api.*
import com.example.phishtrack.data.local.CaseDao
import com.example.phishtrack.data.local.entities.CaseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CasesRepository @Inject constructor(
    private val apiService: ApiService,
    private val caseDao: CaseDao
) {
    // Expose cached cases flow
    val cachedCasesFlow: Flow<List<CaseResponse>> = caseDao.getAllCasesFlow().map { entities ->
        entities.map { entity ->
            CaseResponse(
                id = entity.id,
                caseNumber = entity.caseNumber,
                userId = entity.userId,
                url = entity.url,
                description = entity.description,
                source = entity.source,
                priority = entity.priority,
                status = entity.status,
                tags = if (entity.tags.isEmpty()) emptyList() else entity.tags.split(","),
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
    }

    // Refresh cases from server and update local cache
    suspend fun refreshCases(status: String? = null, priority: String? = null, date: String? = null, page: Int = 1, limit: Int = 50): Result<Unit> {
        return try {
            val paginatedResponse = apiService.getCases(status, priority, date, page, limit)
            val networkCases = paginatedResponse.data
            val entities = networkCases.map { case ->
                CaseEntity(
                    id = case.id,
                    caseNumber = case.caseNumber,
                    userId = case.userId,
                    url = case.url,
                    description = case.description,
                    source = case.source,
                    priority = case.priority,
                    status = case.status,
                    tags = case.tags.joinToString(","),
                    createdAt = case.createdAt,
                    updatedAt = case.updatedAt
                )
            }
            // Only clear cache on first page; append for subsequent pages
            if (page <= 1) {
                caseDao.clearAllCases()
            }
            caseDao.insertCases(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Get total count of cases from the server
    suspend fun getCasesCount(status: String? = null, priority: String? = null, date: String? = null): Int {
        return try {
            val response = apiService.getCases(status, priority, date, 1, 1)
            response.pagination.total
        } catch (e: Exception) {
            val cached = caseDao.getAllCasesFlow().firstOrNull()
            cached?.size ?: 0
        }
    }

    fun getCaseDetail(caseId: String): Flow<Result<CaseDetailResponse>> = flow {
        try {
            val response = apiService.getCaseById(caseId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun createCase(url: String, desc: String?, source: String, priority: String, tags: List<String>): Flow<Result<CaseResponse>> = flow {
        try {
            val response = apiService.createCase(CreateCaseRequest(url, desc, source, priority, tags))
            // Insert created case locally
            caseDao.insertCase(
                CaseEntity(
                    id = response.id,
                    caseNumber = response.caseNumber,
                    userId = response.userId,
                    url = response.url,
                    description = response.description,
                    source = response.source,
                    priority = response.priority,
                    status = response.status,
                    tags = response.tags.joinToString(","),
                    createdAt = response.createdAt,
                    updatedAt = response.updatedAt
                )
            )
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun updateCase(caseId: String, status: String?, priority: String?, desc: String?): Flow<Result<CaseResponse>> = flow {
        try {
            val response = apiService.updateCase(caseId, UpdateCaseRequest(status, priority, desc))
            // Update local DB cache
            val local = caseDao.getCaseById(caseId)
            if (local != null) {
                caseDao.updateCase(
                    local.copy(
                        status = response.status,
                        priority = response.priority,
                        description = response.description,
                        updatedAt = response.updatedAt
                    )
                )
            }
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun deleteCase(caseId: String): Flow<Result<MessageResponse>> = flow {
        try {
            val response = apiService.deleteCase(caseId)
            caseDao.deleteCaseById(caseId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getTimeline(caseId: String): Flow<Result<List<TimelineEvent>>> = flow {
        try {
            val response = apiService.getCaseTimeline(caseId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // --- Analysis Actions ---
    fun runAnalysis(caseId: String): Flow<Result<AnalysisResponse>> = flow {
        try {
            val response = apiService.runAnalysis(RunAnalysisRequest(caseId))
            // Refresh local case status to 'Investigating' since analysis updates status
            val local = caseDao.getCaseById(caseId)
            if (local != null) {
                caseDao.updateCase(local.copy(status = "Investigating"))
            }
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getAnalysis(caseId: String): Flow<Result<AnalysisResponse>> = flow {
        try {
            val response = apiService.getAnalysis(caseId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // --- Forensic Reports ---
    fun generateReport(caseId: String): Flow<Result<ReportResponse>> = flow {
        try {
            val response = apiService.generateReport(caseId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getReportsForCase(caseId: String): Flow<Result<List<ReportResponse>>> = flow {
        try {
            val response = apiService.getReportsByCase(caseId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getReportById(reportId: String): Flow<Result<ReportResponse>> = flow {
        try {
            val response = apiService.getReportById(reportId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun verifyReport(reportId: String): Flow<Result<VerifyReportResponse>> = flow {
        try {
            val response = apiService.verifyReport(reportId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    suspend fun downloadReportBytes(reportId: String): ByteArray {
        val response = apiService.downloadReportPdf(reportId)
        return response.bytes()
    }

    // --- Dashboard ---
    fun getStats(): Flow<Result<StatsResponse>> = flow {
        try {
            val response = apiService.getDashboardStats()
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getRecentCases(): Flow<Result<List<CaseResponse>>> = flow {
        try {
            val response = apiService.getRecentCases()
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getThreatMap(): Flow<Result<List<ThreatLocation>>> = flow {
        try {
            val response = apiService.getThreatMap()
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getWeeklyGraph(): Flow<Result<WeeklyDashboardResponse>> = flow {
        try {
            val response = apiService.getWeeklyGraph()
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    // --- Audit & Custody ---
    fun getAuditLogs(): Flow<Result<List<AuditLogResponse>>> = flow {
        try {
            val response = apiService.getAuditLogs()
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    fun getCustodyChain(caseId: String): Flow<Result<List<ChainOfCustodyResponse>>> = flow {
        try {
            val response = apiService.getCustodyChain(caseId)
            emit(Result.success(response))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
