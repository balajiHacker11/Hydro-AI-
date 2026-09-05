package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.HistorySheetDialog
import com.example.ui.screens.AgriSoilScreen
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.ClassificationScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.SolutionsGuideScreen
import com.example.ui.theme.AquaAccent
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.DeepOceanDark
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.HydroViewModel

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

data class NavDestination(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydroAiApp(viewModel: HydroViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val savedSamples by viewModel.savedSamples.collectAsState()
    var showHistorySheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.notificationMessage) {
        uiState.notificationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissNotification()
        }
    }

    val destinations = listOf(
        NavDestination("Status", Icons.Default.Dashboard, "nav_item_0"),
        NavDestination("Test Water", Icons.Default.Science, "nav_item_1"),
        NavDestination("Plants & Soil", Icons.Default.Agriculture, "nav_item_2"),
        NavDestination("How to Clean", Icons.Default.Lightbulb, "nav_item_3"),
        NavDestination("Ask AI", Icons.AutoMirrored.Filled.Chat, "nav_item_4")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.testTag("app_bar")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = "Hydro AI Logo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Hydro AI",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.2).sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val badgeText = when {
                            uiState.mlResult.waterType.isDrinkable -> "Safe to Drink"
                            uiState.mlResult.waterType == com.example.model.WaterType.TYPE_E || uiState.mlResult.waterType == com.example.model.WaterType.TYPE_B -> "Salty / Needs RO"
                            uiState.mlResult.waterType == com.example.model.WaterType.TYPE_D -> "Unsafe / Polluted"
                            else -> "Filter Recommended"
                        }
                        val badgeBg = if (uiState.mlResult.waterType.isDrinkable) {
                            com.example.ui.theme.SleekAgriGreenContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                        val badgeTextColor = if (uiState.mlResult.waterType.isDrinkable) {
                            com.example.ui.theme.SleekAgriGreen
                        } else {
                            MaterialTheme.colorScheme.primary
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = badgeBg,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                badgeTextColor.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                                color = badgeTextColor,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = { showHistorySheet = true },
                            modifier = Modifier.testTag("history_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Saved Samples History",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                border = androidx.compose.foundation.BorderStroke(
                    0.8.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp
                ) {
                    destinations.forEachIndexed { index, dest ->
                        val isSelected = uiState.currentTab == index
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { viewModel.setTab(index) },
                            icon = {
                                Icon(
                                    imageVector = dest.icon,
                                    contentDescription = dest.title
                                )
                            },
                            label = {
                                Text(
                                    text = dest.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp
                                    )
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            modifier = Modifier.testTag(dest.testTag)
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
                label = "tab_crossfade"
            ) { tab ->
                when (tab) {
                    0 -> DashboardScreen(viewModel = viewModel, uiState = uiState)
                    1 -> ClassificationScreen(viewModel = viewModel, uiState = uiState)
                    2 -> AgriSoilScreen(viewModel = viewModel, uiState = uiState)
                    3 -> SolutionsGuideScreen()
                    4 -> AssistantScreen(viewModel = viewModel, uiState = uiState)
                }
            }
        }
    }

    if (showHistorySheet) {
        HistorySheetDialog(
            samples = savedSamples,
            onSelectSample = { sample ->
                viewModel.loadSampleFromHistory(sample)
            },
            onDeleteSample = { id ->
                viewModel.deleteSampleFromHistory(id)
            },
            onClearAll = {
                // repository clear history
            },
            onDismiss = { showHistorySheet = false }
        )
    }
}
