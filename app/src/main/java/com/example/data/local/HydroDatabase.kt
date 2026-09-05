package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WaterSampleEntity::class], version = 1, exportSchema = false)
abstract class HydroDatabase : RoomDatabase() {
    abstract fun waterSampleDao(): WaterSampleDao

    companion object {
        @Volatile
        private var INSTANCE: HydroDatabase? = null

        fun getDatabase(context: Context): HydroDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HydroDatabase::class.java,
                    "hydro_ai_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
