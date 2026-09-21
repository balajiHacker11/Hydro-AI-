package com.example.data

import com.example.ml.TelemetryAutoencoderEngine
import com.example.model.AnomalyType
import com.example.model.MaintenanceTicket
import com.example.model.NetworkHealthSummary
import com.example.model.SensorType
import com.example.model.StationHealth
import com.example.model.TelemetryReading
import com.example.model.TicketPriority
import com.example.model.TicketStatus
import com.example.model.WaterStation
import java.util.UUID

object StationTelemetryRepository {

    fun getMockStations(): List<WaterStation> {
        val now = System.currentTimeMillis()
        val hourMs = 3600_000L

        // Station 1: Yamuna Basin DWLR-01 (Flagged with Transient Spike Noise)
        val s1ReadingsRaw = generateSeries(
            baseDepth = 14.5,
            noise = 0.15,
            count = 24,
            injectSpikeIndex = 14,
            spikeDelta = 7.8,
            anomalyTypeToInject = AnomalyType.TRANSIENT_SPIKE
        )
        val s1Readings = TelemetryAutoencoderEngine.processAndSanitize(s1ReadingsRaw)

        // Station 2: South Delhi Piezometer PZ-04 (Stuck Sensor / Flatline)
        val s2ReadingsRaw = generateSeries(
            baseDepth = 32.2,
            noise = 0.0,
            count = 24,
            stuckStart = 8,
            anomalyTypeToInject = AnomalyType.STUCK_VALUE
        )
        val s2Readings = TelemetryAutoencoderEngine.processAndSanitize(s2ReadingsRaw)

        // Station 3: Hindon Alluvium DWLR-09 (Sensor Drift - Pressure Transducer decalibration)
        val s3ReadingsRaw = generateSeries(
            baseDepth = 21.0,
            noise = 0.1,
            count = 24,
            driftSlope = 0.25,
            anomalyTypeToInject = AnomalyType.SENSOR_DRIFT
        )
        val s3Readings = TelemetryAutoencoderEngine.processAndSanitize(s3ReadingsRaw)

        // Station 4: Okhla Barrage Monitoring Node (Healthy / Normal)
        val s4ReadingsRaw = generateSeries(
            baseDepth = 8.4,
            noise = 0.08,
            count = 24
        )
        val s4Readings = TelemetryAutoencoderEngine.processAndSanitize(s4ReadingsRaw)

        // Station 5: Mehrauli Deep Ridge Piezometer (Flash Monsoon Groundwater Recharge)
        val s5ReadingsRaw = generateSeries(
            baseDepth = 48.0,
            noise = 0.12,
            count = 24,
            rechargeStepIndex = 10,
            rechargeRise = -5.4, // water level rises (depth to water decreases)
            anomalyTypeToInject = AnomalyType.TRUE_HYDRO_RECHARGE
        )
        val s5Readings = TelemetryAutoencoderEngine.processAndSanitize(s5ReadingsRaw)

        // Station 6: Najafgarh Drain Piezometer (Low Battery Dropout)
        val s6ReadingsRaw = generateSeries(
            baseDepth = 18.2,
            noise = 0.14,
            count = 24,
            lowBatteryStart = 16,
            anomalyTypeToInject = AnomalyType.LOW_VOLTAGE_DROPOUT
        )
        val s6Readings = TelemetryAutoencoderEngine.processAndSanitize(s6ReadingsRaw)

        return listOf(
            WaterStation(
                id = "st-01",
                name = "Yamuna Floodplain DWLR-01",
                code = "CGWB-DL-YAM01",
                district = "East Delhi",
                state = "Delhi NCT",
                latitude = 28.6139,
                longitude = 77.2090,
                aquiferType = "Unconfined Alluvial Sand",
                totalBoreDepthMeters = 50.0,
                installedSensor = SensorType.PRESSURE_TRANSDUCER,
                health = StationHealth.ANOMALOUS,
                lastPingTime = "4 mins ago",
                currentWaterLevel = s1Readings.last().rawWaterLevelMeters,
                baselineNormalDepth = 14.5,
                batteryVoltage = 3.92,
                signalDbm = -74,
                anomalyCount = s1Readings.count { it.isSanitized },
                readings = s1Readings
            ),
            WaterStation(
                id = "st-02",
                name = "South Delhi Piezometer PZ-04",
                code = "CGWB-DL-STH04",
                district = "Hauz Khas",
                state = "Delhi NCT",
                latitude = 28.5494,
                longitude = 77.2001,
                aquiferType = "Weathered Quartzite Ridge",
                totalBoreDepthMeters = 120.0,
                installedSensor = SensorType.PRESSURE_TRANSDUCER,
                health = StationHealth.STUCK,
                lastPingTime = "12 mins ago",
                currentWaterLevel = s2Readings.last().rawWaterLevelMeters,
                baselineNormalDepth = 32.2,
                batteryVoltage = 3.85,
                signalDbm = -82,
                anomalyCount = s2Readings.count { it.isSanitized },
                readings = s2Readings
            ),
            WaterStation(
                id = "st-03",
                name = "Hindon Basin Deep Well-09",
                code = "CGWB-UP-HIN09",
                district = "Ghaziabad",
                state = "Uttar Pradesh",
                latitude = 28.6692,
                longitude = 77.4538,
                aquiferType = "Older Alluvial Silt-Clay",
                totalBoreDepthMeters = 85.0,
                installedSensor = SensorType.RADAR_WATER_LEVEL,
                health = StationHealth.DRIFTING,
                lastPingTime = "18 mins ago",
                currentWaterLevel = s3Readings.last().rawWaterLevelMeters,
                baselineNormalDepth = 21.0,
                batteryVoltage = 3.78,
                signalDbm = -69,
                anomalyCount = s3Readings.count { it.isSanitized },
                readings = s3Readings
            ),
            WaterStation(
                id = "st-04",
                name = "Okhla Barrage Monitoring Node",
                code = "CGWB-DL-OKH02",
                district = "Okhla",
                state = "Delhi NCT",
                latitude = 28.5300,
                longitude = 77.3000,
                aquiferType = "Riverine Medium Sand",
                totalBoreDepthMeters = 35.0,
                installedSensor = SensorType.ULTRASONIC_LEVEL,
                health = StationHealth.HEALTHY,
                lastPingTime = "2 mins ago",
                currentWaterLevel = s4Readings.last().rawWaterLevelMeters,
                baselineNormalDepth = 8.4,
                batteryVoltage = 4.12,
                signalDbm = -65,
                anomalyCount = 0,
                readings = s4Readings
            ),
            WaterStation(
                id = "st-05",
                name = "Mehrauli Deep Ridge PZ-12",
                code = "CGWB-DL-MHR12",
                district = "Mehrauli",
                state = "Delhi NCT",
                latitude = 28.5175,
                longitude = 77.1850,
                aquiferType = "Fractured Quartzite Bedrock",
                totalBoreDepthMeters = 150.0,
                installedSensor = SensorType.PRESSURE_TRANSDUCER,
                health = StationHealth.HEALTHY,
                lastPingTime = "6 mins ago",
                currentWaterLevel = s5Readings.last().rawWaterLevelMeters,
                baselineNormalDepth = 48.0,
                batteryVoltage = 3.98,
                signalDbm = -78,
                anomalyCount = 0,
                readings = s5Readings
            ),
            WaterStation(
                id = "st-06",
                name = "Najafgarh Drain DWLR-07",
                code = "CGWB-DL-NJF07",
                district = "South-West Delhi",
                state = "Delhi NCT",
                latitude = 28.6090,
                longitude = 76.9850,
                aquiferType = "Saline-Fresh Alluvial Interface",
                totalBoreDepthMeters = 60.0,
                installedSensor = SensorType.CONDUCTIVITY_PROBE,
                health = StationHealth.INTERMITTENT,
                lastPingTime = "35 mins ago",
                currentWaterLevel = s6Readings.last().rawWaterLevelMeters,
                baselineNormalDepth = 18.2,
                batteryVoltage = 3.15,
                signalDbm = -95,
                anomalyCount = s6Readings.count { it.isSanitized },
                readings = s6Readings
            )
        )
    }

