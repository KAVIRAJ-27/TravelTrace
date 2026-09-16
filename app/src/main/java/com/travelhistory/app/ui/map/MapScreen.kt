package com.travelhistory.app.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.ui.map.provider.GoogleMapRenderer
import com.travelhistory.app.ui.map.provider.MapProviderType
import com.travelhistory.app.ui.map.provider.MapSelectionMode
import com.travelhistory.app.ui.map.provider.OfflineMapLibreRenderer

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    mapViewModel: MapViewModel = viewModel(),
    initialDateMillis: Long? = null,
    initialTripId: Long? = null,
    onNavigateToOfflineMaps: () -> Unit = {}
) {
    val uiState by mapViewModel.uiState.collectAsState()
    var showProviderDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialDateMillis, initialTripId) {
        if ((initialDateMillis != null && initialDateMillis > 0) || (initialTripId != null && initialTripId > 0)) {
            mapViewModel.selectDate(
                dateMillis = initialDateMillis ?: System.currentTimeMillis(),
                tripId = initialTripId
            )
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Active Map Renderer (Google Maps vs MapLibre Native)
        when (uiState.activeProvider) {
            MapProviderType.GOOGLE_MAPS -> {
                GoogleMapRenderer(
                    records = uiState.records,
                    hasLocationPermission = uiState.hasLocationPermission,
                    onPointSelected = { mapViewModel.onPointSelected(it) }
                )
            }
            MapProviderType.MAPLIBRE_OFFLINE -> {
                OfflineMapLibreRenderer(
                    records = uiState.records,
                    hasLocationPermission = uiState.hasLocationPermission,
                    onPointSelected = { mapViewModel.onPointSelected(it) }
                )
            }
        }

        // 2. Top Controls & Overlays
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Informational Banner if Google Maps is active and key is missing
            if (uiState.activeProvider == MapProviderType.GOOGLE_MAPS && !uiState.isApiKeyConfigured) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "MAPS_API_KEY not configured. Switch to Offline MapLibre?",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                        TextButton(
                            onClick = { mapViewModel.setSelectionMode(MapSelectionMode.FORCE_MAPLIBRE) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Use MapLibre",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Trip Filter Banner (if currently viewing a specific trip)
            if (uiState.isTripRouteActive) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.selectedTrip != null) {
                                    "Viewing Trip (${uiState.selectedTrip?.formattedDistance})"
                                } else {
                                    "Viewing Trip Route"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        TextButton(
                            onClick = { mapViewModel.clearTripFilter() },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Show All Day",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Top Status Bar: Provider Indicator Pill + Offline Maps button + Points Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Provider Status Badge (Clickable to switch mode)
                val isOnlineMap = uiState.activeProvider == MapProviderType.GOOGLE_MAPS
                val badgeColor = if (isOnlineMap) Color(0xFF10B981) else Color(0xFFF59E0B)
                val badgeText = if (isOnlineMap) "Online • Google Maps" else "Offline • MapLibre"

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 4.dp,
                    shadowElevation = 3.dp,
                    modifier = Modifier.clickable { showProviderDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(badgeColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Offline Maps management shortcut button
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 4.dp,
                        shadowElevation = 3.dp,
                        modifier = Modifier.clickable { onNavigateToOfflineMaps() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DownloadForOffline,
                                contentDescription = "Offline Maps",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Offline",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Points Count Chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 4.dp,
                        shadowElevation = 3.dp
                    ) {
                        Text(
                            text = "${uiState.records.size} pts",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Date Navigation Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 6.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { mapViewModel.selectPreviousDay() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Day",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = uiState.selectedDateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (uiState.records.isNotEmpty()) "${uiState.records.size} recorded fixes" else "No records",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { mapViewModel.selectNextDay() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Day",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // 3. Floating Action Button: Switch Provider Quick Action
        FloatingActionButton(
            onClick = { showProviderDialog = true },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 140.dp, end = 16.dp)
                .size(46.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Map,
                contentDescription = "Map Settings",
                modifier = Modifier.size(20.dp)
            )
        }

        // 4. Empty State Floating Notice
        AnimatedVisibility(
            visible = !uiState.hasRecords,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp)
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "No location history for this date.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Start tracking on the Home screen to record GPS journey points.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 5. Location Timeline Bar (if records exist)
        if (uiState.records.isNotEmpty()) {
            val listState = rememberLazyListState()

            // Automatically scroll timeline to selected point
            LaunchedEffect(uiState.selectedPointIndex) {
                if (uiState.selectedPointIndex > 0 && uiState.selectedPointIndex <= uiState.records.size) {
                    listState.animateScrollToItem((uiState.selectedPointIndex - 1).coerceAtLeast(0))
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = if (uiState.selectedPoint != null) 184.dp else 16.dp)
            ) {
                LazyRow(
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(uiState.records) { index, record ->
                        val isSelected = uiState.selectedPoint?.id == record.id
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                            },
                            border = if (isSelected) {
                                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            tonalElevation = if (isSelected) 8.dp else 4.dp,
                            shadowElevation = 3.dp,
                            modifier = Modifier.clickable { mapViewModel.onPointSelected(record) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(
                                            color = when (index) {
                                                0 -> Color(0xFF10B981) // Green
                                                uiState.records.size - 1 -> Color(0xFFEF4444) // Red
                                                else -> MaterialTheme.colorScheme.primary
                                            },
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = record.formattedTime,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Selected Point Details Bottom Card
        AnimatedVisibility(
            visible = uiState.selectedPoint != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            uiState.selectedPoint?.let { record ->
                PointDetailCard(
                    record = record,
                    pointIndex = uiState.selectedPointIndex,
                    totalPoints = uiState.records.size,
                    onDismiss = { mapViewModel.onDismissPointDetails() }
                )
            }
        }

        // 7. Provider Selection Dialog
        if (showProviderDialog) {
            MapProviderSelectionDialog(
                currentMode = uiState.selectionMode,
                isCurrentlyOnline = uiState.isOnline,
                onModeSelected = { mode ->
                    mapViewModel.setSelectionMode(mode)
                    showProviderDialog = false
                },
                onManageOfflineMaps = {
                    showProviderDialog = false
                    onNavigateToOfflineMaps()
                },
                onDismiss = { showProviderDialog = false }
            )
        }
    }
}

@Composable
private fun MapProviderSelectionDialog(
    currentMode: MapSelectionMode,
    isCurrentlyOnline: Boolean,
    onModeSelected: (MapSelectionMode) -> Unit,
    onManageOfflineMaps: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Map Provider Selection",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = if (isCurrentlyOnline) "Network: Connected (Online)" else "Network: Disconnected (Offline)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (isCurrentlyOnline) Color(0xFF10B981) else Color(0xFFF59E0B)
                )
                Spacer(modifier = Modifier.height(12.dp))

                MapSelectionMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeSelected(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal
                            )
                            val description = when (mode) {
                                MapSelectionMode.AUTO -> "Auto-detect: Google Maps when online, MapLibre when offline."
                                MapSelectionMode.FORCE_GOOGLE_MAPS -> "Always use Google Maps SDK."
                                MapSelectionMode.FORCE_MAPLIBRE -> "Always use offline MapLibre Native SDK."
                            }
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onManageOfflineMaps,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DownloadForOffline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Offline Map Regions")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PointDetailCard(
    record: LocationRecord,
    pointIndex: Int,
    totalPoints: Int,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GpsFixed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (pointIndex > 0) "Location Fix #$pointIndex" else "Location Fix",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = record.formattedTimeWithSeconds,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Telemetry Rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Latitude",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.formattedLatitude,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Longitude",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = record.formattedLongitude,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GPS Accuracy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "±${record.formattedAccuracy}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "Raw GPS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
