package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.model.CropSalinityProfile
import com.example.model.Season
import com.example.model.SoilType
import com.example.model.WaterParameters
import com.example.model.WaterType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiGenerateRequest(
    @Json(name = "contents") val contents: List<GeminiContent>,
    @Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @Json(name = "parts") val parts: List<GeminiPart>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @Json(name = "text") val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @Json(name = "content") val content: GeminiContent? = null
)

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiGenerateRequest
    ): GeminiResponse
}

object GeminiClient {
    private const val TAG = "HydroAI_Gemini"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    suspend fun askWaterAssistant(
        userQuery: String,
        currentTelemetry: WaterParameters?,
        activeClassification: WaterType?,
        activeSoil: SoilType? = null,
        activeCrop: CropSalinityProfile? = null,
        activeSeason: Season? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is missing or placeholder. Utilizing built-in expert knowledge base.")
            return@withContext generateExpertLocalResponse(userQuery, currentTelemetry, activeClassification, activeSoil, activeCrop, activeSeason)
        }

        val systemPrompt = """
            You are Hydro AI, an advanced AI Water Quality, Soil Chemistry & Seasonal Agronomy Expert.
            You must evaluate water parameters, water types (Type A through Type E), and explicitly conclude how the water suits land across ALL FOUR SEASONS:
            
            1. ☀️ Summer / Dry Season: High heat accelerates evaporation, drawing salts upward into topsoil (capillary rise). Explain required leaching fraction, mulching, and avoiding midday leaf spray.
            2. 🌧️ Monsoon / Rainy Season: Natural rainwater dilutes salts and replenishes aquifers, but high turbidity and silt runoff can clog heavy clay pores. Explain silt settling and field drainage.
            3. 🍂 Autumn / Post-Monsoon: Moderate humidity and stable soil moisture. Explain nutrient absorption for post-monsoon crops and soil conditioning.
            4. ❄️ Winter / Dormant Season: Low plant transpiration and cold soil dynamics. Explain reduced watering frequency, avoiding cold soil salt retention, and protecting dormant roots.

            Current State:
            - Water Type: ${activeClassification?.title ?: "Type A: Freshwater"} (${activeClassification?.code ?: "Type A"})
            - Telemetry: pH ${currentTelemetry?.ph ?: "7.2"}, TDS ${currentTelemetry?.tds ?: "320"} ppm, EC ${currentTelemetry?.ec ?: "480"} μS/cm, Turbidity ${currentTelemetry?.turbidity ?: "1.2"} NTU, Hardness ${currentTelemetry?.hardness ?: "140"} mg/L CaCO₃, TOC ${currentTelemetry?.organicCarbon ?: "2.1"} ppm
            - Selected Soil: ${activeSoil?.name ?: "Sandy Loam"} (${activeSoil?.texture ?: "Balanced"})
            - Selected Crop: ${activeCrop?.cropName ?: "Tomato"} (${activeCrop?.sensitivity ?: "Moderately Sensitive"})
            - Active Focus Season: ${activeSeason?.title ?: "All Seasons"}

            Guidelines:
            - When answering questions about seasons, land, soil, farming, or crops, ALWAYS provide clear conclusions for ALL 4 SEASONS (Summer, Monsoon, Autumn, Winter) stating whether the water is suitable for land, what soil precautions are needed, and best crops to plant.
            - Keep language clear, authoritative, and practical for both farmers and homeowners.
        """.trimIndent()

