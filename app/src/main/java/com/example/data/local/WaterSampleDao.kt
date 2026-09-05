package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WaterSampleDao {
    @Query("SELECT * FROM water_samples ORDER BY timestamp DESC")
    fun getAllSamples(): Flow<List<WaterSampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSample(sample: WaterSampleEntity): Long

    @Query("DELETE FROM water_samples WHERE id = :id")
    suspend fun deleteSampleById(id: Long)

    @Query("DELETE FROM water_samples")
    suspend fun deleteAllSamples()
}
