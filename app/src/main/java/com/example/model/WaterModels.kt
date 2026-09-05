package com.example.model

data class WaterParameters(
    val ph: Double = 7.2,
    val tds: Double = 320.0,            // ppm (mg/L)
    val ec: Double = 480.0,             // μS/cm
    val turbidity: Double = 1.2,        // NTU
    val hardness: Double = 140.0,       // mg/L CaCO3
    val organicCarbon: Double = 2.1     // ppm (TOC / Chloramines)
) {
    // Water Quality Index calculation (0 to 100)
    val qualityScore: Int
        get() {
            var score = 100.0
            // pH penalty (optimal 6.8 - 7.6)
            if (ph < 6.5) score -= (6.5 - ph) * 18.0
            else if (ph > 8.5) score -= (ph - 8.5) * 16.0

            // TDS penalty
            if (tds > 500.0) score -= ((tds - 500.0) / 100.0).coerceAtMost(35.0)

            // Turbidity penalty
            if (turbidity > 1.0) score -= ((turbidity - 1.0) * 4.0).coerceAtMost(25.0)

            // Hardness penalty
            if (hardness > 200.0) score -= ((hardness - 200.0) / 30.0).coerceAtMost(15.0)

            // Organic penalty
            if (organicCarbon > 4.0) score -= ((organicCarbon - 4.0) * 5.0).coerceAtMost(20.0)

            return score.toInt().coerceIn(8, 100)
        }
}

enum class WaterType(
    val title: String,
    val code: String,
    val criteria: String,
    val primarySolution: String,
    val targetWater: String,
    val typicalYieldRange: String,
    val typicalEnergyKWh: Double,
    val badgeColorHex: Long
) {
    TYPE_A(
        title = "Freshwater / Low-Salinity",
        code = "Type A",
        criteria = "TDS < 500 ppm, pH 6.5–8.5, Turbidity < 5 NTU",
        primarySolution = "UV Sterilization + Multi-stage Sediment Filter",
        targetWater = "Potable Drinking Water (WHO Standards)",
        typicalYieldRange = "95% – 98%",
        typicalEnergyKWh = 0.45,
        badgeColorHex = 0xFF00B4D8
    ),
    TYPE_B(
        title = "Brackish Water",
        code = "Type B",
        criteria = "TDS 1,000–5,000 ppm, High Hardness (>200 mg/L)",
        primarySolution = "Reverse Osmosis (RO) + Ion-Exchange Water Softening",
        targetWater = "Irrigation / Municipal Potable Water",
        typicalYieldRange = "70% – 80%",
        typicalEnergyKWh = 1.85,
        badgeColorHex = 0xFF48CAE4
    ),
    TYPE_C(
        title = "Agricultural Runoff",
        code = "Type C",
        criteria = "High Nitrates, High Turbidity (>5 NTU), Elevated TOC",
        primarySolution = "Coagulation (Alum) + Flocculation + Active Carbon Filter",
        targetWater = "Reusable Agricultural Irrigation Water",
        typicalYieldRange = "85% – 90%",
        typicalEnergyKWh = 1.15,
        badgeColorHex = 0xFF2A9D8F
    ),
    TYPE_D(
        title = "Acidic Mine Groundwater",
        code = "Type D",
        criteria = "pH < 6.0, High Heavy Metal Ions, High EC",
        primarySolution = "Hydrated Lime (Ca(OH)₂) Neutralization + Aeration Clarifier",
        targetWater = "Industrial Process Water / Cooling Towers",
        typicalYieldRange = "90% Fixed Yield",
        typicalEnergyKWh = 1.40,
        badgeColorHex = 0xFFE76F51
    ),
    TYPE_E(
        title = "Highly Saline / Seawater",
        code = "Type E",
        criteria = "TDS > 10,000 ppm, High EC (>15,000 μS/cm)",
        primarySolution = "High-Pressure SWRO Desalination + Energy Recovery Device",
        targetWater = "General Utility & Municipal Distribution",
        typicalYieldRange = "35% – 45%",
        typicalEnergyKWh = 3.65,
        badgeColorHex = 0xFF3F37C9
    );

    val isDrinkable: Boolean
        get() = this == TYPE_A

    val drinkable: Boolean
        get() = this == TYPE_A
}

