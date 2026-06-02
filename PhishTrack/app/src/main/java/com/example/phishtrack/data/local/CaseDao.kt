package com.example.phishtrack.data.local

import androidx.room.*
import com.example.phishtrack.data.local.entities.CaseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {
  @Query("SELECT * FROM cases ORDER BY createdAt DESC")
  fun getAllCasesFlow(): Flow<List<CaseEntity>>

  @Query("SELECT * FROM cases WHERE id = :id")
  suspend fun getCaseById(id: String): CaseEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCases(cases: List<CaseEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertCase(caseEntity: CaseEntity)

  @Update
  suspend fun updateCase(caseEntity: CaseEntity)

  @Query("DELETE FROM cases WHERE id = :id")
  suspend fun deleteCaseById(id: String)

  @Query("DELETE FROM cases")
  suspend fun clearAllCases()
}
