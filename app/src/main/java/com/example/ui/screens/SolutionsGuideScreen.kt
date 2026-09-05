package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.FilterDrama
import androidx.compose.material.icons.filled.Forest
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SectionHeader
import com.example.ui.theme.AquaAccent
import com.example.ui.theme.PureEmerald
import com.example.ui.theme.SleekAgriGreen
import com.example.ui.theme.SleekAgriGreenContainer
import com.example.ui.theme.WarningAmber

@Composable
fun SolutionsGuideScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var selectedEnvTab by remember { mutableIntStateOf(0) }

    val envTabs = listOf("Home & City", "Farm & Garden", "Beach & Coast", "Well & River", "Emergency Clean")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header - Sleek Interface Style
        Column {
            Text(
                text = "HOW TO CLEAN WATER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    fontSize = 11.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Simple Water Purification Guides",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.3).sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Environment Selector Tabs in Sleek Surface Variant
        ScrollableTabRow(
            selectedTabIndex = selectedEnvTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
        ) {
            envTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedEnvTab == index,
                    onClick = { selectedEnvTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedEnvTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (selectedEnvTab == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.testTag("env_tab_$index")
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Environment Content
        when (selectedEnvTab) {
            0 -> UrbanGuide()
            1 -> AgriGuide()
            2 -> CoastalGuide()
            3 -> IndustrialGuide()
            4 -> DroughtGuide()
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Water Conversion Methods Section
        SectionHeader(
            title = "Everyday Water Cleaning Methods",
            subtitle = "Simple guide to how standard home & facility filters work",
            icon = Icons.Default.Science
        )

        ConversionMethodCard(
            title = "Reverse Osmosis (RO Filter)",
            tag = "Removes Salt & Heavy Minerals",
            summary = "Pushes water through a super-fine membrane to filter out 99% of dissolved salts, heavy metals, and limescale, leaving water crisp and pure.",
            formula = "Best for: Salty water, hard tap water, and well water",
            operationalTip = "Replace pre-filter cartridges every 6 to 12 months to keep your main RO filter running strong for years."
        )

        ConversionMethodCard(
            title = "Sediment & Particle Clarifier",
            tag = "Clears Mud & Cloudiness",
            summary = "Traps suspended sand, clay, and algae particles so murky water becomes crystal clear.",
            formula = "Best for: River water, muddy well water, or flood water",
            operationalTip = "Let water settle in a pitcher or jug for 20 minutes before filtering to prevent clogging."
        )

        ConversionMethodCard(
            title = "Activated Carbon Filter",
            tag = "Removes Odors, Bad Taste & Chlorine",
            summary = "Uses natural coconut charcoal to absorb chlorine, organic smells, and bad tastes from tap water.",
            formula = "Best for: City tap water, foul smells, and water pitchers",
            operationalTip = "When your tap water starts tasting flat or smelling like chlorine, replace the carbon cartridge."
        )

        ConversionMethodCard(
            title = "UV Light Disinfection",
            tag = "Kills 99.9% of Bacteria & Germs",
            summary = "Uses safe ultraviolet light rays to neutralize bacteria and viruses without adding any harsh chemicals to your water.",
            formula = "Best for: Untreated spring water, rainwater, and well water",
            operationalTip = "Make sure water is clear of floating dirt first so germs have nowhere to hide from the UV light."
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Tips & Tricks for Facility Management
        SectionHeader(
            title = "Operator Tips & Tricks",
            subtitle = "Field proven guidelines for maximum equipment life and energy efficiency",
            icon = Icons.Default.Lightbulb
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OperatorTipRow(
                    tip = "Prevent Biofouling with Shock Dosing",
                    detail = "Bi-weekly non-oxidizing biocide dosing (DBNPA) stops bacterial slime colonization on RO feed spacers without degrading polyamide chemistry."
                )
                OperatorTipRow(
                    tip = "Energy Recovery Devices (ERD)",
                    detail = "Isobaric pressure exchangers can transfer 96% of the high-pressure concentrate energy directly into the raw feed stream, slashing power draw to < 3.2 kWh/m³."
                )
                OperatorTipRow(
                    tip = "Closed-Loop pH Neutralization",
                    detail = "Install dual pH probes in cascading tanks with PID tuning to prevent overshooting lime dosing and scaling clarifier pipes."
                )
                OperatorTipRow(
                    tip = "Differential Pressure (ΔP) Thresholds",
                    detail = "Trigger automated CIP (Clean-In-Place) flushing when pressure drop across membrane stages increases by 15% above baseline."
                )
            }
        }
    }
}

@Composable
private fun UrbanGuide() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Apartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Urban & Municipal Water Solutions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Urban municipal distribution networks suffer from aging cast iron/lead piping, disinfection by-products (trihalomethanes THMs), and microplastics.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            SolutionPoint("Corrosion Control", "Maintain orthophosphate dosing (1.0 - 3.0 mg/L) to establish an insoluble lead-phosphate protective passivating barrier inside service lines.")
            SolutionPoint("Chloramine & PFAS Stripping", "Deploy Point-Of-Entry (POE) catalytic carbon blocks followed by 0.01-micron hollow-fiber ultrafiltration.")
        }
    }
}

