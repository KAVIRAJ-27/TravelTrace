package com.travelhistory.app.ui.home

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelhistory.app.location.LocationState
import com.travelhistory.app.ui.components.StatCard
import com.travelhistory.app.ui.theme.StatusOffBg
import com.travelhistory.app.ui.theme.StatusOffText
import com.travelhistory.app.ui.theme.StatusOnBg
import com.travelhistory.app.ui.theme.StatusOnText

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
    onNavigateToMap: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by homeViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // ActivityResult Launcher for runtime location permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val isGranted = fineGranted || coarseGranted
        homeViewModel.onPermissionResult(isGranted)
    }

    LaunchedEffect(uiState.shouldRequestPermission) {
        if (uiState.shouldRequestPermission) {
            val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }
            permissionLauncher.launch(permissions)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // App Header with Branding and Live Tracking Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Travel History",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Smart GPS Dashboard",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Tracking Status Pill
            val isTracking = uiState.isTracking
            val badgeBg = if (isTracking) StatusOnBg else StatusOffBg
            val badgeTextColor = if (isTracking) StatusOnText else StatusOffText
            val badgeText = if (isTracking) "Tracking Active" else "Tracking Stopped"

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = badgeBg,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(badgeTextColor, shape = CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tracking Health & Status Card
        com.travelhistory.app.ui.home.components.TrackingHealthCard(
            stats = uiState.trackingHealth,
            onEnableGpsClick = {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                context.startActivity(intent)
            },
            onRequestPermissionClick = {
                homeViewModel.onStartTrackingClicked()
            }
        )

        if (uiState.isTracking && uiState.nextRecordingTime != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next Fix Expected:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = uiState.nextRecordingTimeText,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Error Alerts (GPS Disabled or Permission Denied)
        AnimatedVisibility(visible = uiState.locationState is LocationState.PermissionDenied) {
            val deniedState = uiState.locationState as? LocationState.PermissionDenied
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Location Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Travel History requires GPS location access to log your coordinates. Please grant location permissions to continue tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (deniedState?.isPermanentlyDenied == true) {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            } else {
                                val permissions = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                        Manifest.permission.POST_NOTIFICATIONS
                                    )
                                } else {
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                }
                                permissionLauncher.launch(permissions)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (deniedState?.isPermanentlyDenied == true) "Open App Settings" else "Grant Permission"
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = uiState.locationState is LocationState.LocationDisabled) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.GpsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GPS Location Services Disabled",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Device GPS location is switched off. Enable Location Services in Android Settings to begin tracking.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Enable Location in Settings")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Section
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Primary Action: Start / Stop Tracking
        Button(
            onClick = {
                homeViewModel.onStartTrackingClicked()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (uiState.isTracking) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            ),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            Icon(
                imageVector = if (uiState.isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = if (uiState.isTracking) "Stop Tracking" else "Start Tracking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary Quick Action Tiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickActionButton(
                title = "Open Map",
                icon = Icons.Outlined.Map,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToMap
            )
            QuickActionButton(
                title = "Today's History",
                icon = Icons.Outlined.CalendarToday,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToHistory
            )
            QuickActionButton(
                title = "Settings",
                icon = Icons.Outlined.Settings,
                modifier = Modifier.weight(1f),
                onClick = onNavigateToSettings
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Today's Statistics Section
        Text(
            text = "Today's Statistics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Locations Recorded Today
        StatCard(
            title = "Locations Recorded Today",
            value = uiState.todayRecordCount.toString(),
            icon = Icons.Outlined.FormatListNumbered,
            iconTint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Today's Trips
        StatCard(
            title = "Today's Trips",
            value = "${uiState.todayTripsCount}",
            icon = Icons.Outlined.DirectionsCar,
            iconTint = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Today's Distance (Calculated via Haversine formula)
        StatCard(
            title = "Today's Distance",
            value = uiState.distanceTodayText,
            icon = Icons.Outlined.Route,
            iconTint = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Today's Travel Duration
        StatCard(
            title = "Today's Travel Duration",
            value = uiState.todayTravelDurationText,
            icon = Icons.Outlined.Timer,
            iconTint = MaterialTheme.colorScheme.outline
        )

        // Today's Trips Breakdown Cards
        if (uiState.todayTrips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Today's Trips (${uiState.todayTrips.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                uiState.todayTrips.forEachIndexed { index, trip ->
                    com.travelhistory.app.ui.components.TripSummaryCard(trip = trip, tripIndex = index + 1)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Last Location Card
        Text(
            text = "Last Location",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.latestLocation != null) {
            // Valid Location Fix Display
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Current Coordinates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = uiState.accuracyText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Coordinates 2x2 Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LATITUDE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.latitudeText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "LONGITUDE",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.longitudeText,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RECORDED AT",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = uiState.lastRecordedTimeText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (uiState.todayRecordCount > 0) {
                            OutlinedButton(
                                onClick = onNavigateToMap,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Map,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "View Map",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Clean Empty State Card - strictly NO fake coordinates or fake statistics
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MyLocation,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "No Locations Recorded Today",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Start tracking above to record your journey. Live GPS coordinates and timestamps will appear here once captured.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
