package com.example.phishtrack.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cases")
data class CaseEntity(
    @PrimaryKey val id: String,
    val caseNumber: String,
    val userId: String,
    val title: String,
    val url: String,
    val description: String?,
    val source: String,
    val priority: String,
    val status: String,
    val tags: String, // Stored as comma-separated string for simplicity
    val createdAt: String,
    val updatedAt: String
)
