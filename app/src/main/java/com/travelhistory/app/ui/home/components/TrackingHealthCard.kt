package com.travelhistory.app.ui.home.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelhistory.app.location.GpsHealthStatus
import com.travelhistory.app.location.TrackingHealthStats
import com.travelhistory.app.ui.theme.StatusOffBg
import com.travelhistory.app.ui.theme.StatusOffText
import com.travelhistory.app.ui.theme.StatusOnBg
import com.travelhistory.app.ui.theme.StatusOnText

/**
 * Modern card displaying real-time Tracking Health, GPS availability,
 * failure diagnostics, and Android battery optimization notices.
 */
@Composable
fun TrackingHealthCard(
    stats: TrackingHealthStats,
    onEnableGpsClick: () -> Unit = {},
    onRequestPermissionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
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
            // Header: Title and Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TRACKING HEALTH",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.5.sp
                    )
                }

                // Active / Stopped status badge
                val isTracking = stats.isTracking
                val badgeBg = if (isTracking) StatusOnBg else StatusOffBg
                val badgeTextColor = if (isTracking) StatusOnText else StatusOffText
                val badgeText = if (isTracking) "Tracking Active" else "Tracking Stopped"

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = badgeBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(badgeTextColor, shape = CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metrics Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Column 1: Last Location & Last Update
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Last Location:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stats.formattedLastLocationTime,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Last Update:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stats.timeSinceLastUpdateText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Column 2: Interval, Successful, Failed
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Interval:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stats.intervalLabel,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Successful:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${stats.successfulCount}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = StatusOnText
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Failed:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${stats.failedCount}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (stats.failedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // GPS Status Pill / Bar
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (stats.gpsStatus.isHealthy) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (stats.gpsStatus.isHealthy) Icons.Default.GpsFixed else Icons.Default.GpsOff,
                        contentDescription = null,
                        tint = if (stats.gpsStatus.isHealthy) StatusOnText else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GPS: ${stats.gpsStatus.label}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (stats.gpsStatus.isHealthy) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                    )
                }

                if (stats.gpsStatus == GpsHealthStatus.DISABLED) {
                    Text(
                        text = "Enable",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Battery Optimization Notice
            if (!stats.isIgnoringBatteryOptimizations) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Battery optimization may stop background location tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                context.startActivity(fallbackIntent)
                            }
                        },
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Text("Exempt", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
