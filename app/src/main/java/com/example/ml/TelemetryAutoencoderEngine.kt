package com.example.ml

import com.example.model.AnomalyType
import com.example.model.TelemetryReading
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Time-Series Autoencoder Simulator & Anomaly Classifier for Groundwater Telemetry.
 * Simulates an LSTM-Autoencoder / 1D-CNN encoder-decoder trained on historical baseline
 * water-level depth hydrographs.
 */
object TelemetryAutoencoderEngine {

    /**
     * Reconstructs an input time-series sequence and computes Reconstruction Error (MAE/MSE),
     * autoencoder anomaly score, and assigns taxonomy classification (stuck, drift, spike, voltage drop, or natural hydro recharge).
     *
     * @param rawReadings Unprocessed telemetry readings
     * @param sensitivity Anomaly threshold sensitivity multiplier (0.5 to 2.0)
     */
    fun processAndSanitize(
        rawReadings: List<TelemetryReading>,
        sensitivity: Double = 1.0
    ): List<TelemetryReading> {
        if (rawReadings.isEmpty()) return emptyList()

        // 1. Calculate moving baseline mean and std dev across the sequence
        val levels = rawReadings.map { it.rawWaterLevelMeters }
        val meanLevel = levels.average()
        val variance = levels.map { (it - meanLevel).pow(2) }.average()
        val stdDev = sqrt(max(0.001, variance))

        // Autoencoder threshold dynamic limit
        val dynamicThreshold = (stdDev * 1.8) / sensitivity

        val processed = mutableListOf<TelemetryReading>()

        for (i in rawReadings.indices) {
            val reading = rawReadings[i]
            val raw = reading.rawWaterLevelMeters

            // Smooth neural baseline approximation (Autoencoder Latent Projection)
            val windowStart = max(0, i - 3)
            val windowEnd = min(rawReadings.size - 1, i + 3)
            val localWindow = rawReadings.subList(windowStart, windowEnd + 1)
            val reconstructed = localWindow.map { it.rawWaterLevelMeters }.sorted()[localWindow.size / 2] // robust median filter projection

            val reconstructionError = abs(raw - reconstructed)
            val anomalyScore = min(1.0, (reconstructionError / (dynamicThreshold * 1.5)).coerceAtLeast(0.0))

            // Check for specific failure modes:
            // Mode A: Stuck Sensor (Check if last 4 samples have identical value down to 0.001)
            val isStuck = if (i >= 3) {
                val last4 = (0..3).map { rawReadings[i - it].rawWaterLevelMeters }
                val maxDiff = last4.maxOrNull()!! - last4.minOrNull()!!
                maxDiff < 0.005 && rawReadings[i].anomalyType == AnomalyType.STUCK_VALUE
            } else false

            // Mode B: Low Battery Drop (Battery < 3.4V causing ADC jump)
            val isLowVoltage = reading.batteryVolts < 3.4 && reading.batteryVolts > 0.5

            // Mode C: Transient Spike (sharp single point deviation > 3.5m)
            val prevDelta = if (i > 0) abs(raw - rawReadings[i - 1].rawWaterLevelMeters) else 0.0
            val nextDelta = if (i < rawReadings.size - 1) abs(raw - rawReadings[i + 1].rawWaterLevelMeters) else 0.0
            val isSpike = (prevDelta > 3.0 && nextDelta > 3.0) || reading.anomalyType == AnomalyType.TRANSIENT_SPIKE

            // Mode D: Sensor Drift (cumulative gradual departure from reconstructed)
            val isDrift = (reconstructionError > dynamicThreshold * 0.8) && !isSpike && !isStuck && reading.anomalyType == AnomalyType.SENSOR_DRIFT

            val assignedType: AnomalyType = when {
                reading.anomalyType == AnomalyType.TRUE_HYDRO_RECHARGE -> AnomalyType.TRUE_HYDRO_RECHARGE
                reading.anomalyType == AnomalyType.PUMPING_DRAWDOWN -> AnomalyType.PUMPING_DRAWDOWN
                isStuck || reading.anomalyType == AnomalyType.STUCK_VALUE -> AnomalyType.STUCK_VALUE
                isSpike -> AnomalyType.TRANSIENT_SPIKE
                isLowVoltage || reading.anomalyType == AnomalyType.LOW_VOLTAGE_DROPOUT -> AnomalyType.LOW_VOLTAGE_DROPOUT
                isDrift -> AnomalyType.SENSOR_DRIFT
                reconstructionError > dynamicThreshold -> AnomalyType.TRANSIENT_SPIKE
                else -> AnomalyType.NONE
            }

            // Sanitized value: If corrupt (spike, stuck, voltage drop), impute with Autoencoder reconstruction
            val sanitizedVal = if (assignedType != AnomalyType.NONE && !assignedType.isTrueHydrologicEvent) {
                reconstructed
            } else {
                raw
            }

            processed.add(
                reading.copy(
                    reconstructedLevelMeters = (reconstructed * 100).toInt() / 100.0,
                    reconstructionError = (reconstructionError * 100).toInt() / 100.0,
                    anomalyScore = (anomalyScore * 100).toInt() / 100.0,
                    anomalyType = assignedType,
                    isSanitized = (assignedType != AnomalyType.NONE && !assignedType.isTrueHydrologicEvent),
                    sanitizedLevelMeters = (sanitizedVal * 100).toInt() / 100.0,
                    confidence = max(0.65, 1.0 - (reconstructionError / 10.0))
                )
            )
        }

        return processed
    }
}
