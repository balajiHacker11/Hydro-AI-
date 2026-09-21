package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AnomalyType
import com.example.model.TelemetryReading
import kotlin.math.max
import kotlin.math.min

/**
 * Interactive Time-Series Hydrograph Canvas:
 * Plots Raw Telemetry vs. Autoencoder Neural Reconstruction vs. Flagged Anomaly Points.
 */
@Composable
fun HydrographChart(
    readings: List<TelemetryReading>,
    showReconstructed: Boolean = true,
    modifier: Modifier = Modifier
) {
    if (readings.isEmpty()) return

    val rawColor = Color(0xFF0284C7)           // Sky Blue (Raw Signal)
    val reconstructedColor = Color(0xFF10B981) // Emerald Green (Neural Baseline)
    val anomalyColor = Color(0xFFEF4444)       // Red Alert
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)

    val minLevel = (readings.minOfOrNull { it.rawWaterLevelMeters } ?: 10.0) - 1.5
    val maxLevel = (readings.maxOfOrNull { it.rawWaterLevelMeters } ?: 20.0) + 1.5
    val levelRange = max(0.5, maxLevel - minLevel)

    Column(modifier = modifier) {
        // Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(172.dp)) {
                val width = size.width
                val height = size.height
                val stepX = width / (readings.size - 1).coerceAtLeast(1)

                // 1. Draw horizontal baseline grid lines (3 levels)
                for (step in 0..3) {
                    val y = (height / 3f) * step
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(width, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                    )
                }

                // 2. Plot Autoencoder Reconstructed Line (Green dashed)
                if (showReconstructed) {
                    val reconPath = Path()
                    readings.forEachIndexed { i, r ->
                        val x = i * stepX
                        // Note: Higher depth (m bgl) means deeper groundwater table -> plot lower or higher as depth
                        val normalizedY = ((r.reconstructedLevelMeters - minLevel) / levelRange).toFloat()
                        val y = (normalizedY * height).coerceIn(4f, height - 4f)
                        if (i == 0) reconPath.moveTo(x, y) else reconPath.lineTo(x, y)
                    }
                    drawPath(
                        path = reconPath,
                        color = reconstructedColor,
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }

                // 3. Plot Raw Telemetry Line (Blue solid)
                val rawPath = Path()
                readings.forEachIndexed { i, r ->
                    val x = i * stepX
                    val normalizedY = ((r.rawWaterLevelMeters - minLevel) / levelRange).toFloat()
                    val y = (normalizedY * height).coerceIn(4f, height - 4f)
                    if (i == 0) rawPath.moveTo(x, y) else rawPath.lineTo(x, y)
                }
                drawPath(
                    path = rawPath,
                    color = rawColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )

                // 4. Highlight Anomaly Points on the curve
                readings.forEachIndexed { i, r ->
                    if (r.anomalyType != AnomalyType.NONE && !r.anomalyType.isTrueHydrologicEvent) {
                        val x = i * stepX
                        val normalizedY = ((r.rawWaterLevelMeters - minLevel) / levelRange).toFloat()
                        val y = (normalizedY * height).coerceIn(4f, height - 4f)

                        // Outer alert circle
                        drawCircle(
                            color = Color(r.anomalyType.colorHex).copy(alpha = 0.35f),
                            radius = 14f,
                            center = Offset(x, y)
                        )
                        // Inner center dot
                        drawCircle(
                            color = Color(r.anomalyType.colorHex),
                            radius = 7f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Time axis labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = readings.firstOrNull()?.timeLabel ?: "24h ago",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Time-Series Window (Last 24 Hours)",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = readings.lastOrNull()?.timeLabel ?: "Now",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Legend row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(rawColor, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Raw Sensor (m bgl)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (showReconstructed) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(reconstructedColor, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Autoencoder Baseline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(10.dp).background(anomalyColor, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Flagged Anomaly",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
