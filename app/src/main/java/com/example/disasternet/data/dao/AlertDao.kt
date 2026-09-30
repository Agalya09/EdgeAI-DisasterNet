package com.example.disasternet.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.disasternet.data.entity.AlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: AlertEntity)

    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun getAllFlow(): Flow<List<AlertEntity>>

    @Query("DELETE FROM alerts WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM alerts")
    suspend fun clearAll()
}