@Composable
private fun AgriGuide() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SleekAgriGreen.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Forest, contentDescription = null, tint = SleekAgriGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Agricultural Watershed Solutions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = SleekAgriGreen)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Agricultural runoff carries high synthetic nitrogen, organophosphate pesticides, and extreme turbidity from eroded topsoil.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            SolutionPoint("Denitrifying Woodchip Bioreactors", "Direct subsurface tile drainage through carbonaceous woodchip trenches where anaerobic bacteria convert NO₃⁻ to harmless N₂ gas.")
            SolutionPoint("Sediment Retention & Alum Catchment", "Build settling forebays with automated flocculant logs to precipitate colloidal mud prior to irrigation canal discharge.")
        }
    }
}

@Composable
private fun CoastalGuide() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Waves, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Coastal & Island Saline Water Solutions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Seawater intrusion and hypersaline coastal aquifers (TDS 10,000 - 38,000 ppm) require high-pressure membrane or thermal extraction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            SolutionPoint("Seawater RO Desalination", "Multi-stage intake wells (beach wells) eliminate marine algae fouling before reaching the 65-bar RO high-pressure pumps.")
            SolutionPoint("Eco-Friendly Brine Outfall", "Diffuse hyper-saline reject brine through high-velocity venturi nozzles into open coastal currents to prevent marine hypoxia.")
        }
    }
}

@Composable
private fun IndustrialGuide() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PrecisionManufacturing, contentDescription = null, tint = WarningAmber)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Industrial & Mine Drainage Neutralization", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = WarningAmber)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Acid Mine Drainage (AMD) and plating rinse baths contain toxic heavy metals (Fe, Cu, Zn, Ni) at pH < 4.0 that require chemical precipitation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            SolutionPoint("High-Density Sludge (HDS) Process", "Recycle precipitated gypsum sludge back into the lime mixing tank to act as crystal seed nuclei, producing compact easily dewatered filter cake.")
            SolutionPoint("Sulfide Precipitation", "Follow lime neutralization with sodium hydrosulfide (NaHS) for deep polishing of heavy metals to < 0.05 mg/L discharge standards.")
        }
    }
}

@Composable
private fun DroughtGuide() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeviceThermostat, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Drought & Emergency Water Reclamation", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Emergency resilience protocols for drought-stricken regions, remote disaster relief, and closed-loop facility water recycling.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            SolutionPoint("Greywater Direct Potable Reuse (DPR)", "Triple-barrier purification: Membrane Bioreactor (MBR) + Reverse Osmosis + UV/H₂O₂ advanced oxidation produces distilled-grade potable water.")
            SolutionPoint("Solar Thermal Distillation", "Passive compound parabolic concentrators evaporate contaminated feeds, condensing pure distillate at zero electrical cost.")
        }
    }
}

@Composable
private fun SolutionPoint(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = "• $title", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
        Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun ConversionMethodCard(
    title: String,
    tag: String,
    summary: String,
    formula: String,
    operationalTip: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(text = summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "Core Equation: $formula",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Pro Tip: $operationalTip",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = SleekAgriGreen
            )
        }
    }
}

@Composable
private fun OperatorTipRow(tip: String, detail: String) {
    Column(modifier = Modifier.padding(vertical = 5.dp)) {
        Text(text = tip, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        Text(text = detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
