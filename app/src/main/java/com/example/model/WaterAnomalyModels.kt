package com.example.model

import java.util.UUID

/**
 * Supported sensor types installed on public Digital Water Level Recorders (DWLRs) / Piezometers.
 */
enum class SensorType(val label: String, val unit: String) {
    PRESSURE_TRANSDUCER("Submersible Pressure Transducer", "m"),
    ULTRASONIC_LEVEL("Acoustic / Ultrasonic Level", "m"),
    RADAR_WATER_LEVEL("Non-contact Radar Gauge", "m"),
    CONDUCTIVITY_PROBE("Integrated EC / Salinity Probe", "μS/cm"),
    TEMPERATURE_PROBE("Subsurface Thermal Sensor", "°C")
}

/**
 * Status of an individual monitoring station node.
 */
enum class StationHealth(val label: String, val badgeColorHex: Long) {
    HEALTHY("Operational", 0xFF10B981),        // Emerald Green
    ANOMALOUS("Anomaly Flagged", 0xFFEF4444),    // Crimson Red
    DRIFTING("Drift Suspected", 0xFFF59E0B),    // Amber Warning
    INTERMITTENT("Intermittent Link", 0xFF6366F1), // Indigo
    STUCK("Flatline / Stuck", 0xFFDC2626)       // Dark Red
}

/**
 * Taxonomy of Anomaly Modes detected by the Autoencoder & Quality Control pipeline.
 */
enum class AnomalyType(
    val title: String,
    val code: String,
    val description: String,
    val colorHex: Long,
    val isTrueHydrologicEvent: Boolean
) {
    NONE(
        "Normal Telemetry",
        "QC_PASS",
        "Reading aligns within expected autoencoder reconstruction confidence ribbon (μ ± 2.5σ).",
        0xFF10B981,
        true
    ),
    STUCK_VALUE(
        "Stuck Sensor / Flatline",
        "QC_ERR_STUCK",
        "Zero variance detected for consecutive intervals. Float mechanically obstructed or transducer frozen by sediment.",
        0xFFDC2626,
        false
    ),
    SENSOR_DRIFT(
        "Sensor Drift & Calibration Bias",
        "QC_ERR_DRIFT",
        "Monotonic deviation from neighborhood reference stations. Diaphragm fatigue, biofouling, or crystal decalibration.",
        0xFFF59E0B,
        false
    ),
    TRANSIENT_SPIKE(
        "Transient Spike / Noise",
        "QC_ERR_SPIKE",
        "Single-sample erratic jump (>4m delta) with immediate recovery. Electrical noise, modem surge, or cable ping error.",
        0xFF8B5CF6,
        false
    ),
    LOW_VOLTAGE_DROPOUT(
        "Low Battery / Power Step",
        "QC_ERR_VOLT",
        "Telemetry dropped below 3.3V battery threshold causing step shifts in AD converter baseline.",
        0xFFEC4899,
        false
    ),
    TRUE_HYDRO_RECHARGE(
        "Natural Flash Recharge",
        "HYDRO_RECHARGE",
        "Sudden rise in groundwater table correlated with regional monsoon rainfall. Natural infiltration event, not an error.",
        0xFF0284C7,
        true
    ),
    PUMPING_DRAWDOWN(
        "Active Pumping Drawdown",
        "HYDRO_PUMP",
        "Sharp drawdown cone due to nearby heavy agricultural irrigation tube-well activation.",
        0xFF0D9488,
        true
    )
}

/**
 * A single timestamped telemetry reading from a DWLR station.
 */
data class TelemetryReading(
    val id: String = UUID.randomUUID().toString(),
    val timestampMillis: Long,
    val timeLabel: String,
    val rawWaterLevelMeters: Double,     // Depth to water level below ground surface (m bgl)
    val batteryVolts: Double,           // Telemetry unit solar battery voltage
    val waterTempCelsius: Double,       // Water temperature
    val reconstructedLevelMeters: Double, // Autoencoder neural baseline reconstruction
    val reconstructionError: Double,    // |raw - reconstructed|
    val anomalyScore: Double,           // 0.0 to 1.0 (Autoencoder anomaly probability)
    val anomalyType: AnomalyType,
    val isSanitized: Boolean = false,
    val sanitizedLevelMeters: Double = rawWaterLevelMeters,
    val confidence: Double = 0.95
)

/**
 * A public water monitoring station (e.g., CGWB / State Groundwater Network).
 */
data class WaterStation(
    val id: String,
    val name: String,
    val code: String,                   // e.g., CGWB-DL-042
    val district: String,               // e.g., Yamuna Basin, New Delhi
    val state: String,                  // e.g., Delhi NCT
    val latitude: Double,
    val longitude: Double,
    val aquiferType: String,            // e.g., Alluvial Sands, Hard-Rock Basalt
    val totalBoreDepthMeters: Double,
    val installedSensor: SensorType,
    val health: StationHealth,
    val lastPingTime: String,
    val currentWaterLevel: Double,
    val baselineNormalDepth: Double,
    val batteryVoltage: Double,
    val signalDbm: Int,
    val totalReadingsCount: Int = 120,
    val anomalyCount: Int = 0,
    val readings: List<TelemetryReading> = emptyList()
)

/**
 * Maintenance Ticket dispatched to field hydrogeologists and instrumentation engineers.
 */
enum class TicketPriority(val label: String, val colorHex: Long) {
    CRITICAL("Immediate Action", 0xFFDC2626),
    HIGH("High Priority", 0xFFEA580C),
    MEDIUM("Medium", 0xFFF59E0B),
    LOW("Routine Inspection", 0xFF3B82F6)
}

enum class TicketStatus(val label: String, val colorHex: Long) {
    OPEN("Open", 0xFFEF4444),
    ASSIGNED("Dispatched to Field", 0xFFF59E0B),
    RESOLVED("Resolved & Recalibrated", 0xFF10B981)
}

data class MaintenanceTicket(
    val id: String = "TKT-" + UUID.randomUUID().toString().take(6).uppercase(),
    val stationId: String,
    val stationName: String,
    val stationCode: String,
    val anomalyType: AnomalyType,
    val priority: TicketPriority,
    val status: TicketStatus = TicketStatus.OPEN,
    val createdAt: String,
    val diagnosticSummary: String,
    val recommendedAction: String,
    val assignedEngineer: String = "Field Unit North-3",
    val gpsCoordinates: String
)

/**
 * Overall Network Quality Control summary.
 */
data class NetworkHealthSummary(
    val totalStations: Int = 12,
    val operationalCount: Int = 8,
    val anomalyCount: Int = 3,
    val stuckSensorsCount: Int = 1,
    val totalReadingsProcessed: Int = 1440,
    val sanitizedRecordsCount: Int = 128,
    val networkSanitizationRate: Double = 98.2, // %
    val openTicketsCount: Int = 4
)
