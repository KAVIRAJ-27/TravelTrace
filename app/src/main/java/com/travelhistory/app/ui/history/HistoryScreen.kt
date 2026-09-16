package com.travelhistory.app.ui.history

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.HistoryToggleOff
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelhistory.app.ui.analytics.components.DailyAnalyticsCard
import com.travelhistory.app.ui.analytics.components.DailyDistanceBarChartCard
import com.travelhistory.app.ui.analytics.components.MonthlyAnalyticsCard
import com.travelhistory.app.ui.analytics.components.MonthlyTravelChartCard
import com.travelhistory.app.ui.analytics.components.TripCountBarChartCard
import com.travelhistory.app.ui.analytics.components.WeeklyAnalyticsCard
import com.travelhistory.app.ui.history.components.CalendarView
import com.travelhistory.app.ui.history.components.LocationDetailDialog
import com.travelhistory.app.ui.history.components.LocationTimelineView
import com.travelhistory.app.ui.history.components.TripDetailDialog
import com.travelhistory.app.ui.history.components.TripTimelineView
import java.util.Calendar

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = viewModel(),
    onNavigateToMap: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by historyViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    var showDeleteDayDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Travel Trace",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Timeline & Travel Analytics",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick "Today" jump button (Filter by today's date)
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val todayCal = Calendar.getInstance()
                        val todayMonth = CalendarMonthState(
                            year = todayCal.get(Calendar.YEAR),
                            month = todayCal.get(Calendar.MONTH)
                        )
                        val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)
                        historyViewModel.selectDay(todayDay)
                    },
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Jump to Today",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Calendar Grid with recorded dates indicator dots
        CalendarView(
            monthState = uiState.calendarMonth,
            selectedDay = uiState.selectedDayOfMonth,
            daysWithRecords = uiState.daysWithRecords,
            onPreviousMonth = { historyViewModel.previousMonth() },
            onNextMonth = { historyViewModel.nextMonth() },
            onDaySelected = { historyViewModel.selectDay(it) }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Mode Switcher Tabs: Timeline vs Analytics
        TabRow(
            selectedTabIndex = uiState.selectedTab.ordinal,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
        ) {
            Tab(
                selected = uiState.selectedTab == HistoryTab.TIMELINE,
                onClick = { historyViewModel.setTab(HistoryTab.TIMELINE) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Timeline",
                            fontWeight = if (uiState.selectedTab == HistoryTab.TIMELINE) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )

            Tab(
                selected = uiState.selectedTab == HistoryTab.ANALYTICS,
                onClick = { historyViewModel.setTab(HistoryTab.ANALYTICS) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Analytics",
                            fontWeight = if (uiState.selectedTab == HistoryTab.ANALYTICS) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (uiState.selectedTab == HistoryTab.TIMELINE) {
            // ==========================================
            // TIMELINE VIEW (Trips Timeline + Location Timeline)
            // ==========================================
            if (uiState.hasRecords) {
                // Day Summary Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.formattedSelectedDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            IconButton(
                                onClick = { showDeleteDayDialog = true },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "Delete Day's History",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Row 1: Points & Trips Count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.FormatListNumbered,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Points",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${uiState.totalRecordsCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.DirectionsCar,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Trips",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${uiState.tripsCount}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Row 2: Distance & Duration
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Route,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Distance",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.formattedDistanceText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Duration",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.formattedDurationText,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Button: View Journey on Map
                        Button(
                            onClick = { onNavigateToMap(uiState.selectedDateMillis) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Map,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "View Journey on Map",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Timeline Sub-Filter Chips (Trips vs Location Timeline)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = uiState.timelineSubTab == TimelineSubTab.TRIPS,
                        onClick = { historyViewModel.setTimelineSubTab(TimelineSubTab.TRIPS) },
                        label = { Text("Trip Timeline (${uiState.dayTrips.size})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.DirectionsCar,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )

                    FilterChip(
                        selected = uiState.timelineSubTab == TimelineSubTab.LOCATIONS,
                        onClick = { historyViewModel.setTimelineSubTab(TimelineSubTab.LOCATIONS) },
                        label = { Text("Location Timeline (${uiState.selectedDateRecords.size})") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.MyLocation,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.timelineSubTab == TimelineSubTab.TRIPS) {
                    if (uiState.dayTrips.isNotEmpty()) {
                        TripTimelineView(
                            trips = uiState.dayTrips,
                            records = uiState.selectedDateRecords,
                            onTripClick = { trip, index ->
                                historyViewModel.selectTripForDetail(trip, index)
                            }
                        )
                    } else {
                        // Empty trips state
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No trips detected for this date.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Locations were recorded, but sustained movement meeting trip criteria was not detected. Check Location Timeline to view individual GPS points.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // Location Timeline
                    LocationTimelineView(
                        records = uiState.selectedDateRecords,
                        onRecordClick = { record ->
                            historyViewModel.selectLocationForDetail(record)
                        }
                    )
                }
            } else {
                // Empty state for selected date
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.HistoryToggleOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "No travel data recorded for this date.",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "No GPS fixes or travel trips were recorded on ${uiState.formattedSelectedDate}.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // ==========================================
            // ANALYTICS VIEW
            // ==========================================
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                // 1. Daily Analytics Card
                DailyAnalyticsCard(analytics = uiState.dailyAnalytics)

                // 2. Daily Distance Chart (This Week)
                DailyDistanceBarChartCard(dailyStats = uiState.weeklyAnalytics.dailyStats)

                // 3. Trip Count Chart (This Week)
                TripCountBarChartCard(dailyStats = uiState.weeklyAnalytics.dailyStats)

                // 4. Weekly Analytics Card
                WeeklyAnalyticsCard(analytics = uiState.weeklyAnalytics)

                // 5. Monthly Travel by Week Chart
                MonthlyTravelChartCard(weeklyStats = uiState.monthlyAnalytics.weeklyBreakdown)

                // 6. Monthly Analytics Card
                MonthlyAnalyticsCard(analytics = uiState.monthlyAnalytics)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Location Detail Dialog (Tapped GPS Record)
    uiState.selectedLocationForDetail?.let { record ->
        LocationDetailDialog(
            record = record,
            onDismiss = { historyViewModel.selectLocationForDetail(null) }
        )
    }

    // Trip Detail Dialog (Tapped Trip)
    uiState.selectedTripForDetail?.let { trip ->
        TripDetailDialog(
            trip = trip,
            tripIndex = uiState.selectedTripIndex,
            onViewRoute = { selectedTrip ->
                onNavigateToMap(selectedTrip.startTime)
            },
            onDismiss = { historyViewModel.selectTripForDetail(null) }
        )
    }

    // Delete Day Confirmation Dialog
    if (showDeleteDayDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDayDialog = false },
            title = {
                Text(
                    text = "Delete Day's History?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Delete all location records for ${uiState.formattedSelectedDate}? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        historyViewModel.deleteSelectedDayHistory {
                            Toast.makeText(context, "Records deleted for date", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteDayDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDayDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
