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
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.travelhistory.app.data.db.LocationRecord

/**
 * Chronological vertical timeline of recorded raw GPS location fixes.
 */
@Composable
fun LocationTimelineView(
    records: List<LocationRecord>,
    onRecordClick: (LocationRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedRecords = records.sortedBy { it.timestamp }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        sortedRecords.forEachIndexed { index, record ->
            val isFirst = index == 0
            val isLast = index == sortedRecords.lastIndex

            LocationTimelineItem(
                record = record,
                isFirst = isFirst,
                isLast = isLast,
                onClick = { onRecordClick(record) }
            )
        }
    }
}

@Composable
private fun LocationTimelineItem(
    record: LocationRecord,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Vertical Timeline Spine (Node + Line)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            // Top connecting connector if not first
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(if (isFirst) 8.dp else 12.dp)
                    .background(
                        if (isFirst) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
            )

            // Timeline Node Circle
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                )
            }

            // Bottom connecting line if not last
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(64.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Content Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = record.formattedTime,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Accuracy: ${record.formattedAccuracy}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${record.formattedLatitude}, ${record.formattedLongitude}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
