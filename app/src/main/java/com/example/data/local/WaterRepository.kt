package com.example.data.local

import com.example.model.CropSalinityProfile
import com.example.model.SoilType
import com.example.model.WaterParameters
import kotlinx.coroutines.flow.Flow

class WaterRepository(private val dao: WaterSampleDao) {

    val allSamples: Flow<List<WaterSampleEntity>> = dao.getAllSamples()

    suspend fun saveSample(sample: WaterSampleEntity): Long {
        return dao.insertSample(sample)
    }

    suspend fun deleteSample(id: Long) {
        dao.deleteSampleById(id)
    }

    suspend fun clearHistory() {
        dao.deleteAllSamples()
    }

    // Everyday Water Source Presets
    val presetScenarios = listOf(
        Pair("Clean Tap / Well", WaterParameters(
            ph = 7.3,
            tds = 280.0,
            ec = 420.0,
            turbidity = 0.9,
            hardness = 110.0,
            organicCarbon = 1.2
        )),
        Pair("Salty Well Water", WaterParameters(
            ph = 7.8,
            tds = 2650.0,
            ec = 4100.0,
            turbidity = 1.5,
            hardness = 440.0,
            organicCarbon = 2.1
        )),
        Pair("Farm Canal Water", WaterParameters(
            ph = 7.6,
            tds = 1350.0,
            ec = 2100.0,
            turbidity = 19.4,
            hardness = 230.0,
            organicCarbon = 9.2
        )),
        Pair("Acidic Water", WaterParameters(
            ph = 4.2,
            tds = 3900.0,
            ec = 6200.0,
            turbidity = 12.0,
            hardness = 360.0,
            organicCarbon = 3.5
        )),
        Pair("Ocean Seawater", WaterParameters(
            ph = 8.15,
            tds = 34500.0,
            ec = 51200.0,
            turbidity = 2.4,
            hardness = 1420.0,
            organicCarbon = 2.9
        ))
    )

    val soilTypes = listOf(
        SoilType(
            id = "clay",
            name = "Clay / Heavy Soil",
            texture = "Fine particle, dense micropores",
            infiltrationRate = "Slow (1 - 5 mm/hr)",
            saltVulnerability = "High (Prone to sodium compaction and waterlogging)",
            recommendation = "Requires higher leaching fraction, gypsum amendments, and low-salinity water.",
            drainageFactor = 0.55
        ),
        SoilType(
            id = "sandy_loam",
            name = "Sandy Loam",
            texture = "Balanced granular, good aeration",
            infiltrationRate = "Moderate to High (15 - 30 mm/hr)",
            saltVulnerability = "Low-Moderate (Rapid drainage allows effective salt flushing)",
            recommendation = "Ideal for high-efficiency drip fertigation; resilient to moderate salinity.",
            drainageFactor = 0.92
        ),
        SoilType(
            id = "silt_loam",
            name = "Silt Loam",
            texture = "Medium-fine, excellent water retention",
            infiltrationRate = "Medium (8 - 15 mm/hr)",
            saltVulnerability = "Moderate (Can crust under high sodium absorption ratio SAR)",
            recommendation = "Regular subsoil aeration; avoid using water with SAR > 6 without calcium buffering.",
            drainageFactor = 0.78
        ),
        SoilType(
            id = "saline_coastal",
            name = "Saline Coastal Soil",
            texture = "Coarse to fine marine alluvium",
            infiltrationRate = "Variable (5 - 20 mm/hr)",
            saltVulnerability = "Critical (Already saturated with marine chlorides and sulfates)",
            recommendation = "Restrict strictly to halophyte crops (barley, date palm) or apply intensive deep leaching.",
            drainageFactor = 0.65
        ),
        SoilType(
            id = "peat_acidic",
            name = "Peat / Acidic Organic Soil",
            texture = "Spongy, high organic matter",
            infiltrationRate = "High permeability",
            saltVulnerability = "Moderate-Low (High cation exchange capacity CEC)",
            recommendation = "Buffer incoming acidic water with agricultural lime to prevent aluminum phytotoxicity.",
            drainageFactor = 0.85
        )
    )

    val cropProfiles = listOf(
        CropSalinityProfile("Strawberry", "Berry Fruit", 1.0, "Sensitive", "5.5 - 6.5", 450.0),
        CropSalinityProfile("Almond / Orchard", "Tree Nut", 1.5, "Sensitive", "6.0 - 7.5", 600.0),
        CropSalinityProfile("Avocado", "Subtropical Fruit", 1.2, "Sensitive", "6.0 - 7.0", 500.0),
        CropSalinityProfile("Tomato", "Solanaceous Vegetable", 2.5, "Moderately Sensitive", "6.0 - 6.8", 1200.0),
        CropSalinityProfile("Maize / Corn", "Grain Cereal", 1.7, "Moderately Sensitive", "6.0 - 7.2", 850.0),
        CropSalinityProfile("Potato", "Tuber", 1.7, "Moderately Sensitive", "5.2 - 6.4", 900.0),
        CropSalinityProfile("Alfalfa", "Forage Legume", 2.0, "Moderately Sensitive", "6.5 - 7.5", 1100.0),
        CropSalinityProfile("Wheat", "Cereal Grain", 6.0, "Moderately Tolerant", "6.0 - 7.5", 2800.0),
        CropSalinityProfile("Soybean", "Oilseed Legume", 5.0, "Moderately Tolerant", "6.0 - 7.0", 2400.0),
        CropSalinityProfile("Olive", "Mediterranean Tree", 4.5, "Moderately Tolerant", "6.5 - 8.0", 2200.0),
        CropSalinityProfile("Barley", "Grain Cereal", 8.0, "Tolerant", "6.0 - 8.5", 4500.0),
        CropSalinityProfile("Cotton", "Fiber Crop", 7.7, "Tolerant", "6.5 - 8.0", 4200.0),
        CropSalinityProfile("Date Palm", "Desert Fruit", 4.0, "Tolerant", "6.5 - 8.5", 3500.0),
        CropSalinityProfile("Sugar Beet", "Root Crop", 7.0, "Tolerant", "6.5 - 8.0", 3800.0)
    )
}