enum class Season(
    val title: String,
    val iconEmoji: String,
    val shortName: String,
    val climateTrait: String
) {
    SUMMER("Summer / Dry Season", "☀️", "Summer", "High heat, rapid evaporation & salt build-up"),
    MONSOON("Monsoon / Rainy Season", "🌧️", "Monsoon", "Heavy rainfall, high silt runoff & soil dilution"),
    AUTUMN("Autumn / Post-Monsoon", "🍂", "Autumn", "Moderate humidity, stable soil moisture & harvest"),
    WINTER("Winter / Dormant Season", "❄️", "Winter", "Cool weather, low root uptake & slow evaporation")
}

enum class LandSuitabilityRating(
    val label: String,
    val colorHex: Long,
    val isSafeForLand: Boolean
) {
    EXCELLENT("Highly Suitable for Land", 0xFF2A9D8F, true),
    GOOD("Good for Land", 0xFF00B4D8, true),
    CAUTION("Caution / Needs Treatment", 0xFFE9C46A, false),
    RESTRICTED("Unsafe for Land (Treat First)", 0xFFE76F51, false)
}

data class SeasonLandSuitability(
    val season: Season,
    val rating: LandSuitabilityRating,
    val headline: String,
    val soilImpact: String,
    val irrigationGuide: String,
    val recommendedCrops: List<String>,
    val evaporationRisk: String,
    val extraWateringNeededPercent: Int
)

data class ChemicalReactionDetail(
    val title: String,
    val equation: String,
    val reactants: String,
    val products: String,
    val mechanism: String,
    val residueHandling: String
)

data class DosageRecommendation(
    val chemicalName: String,
    val purpose: String,
    val dosageRate: String,
    val unit: String,
    val storageSpec: String
)

data class MLClassificationResult(
    val waterType: WaterType,
    val confidence: Float,
    val convertibilityScore: Int,
    val predictedYieldPercent: Double,
    val treatableVolumeLiters: Double,
    val energyRequirementKWh: Double,
    val zScores: Map<String, Double>,
    val probabilities: Map<WaterType, Float>,
    val chemicalReactions: List<ChemicalReactionDetail>,
    val dosages: List<DosageRecommendation>,
    val remediationSteps: List<String>,
    val seasonalSuitabilities: List<SeasonLandSuitability> = emptyList()
)

data class SoilType(
    val id: String,
    val name: String,
    val texture: String,
    val infiltrationRate: String,
    val saltVulnerability: String,
    val recommendation: String,
    val drainageFactor: Double
)

data class CropSalinityProfile(
    val cropName: String,
    val cropCategory: String,
    val thresholdECE: Double, // dS/m
    val sensitivity: String,  // Sensitive, Moderately Sensitive, Moderately Tolerant, Tolerant
    val optimalPhRange: String,
    val maxYieldPotentialPPM: Double
) {
    fun calculateYieldPotential(waterEC_uS: Double, soilECe: Double): Pair<Double, Double> {
        val waterEC_dS = waterEC_uS / 1000.0
        val effectiveEC = (soilECe * 0.6) + (waterEC_dS * 0.4)
        val yieldPercent = if (effectiveEC <= thresholdECE) {
            100.0
        } else {
            val slope = when (sensitivity) {
                "Sensitive" -> 16.0
                "Moderately Sensitive" -> 10.0
                "Moderately Tolerant" -> 6.5
                else -> 4.0
            }
            (100.0 - (effectiveEC - thresholdECE) * slope).coerceIn(15.0, 100.0)
        }
        // Leaching Fraction LF = ECw / (5*ECe - ECw)
        val denom = (5.0 * thresholdECE) - waterEC_dS
        val leachingFraction = if (denom > 0.1) {
            ((waterEC_dS / denom) * 100.0).coerceIn(5.0, 45.0)
        } else {
            35.0
        }
        return Pair(yieldPercent, leachingFraction)
    }
}

data class FacilityTelemetry(
    val timestamp: Long = System.currentTimeMillis(),
    val flowRateLitersMin: Double = 142.5,
    val feedPressureBar: Double = 4.8,
    val membraneFoulingIndex: Int = 18,        // %
    val differentialPressurePsi: Double = 1.4,
    val filterBackwashHoursLeft: Int = 46,
    val alumStockPercent: Int = 82,
    val limeStockPercent: Int = 74,
    val antiscalantStockPercent: Int = 91,
    val activeAlarmCount: Int = 0
)

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val relatedType: WaterType? = null
)
