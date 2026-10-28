package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.network.AssistantLanguage
import com.example.ui.screens.AnomalyInspectorScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.SanitizerQualityScreen
import com.example.ui.screens.StationsNetworkScreen
import com.example.ui.screens.TriageTicketsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TelemetryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HydroAiApp()
            }
        }
    }
}

data class TelemetryNavTab(
    val titleEn: String,
    val titleTa: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydroAiApp(viewModel: TelemetryViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    val isTamil = uiState.assistantLanguage == AssistantLanguage.TAMIL

    val tabs = listOf(
        TelemetryNavTab("Stations", "நிலையங்கள்", Icons.Default.GridView, "nav_stations"),
        TelemetryNavTab("Anomaly AI", "பிழை பகுப்பாய்வு", Icons.Default.Timeline, "nav_inspector"),
        TelemetryNavTab("Sanitizer", "சுத்திகரிப்பு", Icons.Default.CleaningServices, "nav_sanitizer"),
        TelemetryNavTab("Triage", "பணி ஆணைகள்", Icons.Default.Assignment, "nav_triage"),
        TelemetryNavTab("AI Voice", "குரல் AI", Icons.Default.AutoAwesome, "nav_ai_voice")
    )

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("hydro_ai_root"),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("app_bar")
                    ) {
                        // Official App Logo
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_hydro_ai_logo),
                                contentDescription = "Hydro AI Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // App Name
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = (-0.2).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = if (isTamil) "DWLR QC" else "DWLR QC",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isTamil) "குரல் வழி AI உதவியாளர் & தரக் கட்டுப்பாடு" else "Voice AI Assistant & Telemetry QC",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Quick Language Switch Pill in Top Bar
                    Surface(
                        onClick = {
                            val nextLang = if (isTamil) AssistantLanguage.ENGLISH else AssistantLanguage.TAMIL
                            viewModel.setAssistantLanguage(nextLang)
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("app_bar_lang_toggle")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isTamil) "🇮🇳 தமிழ்" else "🇬🇧 English",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Live Streaming Connection Toggle
                    IconButton(
                        onClick = { viewModel.toggleLiveStreaming() },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("live_stream_toggle")
                    ) {
                        Icon(
                            imageVector = if (uiState.isLiveStreaming) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = "Live Telemetry Feed",
                            tint = if (uiState.isLiveStreaming) Color(0xFF10B981) else MaterialTheme.colorScheme.outline
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(
                    0.8.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = uiState.currentTab == index
                        val labelText = if (isTamil) tab.titleTa else tab.titleEn
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setTab(index) },
                            icon = {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = labelText
                                )
                            },
                            label = {
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.5.sp
                                    ),
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(tab.testTag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Crossfade(
                targetState = uiState.currentTab,
                label = "telemetry_tab_crossfade"
            ) { tab ->
                when (tab) {
                    0 -> StationsNetworkScreen(viewModel = viewModel, uiState = uiState)
                    1 -> AnomalyInspectorScreen(viewModel = viewModel, uiState = uiState)
                    2 -> SanitizerQualityScreen(viewModel = viewModel, uiState = uiState)
                    3 -> TriageTicketsScreen(viewModel = viewModel, uiState = uiState)
                    4 -> AssistantScreen(viewModel = viewModel, uiState = uiState)
                }
            }
        }
    }
}
