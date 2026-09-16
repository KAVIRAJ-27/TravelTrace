package com.travelhistory.app.ui.settings.components

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelhistory.app.data.export.ExportFormat
import com.travelhistory.app.data.export.ExportRangeType
import com.travelhistory.app.data.export.ExportResult
import com.travelhistory.app.ui.settings.ExportStatus
import com.travelhistory.app.ui.settings.ExportViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ExportDataDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    exportViewModel: ExportViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val uiState by exportViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = {
            if (!uiState.isProcessing) {
                exportViewModel.resetStatus()
                onDismiss()
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Export Location History",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                when (val status = uiState.status) {
                    is ExportStatus.Success -> {
                        ExportSuccessView(
                            result = status.result,
                            context = context,
                            onDone = {
                                exportViewModel.resetStatus()
                                onDismiss()
                            }
                        )
                    }
                    else -> {
                        // 1. Choose Format
                        Text(
                            text = "Choose Format",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExportFormat.entries.forEach { format ->
                                val isSelected = uiState.format == format
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(enabled = !uiState.isProcessing) {
                                            exportViewModel.setFormat(format)
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = format.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 2. Choose Range
                        Text(
                            text = "Export Range",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ExportRangeType.entries.forEach { rangeType ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !uiState.isProcessing) {
                                        exportViewModel.setRangeType(rangeType)
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.rangeType == rangeType,
                                    onClick = { exportViewModel.setRangeType(rangeType) },
                                    enabled = !uiState.isProcessing
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = rangeType.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // 3. Date Selectors for SELECTED_DATE and DATE_RANGE
                        AnimatedVisibility(visible = uiState.rangeType == ExportRangeType.SELECTED_DATE) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = "Date:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                DateSelectorButton(
                                    millis = uiState.selectedDateMillis,
                                    onDateSelected = { exportViewModel.setSelectedDate(it) },
                                    context = context,
                                    enabled = !uiState.isProcessing
                                )
                            }
                        }

                        AnimatedVisibility(visible = uiState.rangeType == ExportRangeType.DATE_RANGE) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Start Date:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        DateSelectorButton(
                                            millis = uiState.startDateMillis,
                                            onDateSelected = { exportViewModel.setStartDate(it) },
                                            context = context,
                                            enabled = !uiState.isProcessing
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "End Date:",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        DateSelectorButton(
                                            millis = uiState.endDateMillis,
                                            onDateSelected = { exportViewModel.setEndDate(it) },
                                            context = context,
                                            enabled = !uiState.isProcessing
                                        )
                                    }
                                }
                            }
                        }

                        // Validation Error
                        if (uiState.validationError != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = uiState.validationError ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Status Feedback
                        when (status) {
                            is ExportStatus.Authenticating -> {
                                StatusProgressRow(text = "Authenticating...")
                            }
                            is ExportStatus.Exporting -> {
                                StatusProgressRow(text = "Creating file...")
                            }
                            is ExportStatus.Cancelled -> {
                                StatusMessageRow(
                                    text = "Authentication cancelled.",
                                    isError = false
                                )
                            }
                            is ExportStatus.Error -> {
                                StatusMessageRow(
                                    text = status.message,
                                    isError = true
                                )
                            }
                            else -> {}
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (uiState.status !is ExportStatus.Success) {
                Button(
                    onClick = {
                        activity?.let { exportViewModel.startExport(it) }
                    },
                    enabled = !uiState.isProcessing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Authenticate & Export")
                }
            }
        },
        dismissButton = {
            if (uiState.status !is ExportStatus.Success) {
                TextButton(
                    onClick = {
                        exportViewModel.resetStatus()
                        onDismiss()
                    },
                    enabled = !uiState.isProcessing
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}

@Composable
private fun DateSelectorButton(
    millis: Long,
    onDateSelected: (Long) -> Unit,
    context: Context,
    enabled: Boolean
) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled) {
                val cal = Calendar.getInstance().apply { timeInMillis = millis }
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedCal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            set(Calendar.DAY_OF_MONTH, dayOfMonth)
                        }
                        onDateSelected(selectedCal.timeInMillis)
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = dateStr,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun StatusProgressRow(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun StatusMessageRow(text: String, isError: Boolean) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ExportSuccessView(
    result: ExportResult,
    context: Context,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Export Successful",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${result.recordCount} location records exported",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = result.fileName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons: [ Share ] and [ Save / Open ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = result.format.mimeType
                        putExtra(Intent.EXTRA_STREAM, result.contentUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Travel History Export (${result.fileName})")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share Location History"))
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Share")
            }

            OutlinedButton(
                onClick = {
                    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(result.contentUri, result.format.mimeType)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        context.startActivity(Intent.createChooser(viewIntent, "Open Export File"))
                    } catch (e: Exception) {
                        // Fallback to share intent if no viewer app exists
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = result.format.mimeType
                            putExtra(Intent.EXTRA_STREAM, result.contentUri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Save Location History"))
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save / Open")
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        TextButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}
