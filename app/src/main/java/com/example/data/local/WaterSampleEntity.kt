package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "water_samples")
data class WaterSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceName: String,
    val ph: Double,
    val tds: Double,
    val ec: Double,
    val turbidity: Double,
    val hardness: Double,
    val organicCarbon: Double,
    val classifiedType: String,
    val convertibilityScore: Int,
    val yieldPercent: Double,
    val primarySolution: String
)