    fun getInitialTickets(): List<MaintenanceTicket> {
        return listOf(
            MaintenanceTicket(
                id = "TKT-DL-8821",
                stationId = "st-02",
                stationName = "South Delhi Piezometer PZ-04",
                stationCode = "CGWB-DL-STH04",
                anomalyType = AnomalyType.STUCK_VALUE,
                priority = TicketPriority.CRITICAL,
                status = TicketStatus.OPEN,
                createdAt = "Today, 08:30 AM",
                diagnosticSummary = "Transducer reading flatlined at 32.20m for 16 consecutive hours. Mechanical float wedged or severe siltation clogging casing filter slot.",
                recommendedAction = "Dispatch mobile purge compressor; flush piezometer casing with clean water; inspect sensor cable suspension tension.",
                assignedEngineer = "Hydrogeologist Team South",
                gpsCoordinates = "28.5494° N, 77.2001° E"
            ),
            MaintenanceTicket(
                id = "TKT-UP-4910",
                stationId = "st-03",
                stationName = "Hindon Basin Deep Well-09",
                stationCode = "CGWB-UP-HIN09",
                anomalyType = AnomalyType.SENSOR_DRIFT,
                priority = TicketPriority.HIGH,
                status = TicketStatus.ASSIGNED,
                createdAt = "Yesterday, 04:15 PM",
                diagnosticSummary = "Progressive calibration drift (+0.25m/day) departing from neighborhood piezometer cluster. Transducer diaphragm zero-point decalibrated.",
                recommendedAction = "Perform manual dip-tape measurement; recalibrate telemetry offset via field calibrator kit or replace sensor unit.",
                assignedEngineer = "Ghaziabad Div Field Unit",
                gpsCoordinates = "28.6692° N, 77.4538° E"
            ),
            MaintenanceTicket(
                id = "TKT-DL-7104",
                stationId = "st-01",
                stationName = "Yamuna Floodplain DWLR-01",
                stationCode = "CGWB-DL-YAM01",
                anomalyType = AnomalyType.TRANSIENT_SPIKE,
                priority = TicketPriority.MEDIUM,
                status = TicketStatus.OPEN,
                createdAt = "Today, 10:15 AM",
                diagnosticSummary = "Single point jump of +7.80m at 14:00 timestamp. Modem transmission packet corruption or grounding loop surge.",
                recommendedAction = "Inspect solar telemetry enclosure grounding rod and lightning arrestor; confirm autoencoder reconstruction replacement in public feed.",
                assignedEngineer = "Instrumentation Tech-02",
                gpsCoordinates = "28.6139° N, 77.2090° E"
            ),
            MaintenanceTicket(
                id = "TKT-DL-3022",
                stationId = "st-06",
                stationName = "Najafgarh Drain DWLR-07",
                stationCode = "CGWB-DL-NJF07",
                anomalyType = AnomalyType.LOW_VOLTAGE_DROPOUT,
                priority = TicketPriority.HIGH,
                status = TicketStatus.OPEN,
                createdAt = "Today, 06:45 AM",
                diagnosticSummary = "Solar pack voltage dropped to 3.15V below operational 3.4V threshold. Step degradation in AD converter readings.",
                recommendedAction = "Clean solar panel dust layer; test 12V LiFePO4 storage battery internal resistance; replace battery if degraded.",
                assignedEngineer = "Solar Telemetry Maintenance Unit",
                gpsCoordinates = "28.6090° N, 76.9850° E"
            )
        )
    }

