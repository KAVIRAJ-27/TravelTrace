package com.travelhistory.app.ui.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travelhistory.app.data.db.LocationRecord
import com.travelhistory.app.data.db.TripRecord
import java.util.Locale

/**
 * Renders detected travel trips in an intuitive, connected vertical timeline.
 * Shows trip start point, intermediate location fixes, end point, and trip metrics.
 */
@Composable
fun TripTimelineView(
    trips: List<TripRecord>,
    records: List<LocationRecord>,
    onTripClick: (TripRecord, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedTrips = trips.sortedBy { it.startTime }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        sortedTrips.forEachIndexed { index, trip ->
            // Extract intermediate points belonging to this trip
            val tripPoints = records.filter { it.timestamp in trip.startTime..trip.endTime }
                .sortedBy { it.timestamp }

            TripTimelineCard(
                trip = trip,
                tripIndex = index + 1,
                tripPoints = tripPoints,
                onClick = { onTripClick(trip, index + 1) }
            )
        }
    }
}

@Composable
private fun TripTimelineCard(
    trip: TripRecord,
    tripIndex: Int,
    tripPoints: List<LocationRecord>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DirectionsCar,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "🚗 TRIP ${String.format(Locale.US, "%02d", tripIndex)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = trip.formattedDistance,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Trip Started Node
            TimelineStepNode(
                timeText = trip.formattedStartTime,
                titleText = "Trip Started",
                coordinatesText = trip.formattedStartCoordinates,
                isStart = true
            )

            // 2. Intermediate points (Show up to 2 intermediate points for clean readability)
            val intermediatePoints = if (tripPoints.size > 2) {
                // Pick middle points
                val step = (tripPoints.size - 2) / 2
                listOfNotNull(
                    tripPoints.getOrNull(1 + step / 2),
                    tripPoints.getOrNull(tripPoints.size - 2)
                ).distinctBy { it.id }
            } else {
                emptyList()
            }

            intermediatePoints.forEach { pt ->
                TimelineConnectorArrow()
                TimelineStepNode(
                    timeText = pt.formattedTime,
                    titleText = "Intermediate Location",
                    coordinatesText = "${pt.formattedLatitude}, ${pt.formattedLongitude}",
                    accuracyText = "±${pt.formattedAccuracy}"
                )
            }

            TimelineConnectorArrow()

            // 3. Trip Ended Node
            TimelineStepNode(
                timeText = trip.formattedEndTime,
                titleText = "Trip Ended",
                coordinatesText = trip.formattedEndCoordinates,
                isEnd = true
            )

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            // Trip Summary Footer: Distance | Duration | Points
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Distance: ${trip.formattedDistance}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Duration: ${trip.formattedDuration}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Points: ${trip.pointCount}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimelineStepNode(
    timeText: String,
    titleText: String,
    coordinatesText: String,
    accuracyText: String? = null,
    isStart: Boolean = false,
    isEnd: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dotColor = when {
            isStart -> MaterialTheme.colorScheme.primary
            isEnd -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.outline
        }

        Box(
            modifier = Modifier
                .size(12.dp)
                .background(dotColor, CircleShape)
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$timeText • $titleText",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (accuracyText != null) {
                    Text(
                        text = accuracyText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = dotColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = coordinatesText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TimelineConnectorArrow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.ArrowDownward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
        )
    }
}
