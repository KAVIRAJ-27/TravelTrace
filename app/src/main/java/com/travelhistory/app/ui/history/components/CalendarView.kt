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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelhistory.app.ui.history.CalendarMonthState

@Composable
fun CalendarView(
    monthState: CalendarMonthState,
    selectedDay: Int,
    daysWithRecords: Set<Int>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            // Month & Year Header with Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Previous Month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Text(
                    text = monthState.monthYearLabel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Next Month",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Weekday labels (Su Mo Tu We Th Fr Sa)
            val weekDays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            val totalCells = monthState.firstDayOfWeekOffset + monthState.daysInMonth
            val rows = (totalCells + 6) / 7

            for (rowIndex in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (colIndex in 0 until 7) {
                        val cellIndex = rowIndex * 7 + colIndex
                        val dayNumber = cellIndex - monthState.firstDayOfWeekOffset + 1

                        if (cellIndex < monthState.firstDayOfWeekOffset || dayNumber > monthState.daysInMonth) {
                            // Blank cell
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            val isSelected = selectedDay == dayNumber
                            val hasRecords = daysWithRecords.contains(dayNumber)
                            val isFuture = monthState.isFutureDate(dayNumber)

                            CalendarDayCell(
                                day = dayNumber,
                                isSelected = isSelected,
                                hasRecords = hasRecords,
                                isFuture = isFuture,
                                onClick = { onDaySelected(dayNumber) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isSelected: Boolean,
    hasRecords: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || hasRecords) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    hasRecords -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        // Indicator dot for recorded history
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(5.dp)
                .background(
                    color = if (hasRecords) {
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    shape = CircleShape
                )
        )
    }
}
