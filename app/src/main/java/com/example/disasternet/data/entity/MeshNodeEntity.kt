package com.example.disasternet.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mesh_nodes")
data class MeshNodeEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val connectionType: String,
    val lastSeen: Long = System.currentTimeMillis(),
    val connected: Boolean = true
)
