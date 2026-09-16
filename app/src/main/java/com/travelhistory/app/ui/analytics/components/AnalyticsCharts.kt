package com.travelhistory.app.ui.analytics.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelhistory.app.data.analytics.DayDistanceStat
import com.travelhistory.app.data.analytics.WeekDistanceStat
import java.util.Locale

/**
 * Clean Canvas-based bar chart for daily distance across the 7 days of a week.
 */
@Composable
fun DailyDistanceBarChartCard(
    dailyStats: List<DayDistanceStat>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Route,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Daily Distance (This Week)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val totalKm = dailyStats.sumOf { it.distanceMeters } / 1000.0
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f km", totalKm),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxDistance = (dailyStats.maxOfOrNull { it.distanceMeters } ?: 0.0) / 1000.0
            if (maxDistance <= 0.0) {
                EmptyChartNotice(message = "No travel distance recorded for this week.")
            } else {
                CanvasBarChart(
                    items = dailyStats.map {
                        BarItem(
                            label = it.dayOfWeekLabel,
                            value = (it.distanceMeters / 1000.0).toFloat(),
                            valueFormatted = if (it.distanceMeters > 0) String.format(Locale.US, "%.1f", it.distanceMeters / 1000.0) else ""
                        )
                    },
                    barColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        }
    }
}

/**
 * Clean Canvas-based bar chart for weekly distance across weeks of the month.
 */
@Composable
fun MonthlyTravelChartCard(
    weeklyStats: List<WeekDistanceStat>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Monthly Travel by Week",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val totalKm = weeklyStats.sumOf { it.distanceMeters } / 1000.0
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f km", totalKm),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxDistance = (weeklyStats.maxOfOrNull { it.distanceMeters } ?: 0.0) / 1000.0
            if (maxDistance <= 0.0) {
                EmptyChartNotice(message = "No travel distance recorded for this month.")
            } else {
                CanvasBarChart(
                    items = weeklyStats.map {
                        BarItem(
                            label = it.weekLabel,
                            value = (it.distanceMeters / 1000.0).toFloat(),
                            valueFormatted = if (it.distanceMeters > 0) String.format(Locale.US, "%.1f", it.distanceMeters / 1000.0) else ""
                        )
                    },
                    barColor = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        }
    }
}

/**
 * Clean Canvas-based bar chart for trip counts across the 7 days of the week.
 */
@Composable
fun TripCountBarChartCard(
    dailyStats: List<DayDistanceStat>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsCar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Trip Count (This Week)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                val totalTrips = dailyStats.sumOf { it.tripCount }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$totalTrips trips",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val maxTrips = dailyStats.maxOfOrNull { it.tripCount } ?: 0
            if (maxTrips <= 0) {
                EmptyChartNotice(message = "No trips recorded for this week.")
            } else {
                CanvasBarChart(
                    items = dailyStats.map {
                        BarItem(
                            label = it.dayOfWeekLabel,
                            value = it.tripCount.toFloat(),
                            valueFormatted = if (it.tripCount > 0) it.tripCount.toString() else ""
                        )
                    },
                    barColor = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
            }
        }
    }
}

private data class BarItem(
    val label: String,
    val value: Float,
    val valueFormatted: String
)

@Composable
private fun CanvasBarChart(
    items: List<BarItem>,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val textPaintColor = MaterialTheme.colorScheme.onSurfaceVariant.hashCode()
    val valuePaintColor = MaterialTheme.colorScheme.onSurface.hashCode()
    val baselineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Canvas(modifier = modifier) {
        if (items.isEmpty()) return@Canvas

        val maxValue = items.maxOfOrNull { it.value }?.takeIf { it > 0f } ?: 1f
        val chartHeight = size.height - 30.dp.toPx() // Reserve 30dp at bottom for labels
        val chartTop = 20.dp.toPx() // Reserve top for value labels
        val availableBarHeight = chartHeight - chartTop
        val slotWidth = size.width / items.size
        val barWidth = (slotWidth * 0.45f).coerceAtMost(28.dp.toPx())

        // Draw baseline
        drawLine(
            color = baselineColor,
            start = Offset(0f, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 1.dp.toPx()
        )

        val paint = android.graphics.Paint().apply {
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 11.sp.toPx()
            isAntiAlias = true
        }

        items.forEachIndexed { index, item ->
            val centerX = (index * slotWidth) + (slotWidth / 2f)
            val barHeight = if (maxValue > 0) (item.value / maxValue) * availableBarHeight else 0f
            val top = chartHeight - barHeight
            val left = centerX - (barWidth / 2f)

            if (barHeight > 0f) {
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Value above bar
                if (item.valueFormatted.isNotEmpty()) {
                    paint.color = valuePaintColor
                    paint.isFakeBoldText = true
                    drawContext.canvas.nativeCanvas.drawText(
                        item.valueFormatted,
                        centerX,
                        top - 4.dp.toPx(),
                        paint
                    )
                }
            } else {
                // Dim point for zero-value
                drawCircle(
                    color = barColor.copy(alpha = 0.25f),
                    radius = 2.dp.toPx(),
                    center = Offset(centerX, chartHeight - 4.dp.toPx())
                )
            }

            // X-axis label
            paint.color = textPaintColor
            paint.isFakeBoldText = false
            drawContext.canvas.nativeCanvas.drawText(
                item.label,
                centerX,
                chartHeight + 18.dp.toPx(),
                paint
            )
        }
    }
}

@Composable
private fun EmptyChartNotice(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
