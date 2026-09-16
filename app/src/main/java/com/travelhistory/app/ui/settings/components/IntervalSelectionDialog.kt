package com.travelhistory.app.ui.settings.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.travelhistory.app.location.TrackingInterval

data class IntervalOption(
    val title: String,
    val minutes: Int,
    val isCustom: Boolean = false
)

private val intervalOptions = listOf(
    IntervalOption("1 minute", 1),
    IntervalOption("5 minutes", 5),
    IntervalOption("10 minutes", 10),
    IntervalOption("15 minutes", 15),
    IntervalOption("30 minutes", 30),
    IntervalOption("1 hour", 60),
    IntervalOption("Custom", 0, isCustom = true)
)

@Composable
fun IntervalSelectionDialog(
    currentInterval: TrackingInterval,
    onIntervalSelected: (TrackingInterval) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember {
        mutableStateOf(
            if (currentInterval.isCustom) {
                intervalOptions.first { it.isCustom }
            } else {
                intervalOptions.find { it.minutes == currentInterval.minutes }
                    ?: intervalOptions.first { it.isCustom }
            }
        )
    }

    var customMinutesInput by remember {
        mutableStateOf(
            if (currentInterval.isCustom) currentInterval.minutes.toString() else "20"
        )
    }

    var customInputError by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    // Determine if current selection triggers a battery consideration warning
    val activeMinutes = if (selectedOption.isCustom) {
        customMinutesInput.toIntOrNull() ?: 0
    } else {
        selectedOption.minutes
    }
    val showBatteryWarning = activeMinutes in 1..5

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    text = "Recording Interval",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose how often the app logs your GPS location:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Battery Consideration Banner
                AnimatedVisibility(visible = showBatteryWarning) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BatteryAlert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Shorter recording intervals may use more battery.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                // Radio Options List
                intervalOptions.forEach { option ->
                    val isSelected = selectedOption == option
                    val itemBg = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                    val itemBorder = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, itemBorder, RoundedCornerShape(12.dp))
                            .background(itemBg)
                            .clickable {
                                selectedOption = option
                                customInputError = null
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedOption = option
                                    customInputError = null
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = option.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }

                        if (option.minutes == 10 && !option.isCustom) {
                            Text(
                                text = "Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }

                // Custom Interval Input Section
                AnimatedVisibility(visible = selectedOption.isCustom) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                    ) {
                        Text(
                            text = "Custom Interval (in Minutes)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = customMinutesInput,
                            onValueChange = { input ->
                                // Allow only digits
                                if (input.all { it.isDigit() }) {
                                    customMinutesInput = input
                                    customInputError = null
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("e.g. 20") },
                            suffix = { Text("Minutes") },
                            singleLine = true,
                            isError = customInputError != null,
                            supportingText = {
                                if (customInputError != null) {
                                    Text(
                                        text = customInputError!!,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                } else {
                                    Text(
                                        text = "Minimum 1 minute. Shorter than 1 min is restricted by Android battery limits.",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { /* validate */ }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedOption.isCustom) {
                        val parsed = customMinutesInput.trim().toIntOrNull()
                        when {
                            parsed == null || customMinutesInput.isBlank() -> {
                                customInputError = "Please enter an interval in minutes."
                            }
                            parsed <= 0 -> {
                                customInputError = "Interval must be at least 1 minute."
                            }
                            parsed > 1440 -> {
                                customInputError = "Interval cannot exceed 1440 minutes (24 hours)."
                            }
                            else -> {
                                onIntervalSelected(TrackingInterval.fromMinutes(parsed, isCustom = true))
                                onDismiss()
                            }
                        }
                    } else {
                        onIntervalSelected(
                            TrackingInterval.fromMinutes(selectedOption.minutes, isCustom = false)
                        )
                        onDismiss()
                    }
                }
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}
