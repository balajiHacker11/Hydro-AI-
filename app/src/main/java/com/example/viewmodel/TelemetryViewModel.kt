package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.StationTelemetryRepository
import com.example.ml.TelemetryAutoencoderEngine
import com.example.model.AnomalyType
import com.example.model.MaintenanceTicket
import com.example.model.NetworkHealthSummary
import com.example.model.StationHealth
import com.example.model.TelemetryReading
import com.example.model.TicketPriority
import com.example.model.TicketStatus
import com.example.model.WaterStation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TelemetryUiState(
    val currentTab: Int = 0, // 0: Stations Network, 1: Anomaly Inspector, 2: Sanitizer & QC, 3: Triage Tickets
    val stations: List<WaterStation> = emptyList(),
    val selectedStationId: String = "st-01",
    val searchQuery: String = "",
    val filterHealth: StationHealth? = null,
    val tickets: List<MaintenanceTicket> = emptyList(),
    val isLiveStreaming: Boolean = true,
    val anomalySensitivity: Float = 1.0f, // Sensitivity multiplier (0.5 to 2.0)
    val showCleanedOverlay: Boolean = true,
    val notificationMessage: String? = null,
    val isAutoSanitizeActive: Boolean = true
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

    /**
     * Re-runs the Autoencoder Engine across all stations with updated sensitivity threshold.
     */
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

    /**
     * Dispatches a new field maintenance ticket for the selected station.
     */
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

    /**
     * Periodic live data ping simulation simulating real-time telemetry updates.
     */
    private fun startLiveStreamingSimulation() {
        viewModelScope.launch {
            while (true) {
                delay(6000L) // every 6 seconds
                if (_uiState.value.isLiveStreaming) {
                    val currentStations = _uiState.value.stations
                    if (currentStations.isNotEmpty()) {
                        // slightly jitter live reading for active station to show live dynamics
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
