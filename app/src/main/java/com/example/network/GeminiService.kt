package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.model.AnomalyType
import com.example.model.CropSalinityProfile
import com.example.model.Season
import com.example.model.SoilType
import com.example.model.WaterParameters
import com.example.model.WaterStation
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

enum class AssistantLanguage(
    val displayName: String,
    val flagEmoji: String,
    val speechLocaleTag: String,
    val ttsLanguageCode: String,
    val ttsCountryCode: String
) {
    ENGLISH("English", "🇬🇧", "en-US", "en", "US"),
    TAMIL("தமிழ்", "🇮🇳", "ta-IN", "ta", "IN")
}

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
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
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
        .connectTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
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
        language: AssistantLanguage = AssistantLanguage.ENGLISH,
        currentStation: WaterStation? = null,
        currentTelemetry: WaterParameters? = null,
        activeClassification: WaterType? = null,
        activeSoil: SoilType? = null,
        activeCrop: CropSalinityProfile? = null,
        activeSeason: Season? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "API Key is missing or placeholder. Using comprehensive local expert knowledge base ($language).")
            return@withContext generateExpertLocalResponse(
                query = userQuery,
                language = language,
                station = currentStation,
                params = currentTelemetry,
                type = activeClassification,
                soil = activeSoil,
                crop = activeCrop,
                season = activeSeason
            )
        }

        val systemPrompt = if (language == AssistantLanguage.TAMIL) {
            """
            நீங்கள் 'ஹைட்ரோ AI' (Hydro AI) - நிலத்தடி நீர், DWLR தானியங்கி பைசோமீட்டர் சென்சார்கள், நீர் தரம், மண் வேதியியல் மற்றும் 4 பருவ கால வேளாண்மைக்கான நிபுணத்துவ செயற்கை நுண்ணறிவு உதவியாளர்.
            
            நீங்கள் பயனரின் கேள்விகளுக்கு எப்போதும் தூய, தெளிவான, நட்புணர்வு கொண்ட தமிழ் மொழியில் விரிவாக பதிலளிக்க வேண்டும்.
            
            தற்போதைய கண்காணிப்பு நிலை விவரங்கள்:
            - தேர்ந்தெடுக்கப்பட்ட நிலையம்: ${currentStation?.name ?: "யமுனை வெள்ளப் பகுதி DWLR-01"} (${currentStation?.code ?: "CGWB-DL-042"})
            - நிலத்தடி நீர் ஆழம்: ${currentStation?.currentWaterLevel ?: 14.8} m bgl (நில மட்டத்திற்கு கீழ் மீட்டர்)
            - சென்சார் நிலை: ${currentStation?.health?.label ?: "இயல்பானது / Operational"}
            - நீர் வகை: ${activeClassification?.title ?: "Type A: நன்னீர்"}
            - நீர் அளவுருக்கள்: pH ${currentTelemetry?.ph ?: "7.2"}, TDS ${currentTelemetry?.tds ?: "320"} ppm, EC ${currentTelemetry?.ec ?: "480"} μS/cm, கலங்கள்/Turbidity ${currentTelemetry?.turbidity ?: "1.2"} NTU, கடினத்தன்மை ${currentTelemetry?.hardness ?: "140"} mg/L.
            - தேர்ந்தெடுக்கப்பட்ட மண்: ${activeSoil?.name ?: "வண்டல் மண் / Alluvium"}
            - தேர்ந்தெடுக்கப்பட்ட பயிர்: ${activeCrop?.cropName ?: "நெல் / தக்காளி"}
            - பருவ காலம்: ${activeSeason?.title ?: "அனைத்து பருவங்கள்"}

            வழிகாட்டுதல்கள்:
            1. பருவ காலம் மற்றும் மண் பற்றிய கேள்விகளுக்கு 4 பருவ காலங்களையும் (கோடைக்காலம் ☀️, மழைக்காலம் 🌧️, இலையுதிர் காலம் 🍂, குளிர்காலம் ❄️) தெளிவாக விளக்கி, மண்ணில் உவர் மேலெழும்புதல், பாசன வழிகாட்டுதல் மற்றும் ஏற்ற பயிர்களைப் பட்டியலிடுங்கள்.
            2. சென்சார் பிழைகள் (Stuck flatline, Drift, Spike, Low battery) பற்றி கேட்கப்பட்டால், பிழைக்கான காரணம் மற்றும் கள பராமரிப்பு (Field crew dispatch, Well purging, Sensor recalibration) வழிமுறைகளைத் தெளிவாகத் தமிழில் கூறுங்கள்.
            3. குடிநீர் பாதுகாப்பு (Drinking water safety) பற்றி கேட்கப்பட்டால், TDS மற்றும் pH அளவுகளின் அடிப்படையில் குடிக்க உகந்ததா என்பதையும், சுத்திகரிப்பு (RO, UV, கொதிக்க வைத்தல்) ஆலோசனைகளையும் வழங்குங்கள்.
            4. பதில்களை அழகிய தலைப்புகள், புல்லட் பாயிண்டுகள் மற்றும் எமோஜிகளுடன் படிக்க எளிதாக அமைத்துத் தமிழில் வழங்குங்கள்.
            """.trimIndent()
        } else {
            """
            You are 'Hydro AI', a senior hydrogeology, DWLR piezometer telemetry, water chemistry, and seasonal agronomy AI Assistant.
            You help water engineers, field technicians, farmers, and citizens monitor groundwater levels, resolve telemetry sensor anomalies, sanitize corrupted data, assess drinking potability, and plan seasonal irrigation.

            Current State Context:
            - Active Station: ${currentStation?.name ?: "Yamuna Floodplain DWLR-01"} (${currentStation?.code ?: "CGWB-DL-042"})
            - Current Water Level: ${currentStation?.currentWaterLevel ?: 14.8} m bgl (meters below ground level)
            - Sensor Health: ${currentStation?.health?.label ?: "Operational"}
            - Installed Sensor: ${currentStation?.installedSensor?.label ?: "Pressure Transducer"}
            - Water Classification: ${activeClassification?.title ?: "Type A: Freshwater"}
            - Water Telemetry: pH ${currentTelemetry?.ph ?: "7.2"}, TDS ${currentTelemetry?.tds ?: "320"} ppm, EC ${currentTelemetry?.ec ?: "480"} μS/cm, Turbidity ${currentTelemetry?.turbidity ?: "1.2"} NTU, Hardness ${currentTelemetry?.hardness ?: "140"} mg/L.
            - Soil: ${activeSoil?.name ?: "Sandy Loam"} | Crop: ${activeCrop?.cropName ?: "Paddy/Tomato"}
            - Season: ${activeSeason?.title ?: "All 4 Seasons"}

            Guidelines:
            - Provide clear, highly actionable, authoritative answers.
            - When answering questions about land, soil, farming, or seasons, conclude for ALL 4 SEASONS: Summer ☀️ (capillary salt rise, leaching fraction), Monsoon 🌧️ (rain dilution, silt settling), Autumn 🍂 (moisture stability, soil conditioning), and Winter ❄️ (dormant root uptake, cut watering by 35%).
            - For sensor anomalies, provide root causes (sediment fouling, zero-drift, ADC step shifts, transient RF noise) and field troubleshooting steps.
            - For drinking water, reference WHO/BIS standards (optimal TDS 150-300 ppm, safe pH 6.5-8.5).
            - Use structured Markdown formatting with clear bullet points.
            """.trimIndent()
        }

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
                generateExpertLocalResponse(userQuery, language, currentStation, currentTelemetry, activeClassification, activeSoil, activeCrop, activeSeason)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API call failed: ${e.message}. Falling back to rich local expert knowledge base.", e)
            generateExpertLocalResponse(userQuery, language, currentStation, currentTelemetry, activeClassification, activeSoil, activeCrop, activeSeason)
        }
    }

    private fun generateExpertLocalResponse(
        query: String,
        language: AssistantLanguage,
        station: WaterStation?,
        params: WaterParameters?,
        type: WaterType?,
        soil: SoilType?,
        crop: CropSalinityProfile?,
        season: Season?
    ): String {
        val q = query.lowercase()
        val currentType = type ?: WaterType.TYPE_A
        val stationName = station?.name ?: "Yamuna Floodplain DWLR-01"
        val stationCode = station?.code ?: "CGWB-DL-042"
        val waterLevel = station?.currentWaterLevel ?: 14.85
        val soilName = soil?.name ?: "Sandy Loam (வண்டல் மண்)"
        val cropName = crop?.cropName ?: "Paddy / Tomato (நெல் / தக்காளி)"

        if (language == AssistantLanguage.TAMIL) {
            return generateTamilResponse(q, station, stationName, stationCode, waterLevel, params, currentType, soilName, cropName)
        } else {
            return generateEnglishResponse(q, station, stationName, stationCode, waterLevel, params, currentType, soilName, cropName)
        }
    }

    private fun generateTamilResponse(
        q: String,
        station: WaterStation?,
        stationName: String,
        stationCode: String,
        waterLevel: Double,
        params: WaterParameters?,
        type: WaterType,
        soilName: String,
        cropName: String
    ): String {
        return when {
            // Season / Land / Farming queries in Tamil
            q.contains("பருவ") || q.contains("season") || q.contains("நிலம்") || q.contains("மண்") || q.contains("விவசாயம்") || q.contains("கோடை") || q.contains("மழை") -> {
                """
                🌾 **4 பருவ காலங்களுக்கும் நிலத்தடி நீரின் பொருத்தம் & மண் பாதுகாப்பு வழிகாட்டி ($stationName)**

                தற்போதைய நீர் வகை: **${type.title}** | மண் வகை: **$soilName**

                ☀️ **1. கோடைக்காலம் (Summer / Dry Season):**
                - **மண் தாக்கம்:** கடுமையான வெயிலால் நீர் விரைவாக ஆவியாகிறது. இதனால் நிலத்தடி நீரிலுள்ள தாது உப்புக்கள் மண்ணின் மேற்பரப்பிற்கு மேல் எழும்பி (Capillary Rise) வெள்ளை உப்புப் படலத்தை உருவாக்கும்.
                - **பாசன வழிகாட்டி:** அதிகாலை அல்லது மாலை வேளையில் மட்டுமே நீர் பாய்ச்சவும். 25–30% கூடுதல் நீரை (Leaching Fraction) வழங்கி உப்பை ஆழமாக கீழ்நோக்கி வடிய வைக்கவும். வைக்கோல் அல்லது இலை மூடாக்கு (Mulching) இடவும்.
                - **ஏற்ற பயிர்கள்:** பேரீச்சை, பருத்தி, தர்பூசணி, கம்பு, கேழ்வரகு.

                🌧️ **2. மழைக்காலம் (Monsoon / Rainy Season):**
                - **மண் தாக்கம்:** இயற்கையான மழைநீர் நிலத்தடி நீர்மட்டத்தை உயர்த்துகிறது மற்றும் உப்புத்தன்மையை நீர்த்துப்போகச் செய்கிறது (Natural Dilution). ஆயினும் வண்டல் மற்றும் கலங்கள் மண்ணின் நுண்துளைகளை அடைக்கலாம்.
                - **பாசன வழிகாட்டி:** மழை இல்லாத இடைவெளிகளில் மட்டும் அளவாக பாசனம் செய்யவும். களிமண் நிலங்களில் நீர் தேங்காமல் இருக்க வடிகால் வாய்க்கால்களை வெட்டவும்.
                - **ஏற்ற பயிர்கள்:** நெல் (Paddy), சோளம், கரும்பு, காய்கறிகள்.

                🍂 **3. இலையுதிர் காலம் (Autumn / Post-Monsoon):**
                - **மண் தாக்கம்:** மண்ணில் நிலையான ஈரப்பதம் நிலவும். நுண்ணுயிர்கள் கரிமப் பொருட்களை மட்கச் செய்து இயற்கை உர ஊட்டச்சத்தை அதிகரிக்கும்.
                - **பாசன வழிகாட்டி:** சொட்டு நீர்ப்பாசனம் (Drip Irrigation) மிகச் சிறந்தது. நிலத்தில் ஜிப்சம் சேர்த்து உவர் தன்மையை கட்டுப்படுத்தலாம்.
                - **ஏற்ற பயிர்கள்:** கோதுமை, பருப்பு வகைகள், காரட், எண்ணெய் வித்துக்கள்.

                ❄️ **4. குளிர்காலம் (Winter / Dormant Season):**
                - **மண் தாக்கம்:** பயிர்களின் நீராவிப் போக்கு குறைவு. குளிர்ந்த மண்ணில் உப்புகள் கரையாமல் தேங்கும் அபாயம் குறைவு.
                - **பாசன வழிகாட்டி:** பாசன அளவை 30-40% குறைக்கவும். வேர்களுக்கு காற்றோட்டம் கிடைக்க நீண்ட இடைவெளியில் ஆழமாக நீர் பாய்ச்சவும்.
                - **ஏற்ற பயிர்கள்:** கடுகு, பார்லி, கீரை வகைகள், கொண்டைக்கடலை.
                """.trimIndent()
            }

            // Sensor Anomaly / Stuck / Flatline / Trouble queries in Tamil
            q.contains("சென்சார்") || q.contains("பிழை") || q.contains("stuck") || q.contains("flatline") || q.contains("drift") || q.contains("error") || q.contains("anomaly") || q.contains("சரிசெய்") -> {
                val anomaly = station?.readings?.firstOrNull { it.anomalyType != AnomalyType.NONE }?.anomalyType ?: AnomalyType.STUCK_VALUE
                """
                ⚠️ **சென்சார் பிழை பகுப்பாய்வு மற்றும் சரிசெய்யும் வழிகாட்டுதல்**
                
                📍 **நிலையம்:** $stationName ($stationCode)
                🔴 **கண்டறியப்பட்ட பிழை:** ${anomaly.title} (${anomaly.code})

                🔍 **பிழைக்கான முக்கிய காரணங்கள்:**
                1. **மெக்கானிக்கல் தடை (Mechanical Float Stuck):** பைசோமீட்டர் ஆழ்குழாயில் சேறு அல்லது வண்டல் படிந்து மிதவை அசைவற்ற நிலைக்குச் செல்லுதல்.
                2. **டிரான்ஸ்யூசர் சோர்வு (Transducer Drift):** அழுத்தம் அளவிடும் டையாபிராமில் படிவுகள் அல்லது மின்சார அளவீடு மாறுபாடு.
                3. **மின்னழுத்தக் குறைவு (Low Solar Voltage):** சோலார் பேனல் தூசு அல்லது பேட்டரி வோல்டேஜ் 3.4V க்குக் கீழ் குறைவதால் ஏற்படும் சிக்னல் இழப்பு.
                4. **மின்னலை குறுக்கீடு (Transient Voltage Spike):** மின்னல் அல்லது மோடம் சிக்னல் சத்தத்தால் திடீரென 4 மீட்டருக்கு மேல் எழும் தவறான ரீடிங்.

                🛠️ **சரிசெய்யும் களப் பராமரிப்பு நடவடிக்கைகள்:**
                - **படி 1:** ஆழ்குழாய் கிணற்றை கம்ப்ரசர் கொண்டு தூய்மைப்படுத்தவும் (Air-lifting / Well Casing Purging).
                - **படி 2:** டிரான்ஸ்யூசர் சென்சாரை வெளியே எடுத்து நன்னீரில் கழுவி பூஞ்சை/சேற்றை அகற்றவும்.
                - **படி 3:** ஜீரோ-ஆஃப்செட் (Zero-Offset) அளவீட்டை கைமுறை நீர்மட்ட மானி (Manual Water Level Tape) கொண்டு மறுசீரமைக்கவும்.
                - **படி 4:** சோலார் பேனலைத் துடைத்து, பேட்டரி வோல்டேஜ் 3.7V - 4.2V உள்ளதா என மல்டிமீட்டர் கொண்டு சோதிக்கவும்.
                - **படி 5:** தானியங்கி கள பராமரிப்பு பணி ஆணையை (Field Work Ticket) அனுப்புங்கள்.
                """.trimIndent()
            }

            // Drinking water / Potability / Health in Tamil
            q.contains("குடிநீர்") || q.contains("drink") || q.contains("பாதுகாப்") || q.contains("tds") || q.contains("ph") || q.contains("சுத்தம்") -> {
                val tdsVal = params?.tds ?: 320.0
                val phVal = params?.ph ?: 7.2
                val isDrinkable = tdsVal < 500 && phVal in 6.5..8.5
                """
                💧 **குடிநீர் தரம் மற்றும் ஆரோக்கிய பகுப்பாய்வு அறிக்கை**

                📊 **தற்போதைய அளவீடுகள்:**
                - **TDS (கரைந்த திடப்பொருட்கள்):** ${tdsVal.toInt()} ppm (mg/L)
                - **pH கார/அமிலத்தன்மை:** $phVal
                - **கலங்கள் (Turbidity):** ${params?.turbidity ?: 1.2} NTU
                - **கடினத்தன்மை (Hardness):** ${params?.hardness ?: 140.0} mg/L CaCO₃

                ✅ **குடிநீர் தகுதி முடிவு:**
                ${if (isDrinkable) "இந்த நீர் உலக சுகாதார நிறுவனம் (WHO) மற்றும் BIS 10500 தரநிலைகளின்படி குடிக்க பாதுகாப்பானது!" 
                else "எச்சரிக்கை! இந்த நீரை நேரடியாக குடிக்கக் கூடாது. தகுந்த சுத்திகரிப்பு அவசியமாகும்."}

                📌 **TDS வழிகாட்டுதல்:**
                - **< 300 ppm:** மிகச் சிறந்த குடிநீர் தரம்.
                - **300 - 600 ppm:** நன்று, குடிக்க உகந்தது.
                - **600 - 900 ppm:** மிதமான தரம் (சிறுநீரகக் கோளாறு உள்ளவர்கள் தவிர்க்கலாம்).
                - **> 1200 ppm:** குடிக்க தகுதியற்றது (RO சுத்திகரிப்பு கட்டாயம்).

                💡 **பரிந்துரைக்கப்படும் எளிய வீட்டு சுத்திகரிப்பு முறைகள்:**
                1. பாக்டீரியா மற்றும் வைரஸ்களை அழிக்க நீரை 10 நிமிடங்கள் கொதிக்க வைக்கவும் (Boiling).
                2. கலங்கலை நீக்க 3-அடுக்கு மணல் மற்றும் கார்பன் வடிகட்டியைப் பயன்படுத்தவும்.
                3. TDS 500 ppm-க்கு மேல் இருந்தால் Reverse Osmosis (RO) பயன்படுத்தவும்.
                """.trimIndent()
            }

            // Crops / Agriculture in Tamil
            q.contains("பயிர்") || q.contains("crop") || q.contains("உவர்") || q.contains("உப்பு") -> {
                """
                🌱 **நிலத்தடி நீரின் உப்புத்தன்மைக்கு ஏற்ற சிறந்த பயிர்கள் பட்டியல்**

                நீர் நிலை: **${waterLevel} m bgl** | நீர் வகை: **${type.title}**

                🏆 **1. அதிக உவர் தாங்கும் பயிர்கள் (High Salinity Tolerant):**
                - **பார்லி (Barley)** - மிக அதிக உப்பை தாங்கும் (8.0 dS/m வரை).
                - **பருத்தி (Cotton)** - உவர் மண்ணில் மிகச் சிறந்த மகசூல் தரும்.
                - **பேரீச்சை (Date Palm)** - உப்பு நீரில் நன்கு வளரும்.
                - **கரும்பு & கம்பு (Sugarcane & Pearl Millet)** - மிதமான உப்புத்தன்மையை எளிதில் தாங்கும்.

                🌾 **2. மிதமான உவர் தாங்கும் பயிர்கள் (Moderately Tolerant):**
                - **நெல் (Paddy Rice)** - போதுமான வடிகால் வசதி இருக்கும்போது நன்கு வளரும்.
                - **கோதுமை (Wheat)** - 4.0 dS/m வரை மகசூல் இழப்பு இன்றி வளரும்.
                - **தக்காளி & சோளம் (Tomato & Maize)** - சொட்டு நீர்ப்பாசனத்தில் நல்ல பலன் தரும்.

                ⚠️ **3. உப்புக்கு மிக நுண்ணிய பயிர்கள் (Sensitives - தவிர்க்கவும்):**
                - ஸ்ட்ராபெரி, பாதாம், வெங்காயம், எலுமிச்சை மற்றும் காரட்.
                - இவை உப்பு நீரில் 25-50% மகசூல் இழப்பை சந்திக்கும்.

                💡 **மண் பாதுகாப்பு முறை:** களிமண் நிலங்களில் ஜிப்சம் (Gypsum - CaSO₄) இடுவதன் மூலம் சோடியம் உப்பை நீக்கி நிலத்தின் கடினத்தன்மையை குறைக்கலாம்.
                """.trimIndent()
            }

            // Station status or general query in Tamil
            else -> {
                """
                🤖 **ஹைட்ரோ AI - விரிவான நீர் கண்காணிப்பு சுருக்கம்**

                📍 **நிலையம்:** $stationName ($stationCode)
                🌊 **தற்போதைய நிலத்தடி நீர் ஆழம்:** $waterLevel மீட்டர் (m bgl)
                🛡️ **கண்காணிப்பு நிலை:** ${station?.health?.label ?: "இயல்பானது / Healthy"}
                ⚡ **சென்சார் வகை:** ${station?.installedSensor?.label ?: "Pressure Transducer"}
                🔋 **சோலார் பேட்டரி:** ${station?.batteryVoltage ?: 3.84} V (சிக்னல்: ${station?.signalDbm ?: -72} dBm)

                💡 **நீங்கள் என்னிடம் கேட்கக்கூடிய கேள்விகள்:**
                - "🌾 4 பருவ காலங்களுக்கும் இந்த நீர் எவ்வாறு ஏற்றது?"
                - "⚠️ சென்சார் ஃப்ளாட்லைன் பிழையை சரிசெய்வது எப்படி?"
                - "💧 இந்த நிலையத்தின் நீர் குடிக்க பாதுகாப்பானதா?"
                - "🌱 உவர் நீருக்கு ஏற்ற பயிர்கள் யாவை?"
                - "🛠️ களப் பொறியாளர் பணி ஆணை அனுப்புவது எப்படி?"
                """.trimIndent()
            }
        }
    }

    private fun generateEnglishResponse(
        q: String,
        station: WaterStation?,
        stationName: String,
        stationCode: String,
        waterLevel: Double,
        params: WaterParameters?,
        type: WaterType,
        soilName: String,
        cropName: String
    ): String {
        return when {
            // Season / Land / Farming queries
            q.contains("season") || q.contains("land") || q.contains("summer") || q.contains("monsoon") || q.contains("winter") || q.contains("autumn") || q.contains("year") -> {
                """
                🌾 **All-Season Land Suitability & Soil Agronomy Assessment ($stationName)**

                Active Water Type: **${type.title}** | Soil Profile: **$soilName**

                ☀️ **1. Summer / Dry Season — Caution & Leaching Needed**
                - **Soil Dynamics**: High surface temperatures induce aggressive capillary rise, pulling deep dissolved mineral salts straight into the top 15cm seedbed, creating white alkaline crusts.
                - **Irrigation Strategy**: Apply a 25–30% Leaching Fraction (LF = ECw / [5·ECe - ECw]) to drive salts below active root zones. Irrigate during dawn or twilight. Mulch with crop residue.
                - **Best Crops**: Date palm, cotton, sorghum, melons, sunflower.

                🌧️ **2. Monsoon / Rainy Season — Peak Recharge & Dilution**
                - **Soil Dynamics**: Heavy precipitation naturally dilutes topsoil salinity and charges aquifer tables. However, colloidal silt and sediment can seal heavy clay macropores.
                - **Irrigation Strategy**: Rely primarily on rainwater. Clear field drainage furrows to prevent root hypoxia. Maintain silt-settling farm ponds before canal diversion.
                - **Best Crops**: Paddy rice, maize, sugarcane, forage legumes.

                🍂 **3. Autumn / Post-Monsoon — Stable Moisture & Soil Conditioning**
                - **Soil Dynamics**: Moderate temperatures and stable matric suction allow soil microflora to rapidly mineralize organic nutrients without salt stress.
                - **Irrigation Strategy**: Ideal window for precision drip irrigation. Blend with gypsum (CaSO₄) on sodic plots to displace exchangeable Na⁺ ions.
                - **Best Crops**: Wheat, pulses, carrots, mustard, root crops.

                ❄️ **4. Winter / Cold Season — Reduced Transpiration Rate**
                - **Soil Dynamics**: Cold soils reduce crop osmotic transpiration. Salts remain localized rather than migrating upward.
                - **Irrigation Strategy**: Cut watering volume by 35–40%. Irrigate deeply at longer intervals during midday sun to maintain soil aerated temperatures.
                - **Best Crops**: Winter barley, tolerant wheat varieties, mustard greens, spinach.
                """.trimIndent()
            }

            // Anomaly / Stuck / Flatline / Trouble queries
            q.contains("stuck") || q.contains("flatline") || q.contains("drift") || q.contains("error") || q.contains("anomaly") || q.contains("sensor") || q.contains("spike") -> {
                val anomaly = station?.readings?.firstOrNull { it.anomalyType != AnomalyType.NONE }?.anomalyType ?: AnomalyType.STUCK_VALUE
                """
                ⚠️ **Piezometer Telemetry Diagnostic & Anomaly Triage**

                📍 **Station:** $stationName ($stationCode)
                🔴 **Anomaly Mode:** ${anomaly.title} (${anomaly.code})

                🔍 **Root Cause Identification:**
                1. **Mechanical Float / Transducer Fouling:** Borehole siltation or biological encrustation mechanically immobilizing the pressure sensor diaphragm.
                2. **Zero-Point Calibration Drift:** Piezoresistive crystal strain or thermal decalibration producing persistent monotonic deviation.
                3. **Low Voltage Dropout:** Solar panel shading or LiFePO4 battery voltage dropping below 3.3V causing analog-to-digital converter (ADC) step errors.
                4. **Transient Line Noise:** Modem transmit surge or RF interference causing single-sample >4m spikes.

                🛠️ **Field Maintenance Protocol:**
                - **Step 1:** Perform well purging using compressed air-lifting to clear accumulated casing sediment.
                - **Step 2:** Pull and inspect the submersible transducer cable for physical kinking or water ingress.
                - **Step 3:** Calibrate zero-offset depth using a certified acoustic water-level sounder tape.
                - **Step 4:** Inspect solar battery terminal voltage (nominal 3.7V - 4.2V).
                - **Step 5:** Dispatch a Field Maintenance Ticket to the regional hydrogeology team.
                """.trimIndent()
            }

            // Drinking water / Potability / Health
            q.contains("drink") || q.contains("safe") || q.contains("potab") || q.contains("tds") || q.contains("ph") || q.contains("health") -> {
                val tdsVal = params?.tds ?: 320.0
                val phVal = params?.ph ?: 7.2
                val isDrinkable = tdsVal < 500 && phVal in 6.5..8.5
                """
                💧 **Potable Drinking Water Quality & Safety Analysis**

                📊 **Laboratory & Field Metrics:**
                - **TDS (Total Dissolved Solids):** ${tdsVal.toInt()} ppm (mg/L)
                - **pH Level:** $phVal (Standard: 6.5 – 8.5)
                - **Turbidity:** ${params?.turbidity ?: 1.2} NTU (BIS Limit: < 1.0 NTU)
                - **Total Hardness:** ${params?.hardness ?: 140.0} mg/L CaCO₃

                ✅ **WHO / BIS 10500 Compliance:**
                ${if (isDrinkable) "Compliant: Safe for direct human consumption with standard disinfection!" 
                else "Non-Compliant: Direct raw ingestion is NOT advised without filtration treatment."}

                📌 **TDS Health Guidelines:**
                - **< 300 ppm:** Excellent mineral balance and crisp palatability.
                - **300 – 600 ppm:** Good, fully acceptable for daily drinking.
                - **600 – 900 ppm:** Fair; elevated mineral content may cause scaling in kettles.
                - **> 1200 ppm:** Poor / Unacceptable; requires Reverse Osmosis (RO) desalination.

                💡 **Recommended Remediation:**
                1. Boil for 10 minutes to eliminate microbial pathogens.
                2. Install activated carbon block filtration to eliminate chlorine odor and chloramines.
                3. If TDS exceeds 500 ppm or heavy metals are suspected, install an RO purifier.
                """.trimIndent()
            }

            // Crops / Salinity
            q.contains("crop") || q.contains("salin") || q.contains("agri") || q.contains("plant") -> {
                """
                🌱 **Groundwater Salinity & Crop Selection Guide**

                Water Depth: **${waterLevel} m bgl** | Classification: **${type.title}**

                🏆 **1. Highly Salinity-Tolerant Crops (ECe > 6.0 dS/m):**
                - **Barley & Rye**: Supreme osmotic resilience with minimal yield penalty.
                - **Cotton**: Deep taproots displace salts effectively.
                - **Sugar Beet & Date Palm**: Highly tolerant of saline aquifers.
                - **Pearl Millet (Bajra) & Sorghum**: Ideal for semi-arid arid belts.

                🌾 **2. Moderately Tolerant Crops (ECe 3.0 – 6.0 dS/m):**
                - **Wheat & Triticale**: Thrives under controlled leaching.
                - **Paddy Rice**: Excellent when freshwater dilution flushes ponding basins.
                - **Tomato & Maize**: Best paired with precision drip fertigation.

                ⚠️ **3. Sensitive Crops (Avoid with High-TDS Water):**
                - Strawberries, Almonds, Citrus, Onions, Green Beans.
                - Suffer severe marginal leaf burn and 30%+ yield suppression if ECw > 1.5 dS/m.

                💡 **Soil Care**: Apply agricultural gypsum (CaSO₄) on heavy clay plots to replace exchangeable Na⁺ with Ca²⁺, preventing soil crusting and hardpan formation.
                """.trimIndent()
            }

            // General or station overview
            else -> {
                """
                🤖 **Hydro AI Network Telemetry Summary**

                📍 **Monitoring Node:** $stationName ($stationCode)
                🌊 **Current Water Level:** $waterLevel meters below ground level (m bgl)
                🛡️ **Station Health:** ${station?.health?.label ?: "Operational"}
                ⚡ **Sensor Type:** ${station?.installedSensor?.label ?: "Submersible Pressure Transducer"}
                🔋 **Telemetry Power:** ${station?.batteryVoltage ?: 3.84} V (Signal: ${station?.signalDbm ?: -72} dBm)

                💡 **Ask me anything in English or தமிழ்:**
                - "🌾 How does this water suit land across all 4 seasons?"
                - "⚠️ How to diagnose and fix stuck flatline sensor errors?"
                - "💧 Is water at this station safe to drink?"
                - "🌱 Best crops for saline groundwater & soil care"
                - "🛠️ How to dispatch a field maintenance work order?"
                """.trimIndent()
            }
        }
    }
}
