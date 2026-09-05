package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDamage
import androidx.compose.material.icons.filled.Yard
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.WaterParameters
import com.example.ui.components.ContaminantReductionChart
import com.example.ui.components.MetricCard
import com.example.ui.components.MetricStatus
import com.example.ui.components.PulsingLiveBadge
import com.example.ui.components.SectionHeader
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.SleekAgriGreen
import com.example.ui.theme.SleekAgriGreenContainer
import com.example.ui.theme.WarningAmber
import com.example.viewmodel.HydroUiState
import com.example.viewmodel.HydroViewModel

@Composable
fun DashboardScreen(
    viewModel: HydroViewModel,
    uiState: HydroUiState,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val params = uiState.currentParams
    val telemetry = uiState.facilityTelemetry

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 24.dp)
    ) {
        // Hero Visual Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.banner_water_facility),
                contentDescription = "Water Treatment Facility Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Sleek Dark Scrim for Hero
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x991D1B20),
                                Color(0xF01D1B20)
                            )
                        )
                    )
            )

            // Overlaid header content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "WATER HEALTH & SAFETY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                fontSize = 11.sp
                            ),
                            color = Color(0xFFD0BCFF)
                        )
                        Text(
                            text = "Real-Time Quality Check",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.3).sp
                            ),
                            color = Color.White
                        )
                    }

                    PulsingLiveBadge(isLive = uiState.isLiveMqttStreaming)
                }
            }
        }

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {

            // Controls & MQTT Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleMqttStreaming() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isLiveMqttStreaming) WarningAmber else MaterialTheme.colorScheme.primary,
                        contentColor = if (uiState.isLiveMqttStreaming) Color(0xFF1D1B20) else MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_mqtt_button")
                ) {
                    Icon(
                        imageVector = if (uiState.isLiveMqttStreaming) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isLiveMqttStreaming) "Pause Live Stream" else "Start Live Updates",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.saveCurrentSampleToHistory("Water Check Log") },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.testTag("save_sample_btn")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Log")
                }
            }

            // Benchmark Presets Carousel
            Text(
                text = "QUICK WATER SOURCES",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.presetScenarios.forEach { (name, presetParams) ->
                    val isSelected = (params.ph == presetParams.ph && params.tds == presetParams.tds)
                    Surface(
                        onClick = { viewModel.loadPreset(presetParams, name) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.testTag("preset_${name.take(6).trim()}")
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // HUMAN-FRIENDLY EVERYDAY WATER SAFETY CARD
            val isDrinkable = uiState.mlResult.waterType.drinkable
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDrinkable) SleekAgriGreenContainer else MaterialTheme.colorScheme.primaryContainer
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isDrinkable) SleekAgriGreen.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isDrinkable) SleekAgriGreen else MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isDrinkable) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (isDrinkable) "Safe & Clean Water" else "Filtration Recommended",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isDrinkable) SleekAgriGreen else MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "Overall Purity: ${params.qualityScore}/100",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = if (params.qualityScore >= 80) "Grade A" else if (params.qualityScore >= 60) "Grade B" else "Grade C",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Everyday Usability Check Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDrinkable) SleekAgriGreen.copy(alpha = 0.35f) else WarningAmber.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.LocalDrink,
                                    contentDescription = null,
                                    tint = if (isDrinkable) SleekAgriGreen else WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Drinking", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                Text(if (isDrinkable) "Safe" else "Filter First", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = if (isDrinkable) SleekAgriGreen else WarningAmber)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (params.turbidity < 3.0 && params.tds < 2000) SleekAgriGreen.copy(alpha = 0.35f) else WarningAmber.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = if (params.turbidity < 3.0 && params.tds < 2000) SleekAgriGreen else WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Cooking", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                Text(if (params.turbidity < 3.0 && params.tds < 2000) "Safe" else "Boil First", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = if (params.turbidity < 3.0 && params.tds < 2000) SleekAgriGreen else WarningAmber)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (params.tds < 1500) SleekAgriGreen.copy(alpha = 0.35f) else WarningAmber.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Yard,
                                    contentDescription = null,
                                    tint = if (params.tds < 1500) SleekAgriGreen else WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Plants", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                                Text(if (params.tds < 1500) "Great" else "High Salt", style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold), color = if (params.tds < 1500) SleekAgriGreen else WarningAmber)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = when {
                            isDrinkable -> "Great water quality! Mineral balance is healthy, and the water is clear and fresh."
                            params.tds > 10000 -> "Seawater salinity detected. Reverse Osmosis (RO) purifier is required before drinking."
                            params.ph < 6.0 -> "Water is slightly acidic. Adding basic minerals or a neutralizer cartridge is recommended."
                            params.turbidity > 5.0 -> "Water looks cloudy. A simple cloth or sediment cartridge filter will clear it up quickly."
                            else -> "Standard home filtration (like a pitcher or under-sink carbon filter) will give you fresh, delicious drinking water."
                        },
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 6 Core Physicochemical Telemetry Grid
            SectionHeader(
                title = "Water Measurements",
                subtitle = "Simple readings explained in plain words",
                icon = Icons.Default.Science,
                badgeText = "WQI: ${params.qualityScore}/100"
            )

            // Row 1: pH & TDS
            // Row 1: pH & Minerals (TDS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Acidity Level (pH)",
                    value = String.format("%.2f", params.ph),
                    unit = "pH",
                    icon = Icons.Default.InvertColors,
                    status = when {
                        params.ph < 6.0 || params.ph > 8.5 -> MetricStatus.CRITICAL
                        params.ph in 6.5..7.8 -> MetricStatus.NORMAL
                        else -> MetricStatus.WARNING
                    },
                    subtitle = when {
                        params.ph < 6.0 -> "Too Acidic (sour taste)"
                        params.ph > 8.5 -> "Too Alkaline (soapy feel)"
                        else -> "Healthy Balance (6.5 – 8.5)"
                    },
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Minerals & Salt (TDS)",
                    value = String.format("%.0f", params.tds),
                    unit = "ppm",
                    icon = Icons.Default.Grain,
                    status = when {
                        params.tds > 5000 -> MetricStatus.CRITICAL
                        params.tds > 1000 -> MetricStatus.WARNING
                        else -> MetricStatus.NORMAL
                    },
                    subtitle = when {
                        params.tds > 10000 -> "Very Salty (Seawater)"
                        params.tds > 1000 -> "High Minerals"
                        else -> "Fresh & Pure (< 500 ppm)"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Water Clarity & Dissolved Salts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Water Clarity",
                    value = String.format("%.2f", params.turbidity),
                    unit = "NTU",
                    icon = Icons.Default.Opacity,
                    status = when {
                        params.turbidity > 5.0 -> MetricStatus.CRITICAL
                        params.turbidity > 1.0 -> MetricStatus.WARNING
                        else -> MetricStatus.NORMAL
                    },
                    subtitle = when {
                        params.turbidity > 5.0 -> "Cloudy / Muddy (Filter needed)"
                        params.turbidity > 1.0 -> "Slightly Hazy"
                        else -> "Crystal Clear"
                    },
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Salt Ions (Conductivity)",
                    value = String.format("%.0f", params.ec),
                    unit = "μS/cm",
                    icon = Icons.Default.Bolt,
                    status = when {
                        params.ec > 8000 -> MetricStatus.CRITICAL
                        params.ec > 1500 -> MetricStatus.WARNING
                        else -> MetricStatus.NORMAL
                    },
                    subtitle = if (params.ec > 1500) "High Salinity" else "Low Salt / Fresh",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Limescale Hardness & Freshness
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "Limescale (Hardness)",
                    value = String.format("%.0f", params.hardness),
                    unit = "mg/L",
                    icon = Icons.Default.WaterDamage,
                    status = if (params.hardness > 250) MetricStatus.WARNING else MetricStatus.NORMAL,
                    subtitle = if (params.hardness > 250) "Hard (Leaves white spots)" else "Soft (Gentle on skin & pipes)",
                    modifier = Modifier.weight(1f)
                )

                MetricCard(
                    title = "Purity & Odor",
                    value = String.format("%.1f", params.organicCarbon),
                    unit = "ppm",
                    icon = Icons.Default.Thermostat,
                    status = if (params.organicCarbon > 4.5) MetricStatus.WARNING else MetricStatus.NORMAL,
                    subtitle = if (params.organicCarbon > 4.5) "Has odor, carbon filter needed" else "Clean & odor-free",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Purification Cleaning Visualizer
            SectionHeader(
                title = "Before & After Cleaning",
                subtitle = "See how filtration removes dirt and impurities",
                icon = Icons.Default.Refresh
            )

            // Contaminant Reduction Visualizer
            ContaminantReductionChart(
                turbidityRaw = params.turbidity,
                tdsRaw = params.tds,
                phRaw = params.ph,
                predictedYield = uiState.mlResult.predictedYieldPercent
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Filter Health & Maintenance
            SectionHeader(
                title = "Filter System Health",
                subtitle = "Status of pumps and filter cartridges",
                icon = Icons.Default.Speed
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Quick Stats Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        FacilityStat(label = "Flow Speed", value = "${String.format("%.0f", telemetry.flowRateLitersMin)} L/min")
                        FacilityStat(label = "Water Pressure", value = "${String.format("%.1f", telemetry.feedPressureBar)} bar")
                        FacilityStat(label = "Clean Output", value = "${String.format("%.1f", uiState.mlResult.predictedYieldPercent)}%")
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Filter Cleanliness
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Main Filter Cleanliness",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${100 - telemetry.membraneFoulingIndex}% Clean",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = SleekAgriGreen
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (100 - telemetry.membraneFoulingIndex) / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = SleekAgriGreen,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Supplies status
                    ChemicalStockRow(name = "Sediment Filter Cartridge", percent = 92)
                    ChemicalStockRow(name = "Carbon Filter Cartridge", percent = telemetry.antiscalantStockPercent)
                    ChemicalStockRow(name = "Water Conditioner Buffer", percent = telemetry.limeStockPercent)

                    Spacer(modifier = Modifier.height(10.dp))

                    // Next cleaning badge
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next automated self-cleaning cycle in ${telemetry.filterBackwashHoursLeft} hours",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick button to go to Water Testing
            Button(
                onClick = { viewModel.setTab(1) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("navigate_to_ml_btn")
            ) {
                Text(
                    text = "Test & Adjust Water Values",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun FacilityStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ChemicalStockRow(name: String, percent: Int) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = if (percent < 25) DangerCoral else MaterialTheme.colorScheme.primary
            )
        }
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (percent < 25) DangerCoral else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}
