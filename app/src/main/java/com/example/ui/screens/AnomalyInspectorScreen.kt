package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnomalyType
import com.example.model.StationHealth
import com.example.ui.components.HydrographChart
import com.example.viewmodel.TelemetryUiState
import com.example.viewmodel.TelemetryViewModel

@Composable
fun AnomalyInspectorScreen(
    viewModel: TelemetryViewModel,
    uiState: TelemetryUiState
) {
    val station = uiState.selectedStation

    if (station == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No station selected.")
        }
        return
    }

    val flaggedReadings = station.readings.filter { it.anomalyType != AnomalyType.NONE && !it.anomalyType.isTrueHydrologicEvent }
    val primaryAnomaly = flaggedReadings.firstOrNull()?.anomalyType ?: AnomalyType.NONE

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))

            // Station Heading & Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = station.name,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${station.code} • ${station.district}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(station.health.badgeColorHex).copy(alpha = 0.15f)
                ) {
                    Text(
                        text = station.health.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(station.health.badgeColorHex),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hydrograph Time-Series Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DEEP LEARNING TIME-SERIES HYDROGRAPH",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Autoencoder Reconstruction vs. Raw Piezometer Depth",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Toggle Overlay Switch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Neural Baseline",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Switch(
                                checked = uiState.showCleanedOverlay,
                                onCheckedChange = { viewModel.toggleCleanedOverlay() },
                                modifier = Modifier.testTag("toggle_neural_baseline")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HydrographChart(
                        readings = station.readings,
                        showReconstructed = uiState.showCleanedOverlay
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Autoencoder Diagnostic & Anomaly Classifier Panel
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (primaryAnomaly != AnomalyType.NONE) Color(primaryAnomaly.colorHex).copy(alpha = 0.08f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = BorderStroke(
                    1.2.dp,
                    if (primaryAnomaly != AnomalyType.NONE) Color(primaryAnomaly.colorHex).copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (primaryAnomaly != AnomalyType.NONE) Color(primaryAnomaly.colorHex).copy(alpha = 0.2f)
                                        else Color(0xFF10B981).copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (primaryAnomaly != AnomalyType.NONE) Icons.Default.Warning else Icons.Default.CheckCircle,
                                    contentDescription = "Diagnostic",
                                    tint = if (primaryAnomaly != AnomalyType.NONE) Color(primaryAnomaly.colorHex) else Color(0xFF10B981),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (primaryAnomaly != AnomalyType.NONE) primaryAnomaly.title else "Telemetry Aligns with Historical Model",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Error Code: ${primaryAnomaly.code}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = if (primaryAnomaly != AnomalyType.NONE) Color(primaryAnomaly.colorHex) else Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = primaryAnomaly.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Anomaly Sensitivity Slider
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Autoencoder Detection Sensitivity",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${(uiState.anomalySensitivity * 100).toInt()}% (${if (uiState.anomalySensitivity > 1.2) "High Recall" else "Balanced"})",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Slider(
                            value = uiState.anomalySensitivity,
                            onValueChange = { viewModel.setAnomalySensitivity(it) },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.testTag("sensitivity_slider")
                        )
                    }

                    if (primaryAnomaly != AnomalyType.NONE && !primaryAnomaly.isTrueHydrologicEvent) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Dispatch Maintenance Ticket Button
                        Button(
                            onClick = {
                                viewModel.createTicketForStation(station, primaryAnomaly)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dispatch_ticket_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(primaryAnomaly.colorHex)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Build, contentDescription = "Ticket", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Dispatch Field Maintenance Ticket", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Raw vs Sanitized Stream Table
            Text(
                text = "Recent Readings & Autoencoder Residuals",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // List individual readings with residuals
        items(station.readings.takeLast(10).reversed()) { reading ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (reading.anomalyType != AnomalyType.NONE && !reading.anomalyType.isTrueHydrologicEvent)
                        Color(reading.anomalyType.colorHex).copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = reading.timeLabel,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Battery: ${reading.batteryVolts}V • ${reading.waterTempCelsius}°C",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${reading.rawWaterLevelMeters} m",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (reading.anomalyType != AnomalyType.NONE && !reading.anomalyType.isTrueHydrologicEvent)
                                        Color(reading.anomalyType.colorHex) else MaterialTheme.colorScheme.onSurface
                                )
                            )
                            if (reading.isSanitized) {
                                Text(
                                    text = " ➔ ${reading.sanitizedLevelMeters} m",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                )
                            }
                        }

                        Text(
                            text = if (reading.anomalyType != AnomalyType.NONE) reading.anomalyType.title else "Residual: ±${reading.reconstructionError}m",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (reading.anomalyType != AnomalyType.NONE) Color(reading.anomalyType.colorHex) else MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
