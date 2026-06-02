package com.example.phishtrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.phishtrack.data.local.entities.CaseEntity

@Database(entities = [CaseEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
  abstract fun caseDao(): CaseDao
}
