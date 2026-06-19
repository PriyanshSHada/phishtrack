package com.example.phishtrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.phishtrack.data.local.entities.CaseEntity
import com.example.phishtrack.data.local.entities.DashboardCacheEntity

@Database(entities = [CaseEntity::class, DashboardCacheEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun caseDao(): CaseDao
  abstract fun dashboardDao(): DashboardDao
}