    private fun generateSeries(
        baseDepth: Double,
        noise: Double,
        count: Int,
        injectSpikeIndex: Int = -1,
        spikeDelta: Double = 0.0,
        stuckStart: Int = -1,
        driftSlope: Double = 0.0,
        rechargeStepIndex: Int = -1,
        rechargeRise: Double = 0.0,
        lowBatteryStart: Int = -1,
        anomalyTypeToInject: AnomalyType = AnomalyType.NONE
    ): List<TelemetryReading> {
        val list = mutableListOf<TelemetryReading>()
        var currentDrift = 0.0

        for (i in 0 until count) {
            val hour = (count - 1 - i)
            val timeLabel = String.format("%02d:00", (24 - hour) % 24)
            var depth = baseDepth + ((i % 5) - 2) * noise

            var readingType = AnomalyType.NONE
            var volts = 3.95 - (i * 0.01)

            if (injectSpikeIndex >= 0 && i == injectSpikeIndex) {
                depth += spikeDelta
                readingType = anomalyTypeToInject
            } else if (stuckStart >= 0 && i >= stuckStart) {
                depth = baseDepth // constant flatline
                readingType = anomalyTypeToInject
            } else if (driftSlope > 0.0 && i > 5) {
                currentDrift += driftSlope
                depth += currentDrift
                if (currentDrift > 0.8) readingType = anomalyTypeToInject
            } else if (rechargeStepIndex >= 0 && i >= rechargeStepIndex) {
                depth += rechargeRise // sudden natural recharge (water level rises)
                readingType = anomalyTypeToInject
            } else if (lowBatteryStart >= 0 && i >= lowBatteryStart) {
                volts = 3.12
                depth += 2.40 // artificial step jump due to low supply rail
                readingType = anomalyTypeToInject
            }

            list.add(
                TelemetryReading(
                    id = "rd-$i-${UUID.randomUUID().toString().take(4)}",
                    timestampMillis = System.currentTimeMillis() - (hour * 3600_000L),
                    timeLabel = timeLabel,
                    rawWaterLevelMeters = (depth * 100).toInt() / 100.0,
                    batteryVolts = (volts * 100).toInt() / 100.0,
                    waterTempCelsius = 24.5 + (i * 0.05),
                    reconstructedLevelMeters = baseDepth,
                    reconstructionError = 0.0,
                    anomalyScore = 0.0,
                    anomalyType = readingType
                )
            )
        }
        return list
    }
}
