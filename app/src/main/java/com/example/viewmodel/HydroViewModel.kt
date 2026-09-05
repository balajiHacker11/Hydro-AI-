package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.HydroDatabase
import com.example.data.local.WaterRepository
import com.example.data.local.WaterSampleEntity
import com.example.ml.WaterMLEngine
import com.example.model.ChatMessage
import com.example.model.CropSalinityProfile
import com.example.model.FacilityTelemetry
import com.example.model.MLClassificationResult
import com.example.model.Season
import com.example.model.SoilType
import com.example.model.WaterParameters
import com.example.model.WaterType
import com.example.network.GeminiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

data class HydroUiState(
    val currentParams: WaterParameters = WaterParameters(),
    val rawInflowLiters: Double = 10000.0,
    val mlResult: MLClassificationResult = WaterMLEngine.classifyAndPredict(WaterParameters(), 10000.0),
    val isLiveMqttStreaming: Boolean = false,
    val selectedSoil: SoilType? = null,
    val selectedCrop: CropSalinityProfile? = null,
    val selectedSeason: Season = Season.SUMMER,
    val facilityTelemetry: FacilityTelemetry = FacilityTelemetry(),
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            isUser = false,
            text = "Hello! I am Hydro AI, your Water & Seasonal Land Assistant. Ask me how your water type suits soil across all seasons (Summer, Monsoon, Autumn, Winter), or about chemical treatment and crop choices."
        )
    ),
    val isAssistantThinking: Boolean = false,
    val currentTab: Int = 0, // 0: Dashboard/Telemetry, 1: ML Classification & Yield, 2: Agri & Soil, 3: Solutions & Tips, 4: AI Assistant
    val notificationMessage: String? = null
)

class HydroViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WaterRepository
    val savedSamples: StateFlow<List<WaterSampleEntity>>

    private val _uiState = MutableStateFlow(HydroUiState())
    val uiState: StateFlow<HydroUiState> = _uiState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        val db = HydroDatabase.getDatabase(application)
        repository = WaterRepository(db.waterSampleDao())
        savedSamples = repository.allSamples.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Set default soil and crop
        _uiState.update { state ->
            state.copy(
                selectedSoil = repository.soilTypes.first(),
                selectedCrop = repository.cropProfiles.first()
            )
        }
    }

    val presetScenarios get() = repository.presetScenarios
    val soilTypes get() = repository.soilTypes
    val cropProfiles get() = repository.cropProfiles

    fun setTab(index: Int) {
        _uiState.update { it.copy(currentTab = index) }
    }

    fun updateParameters(newParams: WaterParameters) {
        val result = WaterMLEngine.classifyAndPredict(newParams, _uiState.value.rawInflowLiters)
        _uiState.update {
            it.copy(
                currentParams = newParams,
                mlResult = result
            )
        }
    }

    fun updateInflowLiters(liters: Double) {
        val clamped = liters.coerceAtLeast(100.0)
        val result = WaterMLEngine.classifyAndPredict(_uiState.value.currentParams, clamped)
        _uiState.update {
            it.copy(
                rawInflowLiters = clamped,
                mlResult = result
            )
        }
    }

    fun loadPreset(params: WaterParameters, name: String) {
        updateParameters(params)
        showNotification("Loaded preset: $name")
    }

    fun selectSoil(soil: SoilType) {
        _uiState.update { it.copy(selectedSoil = soil) }
    }

    fun selectCrop(crop: CropSalinityProfile) {
        _uiState.update { it.copy(selectedCrop = crop) }
    }

    fun selectSeason(season: Season) {
        _uiState.update { it.copy(selectedSeason = season) }
    }

    fun toggleMqttStreaming() {
        val newState = !_uiState.value.isLiveMqttStreaming
        _uiState.update { it.copy(isLiveMqttStreaming = newState) }

        if (newState) {
            startLiveTelemetrySimulation()
            showNotification("MQTT Live Sensor Telemetry Stream Connected")
        } else {
            streamingJob?.cancel()
            streamingJob = null
            showNotification("MQTT Live Sensor Telemetry Paused")
        }
    }

    private fun startLiveTelemetrySimulation() {
        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            while (isActive) {
                delay(2200)
                val current = _uiState.value.currentParams
                // Introduce subtle natural sensor jitter
                val deltaPh = (Random.nextDouble(-0.06, 0.06))
                val deltaTds = (Random.nextDouble(-12.0, 12.0))
                val deltaEc = (Random.nextDouble(-20.0, 20.0))
                val deltaTurb = (Random.nextDouble(-0.15, 0.15))
                val deltaHardness = (Random.nextDouble(-2.0, 2.0))

                val updatedParams = current.copy(
                    ph = (current.ph + deltaPh).coerceIn(1.0, 14.0),
                    tds = (current.tds + deltaTds).coerceAtLeast(10.0),
                    ec = (current.ec + deltaEc).coerceAtLeast(20.0),
                    turbidity = (current.turbidity + deltaTurb).coerceAtLeast(0.1),
                    hardness = (current.hardness + deltaHardness).coerceAtLeast(5.0)
                )

                val telemetry = _uiState.value.facilityTelemetry
                val updatedTelemetry = telemetry.copy(
                    flowRateLitersMin = (telemetry.flowRateLitersMin + Random.nextDouble(-3.0, 3.0)).coerceIn(80.0, 220.0),
                    feedPressureBar = (telemetry.feedPressureBar + Random.nextDouble(-0.1, 0.1)).coerceIn(2.0, 14.0),
                    timestamp = System.currentTimeMillis()
                )

                val mlResult = WaterMLEngine.classifyAndPredict(updatedParams, _uiState.value.rawInflowLiters)

                _uiState.update {
                    it.copy(
                        currentParams = updatedParams,
                        mlResult = mlResult,
                        facilityTelemetry = updatedTelemetry
                    )
                }
            }
        }
    }

    fun saveCurrentSampleToHistory(sourceName: String = "Facility Inlet Sensor") {
        viewModelScope.launch {
            val state = _uiState.value
            val entity = WaterSampleEntity(
                sourceName = sourceName,
                ph = state.currentParams.ph,
                tds = state.currentParams.tds,
                ec = state.currentParams.ec,
                turbidity = state.currentParams.turbidity,
                hardness = state.currentParams.hardness,
                organicCarbon = state.currentParams.organicCarbon,
                classifiedType = state.mlResult.waterType.title,
                convertibilityScore = state.mlResult.convertibilityScore,
                yieldPercent = state.mlResult.predictedYieldPercent,
                primarySolution = state.mlResult.waterType.primarySolution
            )
            repository.saveSample(entity)
            showNotification("Water sample saved to local history")
        }
    }

    fun loadSampleFromHistory(entity: WaterSampleEntity) {
        val params = WaterParameters(
            ph = entity.ph,
            tds = entity.tds,
            ec = entity.ec,
            turbidity = entity.turbidity,
            hardness = entity.hardness,
            organicCarbon = entity.organicCarbon
        )
        updateParameters(params)
        showNotification("Loaded: ${entity.sourceName}")
    }

    fun deleteSampleFromHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteSample(id)
            showNotification("Sample deleted")
        }
    }

    fun sendChatMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(isUser = true, text = text.trim())
        _uiState.update {
            it.copy(
                chatMessages = it.chatMessages + userMessage,
                isAssistantThinking = true
            )
        }

        viewModelScope.launch {
            val state = _uiState.value
            val replyText = GeminiClient.askWaterAssistant(
                userQuery = text,
                currentTelemetry = state.currentParams,
                activeClassification = state.mlResult.waterType,
                activeSoil = state.selectedSoil,
                activeCrop = state.selectedCrop,
                activeSeason = state.selectedSeason
            )

            val botMessage = ChatMessage(
                isUser = false,
                text = replyText,
                relatedType = state.mlResult.waterType
            )

            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + botMessage,
                    isAssistantThinking = false
                )
            }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    private fun showNotification(msg: String) {
        _uiState.update { it.copy(notificationMessage = msg) }
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
    }
}