        val request = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(parts = listOf(GeminiPart(text = userQuery)))
            ),
            systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
        )

        try {
            val response = api.generateContent(apiKey, request)
            val reply = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!reply.isNullOrBlank()) {
                reply
            } else {
                generateExpertLocalResponse(userQuery, currentTelemetry, activeClassification, activeSoil, activeCrop, activeSeason)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed: ${e.message}", e)
            generateExpertLocalResponse(userQuery, currentTelemetry, activeClassification, activeSoil, activeCrop, activeSeason)
        }
    }

    private fun generateExpertLocalResponse(
        query: String,
        params: WaterParameters?,
        type: WaterType?,
        soil: SoilType? = null,
        crop: CropSalinityProfile? = null,
        season: Season? = null
    ): String {
        val q = query.lowercase()
        val currentType = type ?: WaterType.TYPE_A
        val soilName = soil?.name ?: "Sandy Loam"
        val cropName = crop?.cropName ?: "Tomato"

        return when {
            q.contains("season") || q.contains("land") || q.contains("summer") || q.contains("monsoon") || q.contains("winter") || q.contains("autumn") || q.contains("year") -> {
                when (currentType) {
                    WaterType.TYPE_A -> """
                        🌾 **All-Season Land Suitability: Freshwater (${currentType.code}) on $soilName**
                        
                        ☀️ **Summer / Dry Season — Highly Suitable**
                        - **Soil Impact**: No salt buildup. High evaporation rate is easily managed because water carries negligible minerals (<500 ppm TDS).
                        - **Land Advice**: Water in early mornings or evenings. Use organic straw mulching to conserve moisture.
                        - **Best Crops**: Strawberries, tomatoes, melons, fruit trees.
                        
                        🌧️ **Monsoon / Rainy Season — Highly Suitable**
                        - **Soil Impact**: Blends naturally with rainwater. Excellent for deep aquifer recharge.
                        - **Land Advice**: Keep field furrows clear of blockages to avoid waterlogging on heavy clay plots.
                        - **Best Crops**: Paddy rice, corn, legumes, leafy greens.
                        
                        🍂 **Autumn / Post-Monsoon — Highly Suitable**
                        - **Soil Impact**: Perfect soil moisture retention and active microbial nutrient recycling.
                        - **Land Advice**: Standard drip or furrow irrigation; zero risk of soil crusting.
                        - **Best Crops**: Wheat, pulses, carrots, root crops.
                        
                        ❄️ **Winter / Cold Season — Highly Suitable**
                        - **Soil Impact**: Cold soil stays clean with no white mineral rings.
                        - **Land Advice**: Cut irrigation volume by 35% to match slower winter crop uptake.
                        - **Best Crops**: Winter wheat, barley, spinach, mustard.
                    """.trimIndent()

                    WaterType.TYPE_B -> """
                        🌾 **All-Season Land Suitability: Brackish Water (${currentType.code}, TDS ~${params?.tds?.toInt() ?: 2650} ppm) on $soilName**
                        
                        ☀️ **Summer / Dry Season — Caution / High Salt Risk**
                        - **Soil Impact**: High heat causes rapid evaporation, drawing dissolved salts straight to topsoil (capillary rise), creating white salt crusts.
                        - **Land Advice**: Apply 25–30% extra water (leaching fraction) to flush salts down below root zones. Never spray directly on plant leaves in hot sun.
                        - **Best Crops**: Date palm, cotton, barley, sugar beet. Avoid sensitive fruits!
                        
                        🌧️ **Monsoon / Rainy Season — Good (Best Season to Irrigate)**
                        - **Soil Impact**: Heavy rains naturally dilute mineral salts and continuously wash them out of topsoil.
                        - **Land Advice**: Irrigate freely between rain showers. Rainwater acts as a natural soil buffer.
                        - **Best Crops**: Cotton, sunflower, millets, tolerant rice.
                        
                        🍂 **Autumn / Post-Monsoon — Caution**
                        - **Soil Impact**: As monsoon showers stop, drying winds can concentrate lingering salts in the upper seedbed.
                        - **Land Advice**: Mix with harvested rainwater or apply agricultural gypsum to preserve soil tilth.
                        - **Best Crops**: Barley, olive trees, sugar beet, soybeans.
                        
                        ❄️ **Winter / Cold Season — Good**
                        - **Soil Impact**: Low evaporation slows down surface salt crusting, keeping minerals dissolved in deeper soil layers.
                        - **Land Advice**: Irrigate deeply with longer intervals between waterings to let cold soil breathe.
                        - **Best Crops**: Winter barley, tolerant wheat, mustard greens.
                    """.trimIndent()

                    WaterType.TYPE_C -> """
                        🌾 **All-Season Land Suitability: Agricultural Runoff (${currentType.code}, Turbidity ~${params?.turbidity ?: 19.4} NTU) on $soilName**
                        
                        ☀️ **Summer / Dry Season — Caution / Algae Risk**
                        - **Soil Impact**: High warmth + high nitrogen & organic carbon triggers green algae slime, which can suffocate soil roots.
                        - **Land Advice**: Pass water through a simple sand or mesh filter. Water under mulch rather than in open flooded pools.
                        - **Best Crops**: Corn / maize, sorghum, fodder grass, sunflowers.
                        
                        🌧️ **Monsoon / Rainy Season — Caution / Mud Runoff**
                        - **Soil Impact**: Extreme muddy turbidity (>20 NTU) deposits dense silt cakes that can seal clay soil pores.
                        - **Land Advice**: Divert runoff into a farm settling silt pond for 24 hours before releasing onto field beds.
                        - **Best Crops**: Paddy rice, cover grasses, vetiver.
                        
                        🍂 **Autumn / Post-Monsoon — Good (Natural Fertilizer)**
                        - **Soil Impact**: Mild autumn temperatures allow beneficial soil microbes to convert organic carbon and nitrates into natural fertilizer.
                        - **Land Advice**: Ideal for feeding hungry autumn cereals and conditioning tired harvest plots.
                        - **Best Crops**: Wheat, mustard, silage sorghum, forage legumes.
                        
                        ❄️ **Winter / Cold Season — Good**
                        - **Soil Impact**: Cool weather prevents fermentation and odor; nitrates stay stable in root zones.
                        - **Land Advice**: Irrigate during midday sun; inspect drip emitters periodically for sediment.
                        - **Best Crops**: Winter cereals, alfalfa, legumes, root vegetables.
                    """.trimIndent()

                    WaterType.TYPE_D -> """
                        ⚠️ **All-Season Land Suitability: Acidic Water (${currentType.code}, pH ~${params?.ph ?: 4.2}) on $soilName**
                        
                        ☀️ **Summer / Dry Season — STRICTLY RESTRICTED**
                        - **Soil Impact**: Hot acid scorches root hairs, destroys earthworms, and releases toxic aluminum into soil water.
                        - **Land Advice**: DO NOT apply directly to land! Dose hydrated lime (Ca(OH)₂) to raise pH above 6.8 before any watering.
                        - **Crops**: None without lime neutralization.
                        
                        🌧️ **Monsoon / Rainy Season — RESTRICTED**
                        - **Soil Impact**: Rainwater dilutes acidity only slightly; acid runoff spreads into neighboring plots, stripping soil calcium.
                        - **Land Advice**: Channel through crushed limestone trenches before allowing into farm soil.
                        - **Crops**: Unsuitable without prior neutralization.
                        
                        🍂 **Autumn / Post-Monsoon — RESTRICTED**
                        - **Soil Impact**: Acidity locks up phosphorus in the soil, stunting autumn crop emergence.
                        - **Land Advice**: Neutralize with lime slurry and incorporate gypsum into topsoil before planting.
                        - **Crops**: Only crops grown on heavily lime-buffered soils.
                        
                        ❄️ **Winter / Cold Season — RESTRICTED**
                        - **Soil Impact**: Acid residues persist in cold, sluggish winter soils without natural bacterial breakdown.
                        - **Land Advice**: Treat with lime to pH 7.0 and aerate before winter irrigation.
                        - **Crops**: Neutralized water only.
                    """.trimIndent()

                    WaterType.TYPE_E -> """
                        ⛔ **All-Season Land Suitability: Seawater / High Salinity (${currentType.code}, TDS ~${params?.tds?.toInt() ?: 34500} ppm) on $soilName**
                        
                        ☀️ **Summer / Dry Season — STRICTLY PROHIBITED**
                        - **Soil Impact**: Extreme osmotic shock: salts pull moisture OUT of plant cells, killing crops within hours and turning land into barren salt-crusted desert.
                        - **Land Advice**: Never irrigate agricultural land with raw seawater. Reverse Osmosis (RO) desalination is mandatory.
                        - **Crops**: Mangroves & coastal halophytes only.
                        
                        🌧️ **Monsoon / Rainy Season — RESTRICTED**
                        - **Soil Impact**: Heavy rain cannot counteract sodium dispersion; soil structure collapses into an airless hardpan.
                        - **Land Advice**: Confine to ocean drainage basins or pass through RO membrane filters.
                        - **Crops**: Coastal Salicornia (glasswort) only.
                        
                        🍂 **Autumn / Post-Monsoon — RESTRICTED**
                        - **Soil Impact**: Marine chlorides cause severe chemical sterilization of beneficial soil fungi and earthworms.
                        - **Land Advice**: Desalination required.
                        - **Crops**: Desalinated water only.
                        
                        ❄️ **Winter / Cold Season — RESTRICTED**
                        - **Soil Impact**: Cold salt brine locks up soil moisture, preventing winter seeds from sprouting.
                        - **Land Advice**: Must be desalinated to <500 ppm TDS before farm or garden application.
                        - **Crops**: Desalinated water only.
                    """.trimIndent()
                }
            }
            q.contains("ph") && (q.contains("low") || q.contains("acid") || q.contains("lime")) -> {
                """
                🧪 **Acidic Water Neutralization Protocol**
                - **Reaction Equation**:
                  $$\text{Ca(OH)}_2 + \text{H}_2\text{SO}_4 \rightarrow \text{CaSO}_4\downarrow + 2\text{H}_2\text{O}$$
                - **Remediation Mechanism**: Dosing Hydrated Lime ($\text{Ca(OH)}_2$) elevates hydronium pH to the target neutral range (6.8–7.6) while precipitating insoluble calcium sulfate (gypsum) and metal hydroxides ($\text{Fe(OH)}_3$).
                - **Dosage Rate**: ${(56.0 * (7.0 - (params?.ph ?: 5.0)).coerceAtLeast(0.5) * 2.2).toInt()} g/m³ feed water.
                - **Operational Tip**: Ensure aeration prior to lime dosing to oxidize dissolved ferrous iron ($\text{Fe}^{2+} \rightarrow \text{Fe}^{3+}$).
                """.trimIndent()
            }
            q.contains("crop") || q.contains("soil") || q.contains("agri") || q.contains("salin") -> {
                val ec_dS = (params?.ec ?: 1200.0) / 1000.0
                """
                🌱 **Agricultural Soil & Crop Salinity Assessment for $soilName**
                - **Current Salinity (ECw)**: $ec_dS dS/m
                - **Target Crop ($cropName)**: Check salinity tolerance and seasonal growth windows.
                - **Sensitive Crops (Strawberries, Almonds, Avocados)**: Threshold ECe < 1.5 dS/m. Using this water directly without dilution risks ${if (ec_dS > 1.5) "15–30% yield loss" else "no acute stress"}.
                - **Tolerant Crops (Wheat, Barley, Cotton)**: Highly resilient; can tolerate up to 6.0–8.0 dS/m.
                - **Leaching Fraction (LF)**: Required LF ≈ ${(ec_dS / (5 * 2.0 - ec_dS) * 100).coerceIn(8.0, 35.0).toInt()}% excess irrigation depth to displace salts beneath root zones.
                - **Soil Strategy**: For Clay soils, blend with gypsum (CaSO₄) to replace exchangeable Na⁺ and prevent sodic soil crusting.
                """.trimIndent()
            }
            q.contains("turbid") || q.contains("alum") || q.contains("filter") -> {
                """
                💧 **Turbidity Remediation & Coagulation**
                - **Active Coagulant Reaction**:
                  Al₂(SO₄)₃·14H₂O + 3Ca(HCO₃)₂ → 2Al(OH)₃↓ + 3CaSO₄ + 14H₂O + 6CO₂
                - **Process**: Trivalent Al³⁺ destabilizes negatively charged colloidal silt and clay flocs.
                - **Recommended Alum Dose**: ${(12.0 + (params?.turbidity ?: 5.0) * 2.0).toInt()} mg/L.
                - **Downstream Polishing**: Follow lamella plate settling with dual-media sand and Granular Activated Carbon (GAC) filtration to achieve < 0.5 NTU.
                """.trimIndent()
            }
            q.contains("ro") || q.contains("desalin") || q.contains("membrane") || q.contains("salt") -> {
                """
                ⚡ **Reverse Osmosis & Desalination Strategy**
                - **Mechanism**: Solution-diffusion through spiral-wound polyamide composite membranes exceeding osmotic pressure (ΔP > Π).
                - **Expected Yield**: ${if ((params?.tds ?: 2000.0) > 10000) "35%–45% (High-Pressure SWRO)" else "70%–80% (BWRO)"}.
                - **Pre-treatment**: Essential to dose phosphonate antiscalant at 3.0–4.5 ppm and maintain Silt Density Index (SDI₁₅ < 3.0) to avoid irreversible membrane compaction.
                - **Energy Metric**: Estimated consumption is ${if ((params?.tds ?: 2000.0) > 10000) "3.4–3.8" else "1.5–1.9"} kWh/m³ permeate.
                """.trimIndent()
            }
            else -> {
                """
                🤖 **Hydro AI All-Season Water Analysis**
                - **Current Classification**: ${currentType.title} (${currentType.typicalYieldRange} yield)
                - **Core Parameters**: pH ${params?.ph ?: 7.2}, TDS ${params?.tds ?: 350.0} ppm, EC ${params?.ec ?: 500.0} μS/cm, Turbidity ${params?.turbidity ?: 1.0} NTU.
                - **Land Suitability Across Seasons**:
                  • ☀️ Summer: ${if (currentType.isDrinkable) "Safe & High Yield" else if (currentType == WaterType.TYPE_B) "Requires 30% Leaching" else "Must Treat Before Soil Use"}
                  • 🌧️ Monsoon: ${if (currentType == WaterType.TYPE_C) "Settle Silt Mud" else if (currentType == WaterType.TYPE_B) "Natural Rain Dilution (Best)" else "Safe Drainage"}
                  • 🍂 Autumn: ${if (currentType == WaterType.TYPE_C) "Nutrient-Rich Fertilizer" else "Balanced Soil Moisture"}
                  • ❄️ Winter: ${if (currentType.isDrinkable || currentType == WaterType.TYPE_B) "Reduced Watering Rate" else "Neutralize / Desalinate"}
                - **Action Plan**: ${currentType.primarySolution}
                """.trimIndent()
            }
        }
    }
}
