package com.example.disasternet.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.disasternet.data.dao.AlertDao
import com.example.disasternet.data.dao.EmergencyDao
import com.example.disasternet.data.dao.MeshNodeDao
import com.example.disasternet.data.entity.AlertEntity
import com.example.disasternet.data.entity.EmergencyMessageEntity
import com.example.disasternet.data.entity.MeshNodeEntity

@Database(
    entities = [
        EmergencyMessageEntity::class,
        AlertEntity::class,
        MeshNodeEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun emergencyDao(): EmergencyDao
    abstract fun alertDao(): AlertDao
    abstract fun meshNodeDao(): MeshNodeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "disasternet.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
