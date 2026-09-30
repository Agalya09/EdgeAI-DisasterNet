package com.example.disasternet.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.disasternet.data.entity.EmergencyMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmergencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: EmergencyMessageEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrIgnore(entity: EmergencyMessageEntity): Long

    @Update
    suspend fun update(entity: EmergencyMessageEntity)

    @Query("SELECT * FROM emergency_messages WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): EmergencyMessageEntity?

    @Query("SELECT * FROM emergency_messages WHERE id = :id LIMIT 1")
    fun getByIdFlow(id: String): Flow<EmergencyMessageEntity?>

    // Privacy-preserving query: Standard user only ever queries their own messages
    @Query("SELECT * FROM emergency_messages WHERE senderId = :senderId ORDER BY createdAt DESC")
    fun getForSenderFlow(senderId: String): Flow<List<EmergencyMessageEntity>>

    @Query("SELECT * FROM emergency_messages WHERE senderId = :senderId ORDER BY createdAt DESC LIMIT 1")
    fun getLatestForSenderFlow(senderId: String): Flow<EmergencyMessageEntity?>

    @Query("SELECT * FROM emergency_messages WHERE senderId = :senderId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestForSender(senderId: String): EmergencyMessageEntity?

    // Rescuer emergency queue: all incoming emergencies
    @Query("SELECT * FROM emergency_messages ORDER BY priorityScore DESC, createdAt DESC")
    fun getAllPendingFlow(): Flow<List<EmergencyMessageEntity>>

    @Query("SELECT * FROM emergency_messages ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<EmergencyMessageEntity>>

    @Query("UPDATE emergency_messages SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("UPDATE emergency_messages SET status = :status, priorityScore = :score WHERE id = :id")
    suspend fun updateStatusAndPriority(id: String, status: String, score: Double)
}
