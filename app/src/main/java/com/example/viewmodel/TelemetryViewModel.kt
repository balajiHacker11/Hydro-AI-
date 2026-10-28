package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StationTelemetryRepository
import com.example.ml.TelemetryAutoencoderEngine
import com.example.model.AnomalyType
import com.example.model.ChatMessage
import com.example.model.MaintenanceTicket
import com.example.model.NetworkHealthSummary
import com.example.model.StationHealth
import com.example.model.TelemetryReading
import com.example.model.TicketPriority
import com.example.model.TicketStatus
import com.example.model.WaterParameters
import com.example.model.WaterStation
import com.example.model.WaterType
import com.example.network.AssistantLanguage
import com.example.network.GeminiClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TelemetryUiState(
    val currentTab: Int = 0, // 0: Stations, 1: Anomaly AI, 2: Sanitizer, 3: Triage, 4: AI Voice Assistant
    val stations: List<WaterStation> = emptyList(),
    val selectedStationId: String = "st-01",
    val searchQuery: String = "",
    val filterHealth: StationHealth? = null,
    val tickets: List<MaintenanceTicket> = emptyList(),
    val isLiveStreaming: Boolean = true,
    val anomalySensitivity: Float = 1.0f, // Sensitivity multiplier (0.5 to 2.0)
    val showCleanedOverlay: Boolean = true,
    val notificationMessage: String? = null,
    val isAutoSanitizeActive: Boolean = true,
    // AI Voice Assistant State
    val assistantLanguage: AssistantLanguage = AssistantLanguage.ENGLISH,
    val isAssistantThinking: Boolean = false,
    val isAutoSpeakEnabled: Boolean = false,
    val currentlySpeakingMessageId: String? = null,
    val isVoiceListening: Boolean = false,
    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            isUser = false,
            text = "👋 Hello! I am Hydro AI, your Water Telemetry & Voice Assistant. I can analyze DWLR piezometers, troubleshoot sensor anomalies, assess drinking potability, and evaluate land across all 4 seasons in English & தமிழ்."
        )
    )
) {
    val selectedStation: WaterStation?
        get() = stations.firstOrNull { it.id == selectedStationId } ?: stations.firstOrNull()

    val networkSummary: NetworkHealthSummary
        get() {
            val total = stations.size
            val operational = stations.count { it.health == StationHealth.HEALTHY }
            val anomalous = stations.count { it.health == StationHealth.ANOMALOUS || it.health == StationHealth.DRIFTING }
            val stuck = stations.count { it.health == StationHealth.STUCK }
            val totalReadings = stations.sumOf { it.readings.size }
            val sanitized = stations.sumOf { s -> s.readings.count { it.isSanitized } }
            val rate = if (totalReadings > 0) {
                ((totalReadings - sanitized).toDouble() / totalReadings.toDouble()) * 100.0
            } else 100.0
            return NetworkHealthSummary(
                totalStations = total,
                operationalCount = operational,
                anomalyCount = anomalous,
                stuckSensorsCount = stuck,
                totalReadingsProcessed = totalReadings,
                sanitizedRecordsCount = sanitized,
                networkSanitizationRate = (rate * 10).toInt() / 10.0,
                openTicketsCount = tickets.count { it.status != TicketStatus.RESOLVED }
            )
        }
}

class TelemetryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(TelemetryUiState())
    val uiState: StateFlow<TelemetryUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        startLiveStreamingSimulation()
    }

    private fun loadInitialData() {
        val initialStations = StationTelemetryRepository.getMockStations()
        val initialTickets = StationTelemetryRepository.getInitialTickets()
        _uiState.update {
            it.copy(
                stations = initialStations,
                tickets = initialTickets,
                selectedStationId = initialStations.firstOrNull()?.id ?: ""
            )
        }
    }

    fun setTab(tabIndex: Int) {
        _uiState.update { it.copy(currentTab = tabIndex) }
    }

    fun selectStation(stationId: String) {
        _uiState.update { it.copy(selectedStationId = stationId) }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun filterByHealth(health: StationHealth?) {
        _uiState.update { it.copy(filterHealth = if (it.filterHealth == health) null else health) }
    }

    fun setAnomalySensitivity(value: Float) {
        _uiState.update { it.copy(anomalySensitivity = value) }
        recalculateWithSensitivity(value)
    }

    fun toggleCleanedOverlay() {
        _uiState.update { it.copy(showCleanedOverlay = !it.showCleanedOverlay) }
    }

    fun toggleLiveStreaming() {
        val newState = !_uiState.value.isLiveStreaming
        _uiState.update {
            it.copy(
                isLiveStreaming = newState,
                notificationMessage = if (newState) "Live telemetry streaming connected" else "Streaming paused"
            )
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    fun showNotification(msg: String) {
        _uiState.update { it.copy(notificationMessage = msg) }
    }

    // ==========================================
    // AI Assistant, Voice & Language Operations
    // ==========================================

    fun setAssistantLanguage(lang: AssistantLanguage) {
        if (_uiState.value.assistantLanguage == lang) return
        
        val greeting = if (lang == AssistantLanguage.TAMIL) {
            "வணக்கம்! நான் ஹைட்ரோ AI (Hydro AI), உங்கள் நிலத்தடி நீர் மற்றும் குரல் வழி உதவியாளர். தமிழ் மற்றும் ஆங்கிலத்தில் நிலத்தடி நீர் நிலை, சென்சார் பிழைகள், குடிநீர் தரம், மற்றும் 4 பருவ கால விவசாய ஆலோசனைகளை கேட்கலாம்."
        } else {
            "👋 Hello! I am Hydro AI, your Water Telemetry & Voice Assistant. I can analyze DWLR piezometers, troubleshoot sensor anomalies, assess drinking potability, and evaluate land across all 4 seasons in English & தமிழ்."
        }

        _uiState.update {
            it.copy(
                assistantLanguage = lang,
                chatMessages = listOf(
                    ChatMessage(
                        isUser = false,
                        text = greeting
                    )
                ),
                notificationMessage = if (lang == AssistantLanguage.TAMIL) "மொழி தமிழுக்கு மாற்றப்பட்டது 🇮🇳" else "Language changed to English 🇬🇧"
            )
        }
    }

    fun toggleAutoSpeak() {
        val current = _uiState.value.isAutoSpeakEnabled
        _uiState.update {
            it.copy(
                isAutoSpeakEnabled = !current,
                notificationMessage = if (!current) "🔊 Auto Voice Output Enabled" else "🔇 Auto Voice Output Muted"
            )
        }
    }

    fun setSpeakingMessageId(id: String?) {
        _uiState.update { it.copy(currentlySpeakingMessageId = id) }
    }

    fun setVoiceListening(isListening: Boolean) {
        _uiState.update { it.copy(isVoiceListening = isListening) }
    }

    fun clearChat() {
        val greeting = if (_uiState.value.assistantLanguage == AssistantLanguage.TAMIL) {
            "வணக்கம்! புதிய உரையாடலைத் தொடங்கலாம். உங்கள் கேள்வியைக் கேட்கவும்."
        } else {
            "Hello! Chat history cleared. How can I assist you with your water data today?"
        }
        _uiState.update {
            it.copy(
                chatMessages = listOf(ChatMessage(isUser = false, text = greeting)),
                currentlySpeakingMessageId = null
            )
        }
    }

    fun sendChatMessage(text: String, onResponseReadyToSpeak: ((ChatMessage) -> Unit)? = null) {
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
            val currentStation = state.selectedStation
            val currentParams = WaterParameters(
                ph = 7.2,
                tds = 340.0,
                ec = 510.0,
                turbidity = 1.3,
                hardness = 145.0
            )

            val replyText = GeminiClient.askWaterAssistant(
                userQuery = text,
                language = state.assistantLanguage,
                currentStation = currentStation,
                currentTelemetry = currentParams,
                activeClassification = WaterType.TYPE_A
            )

            val botMessage = ChatMessage(
                isUser = false,
                text = replyText,
                relatedType = WaterType.TYPE_A
            )

            _uiState.update {
                it.copy(
                    chatMessages = it.chatMessages + botMessage,
                    isAssistantThinking = false
                )
            }

            if (state.isAutoSpeakEnabled) {
                onResponseReadyToSpeak?.invoke(botMessage)
            }
        }
    }

    /**
     * Ask AI about current station directly from other screens.
     */
    fun askAboutStation(station: WaterStation) {
        selectStation(station.id)
        setTab(4) // Switch to AI Assistant tab
        val prompt = if (_uiState.value.assistantLanguage == AssistantLanguage.TAMIL) {
            "${station.name} (${station.code}) நிலையத்தின் தற்போதைய நிலத்தடி நீர் நிலை மற்றும் சென்சார் ஆரோக்கியத்தை பகுப்பாய்வு செய்து விளக்குங்கள்."
        } else {
            "Analyze current groundwater level (${station.currentWaterLevel} m bgl) and telemetry health for ${station.name} (${station.code})."
        }
        sendChatMessage(prompt)
    }

    /**
     * Ask AI about specific sensor anomaly.
     */
    fun askAboutAnomaly(station: WaterStation, anomaly: AnomalyType) {
        selectStation(station.id)
        setTab(4)
        val prompt = if (_uiState.value.assistantLanguage == AssistantLanguage.TAMIL) {
            "${station.name} நிலையத்தில் கண்டறியப்பட்ட '${anomaly.title}' (${anomaly.code}) பிழைக்கான காரணங்கள் மற்றும் களத்தில் சரிசெய்யும் வழிகளை விளக்குங்கள்."
        } else {
            "Diagnose anomaly '${anomaly.title}' (${anomaly.code}) at ${station.name} and provide field troubleshooting recommendations."
        }
        sendChatMessage(prompt)
    }

    // ==========================================
    // Telemetry Engine & Maintenance Tickets
    // ==========================================

    private fun recalculateWithSensitivity(sensitivity: Float) {
        val updated = _uiState.value.stations.map { station ->
            val processed = TelemetryAutoencoderEngine.processAndSanitize(
                rawReadings = station.readings,
                sensitivity = sensitivity.toDouble()
            )
            station.copy(
                readings = processed,
                anomalyCount = processed.count { it.isSanitized }
            )
        }
        _uiState.update { it.copy(stations = updated) }
    }

    fun createTicketForStation(station: WaterStation, anomaly: AnomalyType) {
        val newTicket = MaintenanceTicket(
            stationId = station.id,
            stationName = station.name,
            stationCode = station.code,
            anomalyType = anomaly,
            priority = if (anomaly == AnomalyType.STUCK_VALUE) TicketPriority.CRITICAL else TicketPriority.HIGH,
            status = TicketStatus.OPEN,
            createdAt = "Just now",
            diagnosticSummary = "Autoencoder flagged ${anomaly.title} (${anomaly.code}) with high anomaly score. Requires physical probe check.",
            recommendedAction = "Inspect probe depth; clean sediment fouling; recalibrate pressure transducer zero offset.",
            assignedEngineer = "Emergency Field Unit",
            gpsCoordinates = "${station.latitude}° N, ${station.longitude}° E"
        )

        _uiState.update {
            it.copy(
                tickets = listOf(newTicket) + it.tickets,
                notificationMessage = "Maintenance Ticket ${newTicket.id} created & dispatched!"
            )
        }
    }

    fun updateTicketStatus(ticketId: String, newStatus: TicketStatus) {
        _uiState.update { state ->
            val updatedList = state.tickets.map { ticket ->
                if (ticket.id == ticketId) ticket.copy(status = newStatus) else ticket
            }
            state.copy(
                tickets = updatedList,
                notificationMessage = "Ticket status updated to: ${newStatus.label}"
            )
        }
    }

    private fun startLiveStreamingSimulation() {
        viewModelScope.launch {
            while (true) {
                delay(6000L) // every 6 seconds
                if (_uiState.value.isLiveStreaming) {
                    val currentStations = _uiState.value.stations
                    if (currentStations.isNotEmpty()) {
                        val updated = currentStations.map { station ->
                            val readings = station.readings.toMutableList()
                            if (readings.isNotEmpty()) {
                                val last = readings.last()
                                val jitter = ((-2..2).random()) * 0.02
                                val newLevel = if (station.health == StationHealth.STUCK) last.rawWaterLevelMeters else (last.rawWaterLevelMeters + jitter)
                                readings[readings.size - 1] = last.copy(
                                    rawWaterLevelMeters = (newLevel * 100).toInt() / 100.0
                                )
                            }
                            station.copy(
                                lastPingTime = "Just now",
                                currentWaterLevel = readings.lastOrNull()?.rawWaterLevelMeters ?: station.currentWaterLevel,
                                readings = readings
                            )
                        }
                        _uiState.update { it.copy(stations = updated) }
                    }
                }
            }
        }
    }
}
