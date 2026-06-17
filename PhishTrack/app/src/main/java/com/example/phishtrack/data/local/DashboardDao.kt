package com.example.phishtrack.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.phishtrack.data.local.entities.DashboardCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DashboardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(entity: DashboardCacheEntity)

    @Query("SELECT * FROM dashboard_cache WHERE id = :id")
    suspend fun getCacheById(id: String): DashboardCacheEntity?

    @Query("SELECT * FROM dashboard_cache WHERE id = :id")
    fun getCacheFlowById(id: String): Flow<DashboardCacheEntity?>

    @Query("DELETE FROM dashboard_cache")
    suspend fun clearAll()
}
