package com.example.disasternet.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val body: String,
    val severity: String,
    val createdAt: Long = System.currentTimeMillis()
)
