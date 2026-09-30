package com.example.disasternet.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.disasternet.data.entity.MeshNodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshNodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(node: MeshNodeEntity)

    @Query("UPDATE mesh_nodes SET connected = :connected, lastSeen = :lastSeen WHERE id = :id")
    suspend fun updateConnection(id: String, connected: Boolean, lastSeen: Long)

    @Query("SELECT * FROM mesh_nodes ORDER BY lastSeen DESC")
    fun getAllFlow(): Flow<List<MeshNodeEntity>>

    @Query("SELECT COUNT(*) FROM mesh_nodes WHERE connected = 1")
    fun getConnectedCountFlow(): Flow<Int>

    @Query("DELETE FROM mesh_nodes")
    suspend fun clearAll()
}
