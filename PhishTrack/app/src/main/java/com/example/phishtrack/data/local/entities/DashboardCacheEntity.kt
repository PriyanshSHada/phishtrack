package com.example.phishtrack.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dashboard_cache")
data class DashboardCacheEntity(
    @PrimaryKey val id: String, // "stats", "threat_map", "weekly_graph"
    val jsonPayload: String,
    val lastUpdated: Long
)
