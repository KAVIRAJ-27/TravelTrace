package com.travelhistory.app.ui.settings

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.travelhistory.app.TravelHistoryApplication
import com.travelhistory.app.data.export.ExportFormat
import com.travelhistory.app.data.export.ExportRangeType
import com.travelhistory.app.data.export.ExportResult
import com.travelhistory.app.data.export.LocationExporter
import com.travelhistory.app.security.BiometricAuthManager
import com.travelhistory.app.security.BiometricAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State machine for the export process.
 */
sealed class ExportStatus {
    data object Idle : ExportStatus()
    data object Authenticating : ExportStatus()
    data object Exporting : ExportStatus()
    data class Success(val result: ExportResult) : ExportStatus()
    data class Error(val message: String) : ExportStatus()
    data object Cancelled : ExportStatus()
}

/**
 * UI State for the export options dialog.
 */
data class ExportUiState(
    val format: ExportFormat = ExportFormat.CSV,
    val rangeType: ExportRangeType = ExportRangeType.TODAY,
    val selectedDateMillis: Long = System.currentTimeMillis(),
    val startDateMillis: Long = System.currentTimeMillis(),
    val endDateMillis: Long = System.currentTimeMillis(),
    val status: ExportStatus = ExportStatus.Idle,
    val validationError: String? = null
) {
    val isProcessing: Boolean
        get() = status is ExportStatus.Authenticating || status is ExportStatus.Exporting
}

class ExportViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as TravelHistoryApplication).locationRepository

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun setFormat(format: ExportFormat) {
        _uiState.update { it.copy(format = format, validationError = null) }
    }

    fun setRangeType(rangeType: ExportRangeType) {
        _uiState.update { it.copy(rangeType = rangeType, validationError = null) }
    }

    fun setSelectedDate(millis: Long) {
        _uiState.update { it.copy(selectedDateMillis = millis, validationError = null) }
    }

    fun setStartDate(millis: Long) {
        _uiState.update { current ->
            val error = if (millis > current.endDateMillis) "Start date cannot be after end date." else null
            current.copy(startDateMillis = millis, validationError = error)
        }
    }

    fun setEndDate(millis: Long) {
        _uiState.update { current ->
            val error = if (current.startDateMillis > millis) "Start date cannot be after end date." else null
            current.copy(endDateMillis = millis, validationError = error)
        }
    }

    fun resetStatus() {
        _uiState.update { it.copy(status = ExportStatus.Idle, validationError = null) }
    }

    /**
     * Initiates the secure export sequence.
     *
     * IMPORTANT:
     * 1. Validates range.
     * 2. Checks biometric capability.
     * 3. Prompts user for biometric authentication.
     * 4. Queries database ONLY after authentication succeeds.
     */
    fun startExport(activity: FragmentActivity) {
        val current = _uiState.value

        // Validate Date Range
        if (current.rangeType == ExportRangeType.DATE_RANGE && current.startDateMillis > current.endDateMillis) {
            _uiState.update { it.copy(validationError = "Start date cannot be after end date.") }
            return
        }

        // Check biometric availability
        val availability = BiometricAuthManager.checkAvailability(activity)
        if (availability is BiometricAvailability.Unavailable) {
            _uiState.update { it.copy(status = ExportStatus.Error(availability.reason)) }
            return
        }

        // Trigger BiometricPrompt
        _uiState.update { it.copy(status = ExportStatus.Authenticating, validationError = null) }

        BiometricAuthManager.authenticate(
            activity = activity,
            title = "Authenticate to export your location history.",
            subtitle = "Verify your identity to proceed with data export",
            onSuccess = {
                proceedWithExport()
            },
            onError = { errorMsg ->
                _uiState.update { it.copy(status = ExportStatus.Error(errorMsg)) }
            },
            onCancel = {
                _uiState.update { it.copy(status = ExportStatus.Cancelled) }
            }
        )
    }

    private fun proceedWithExport() {
        _uiState.update { it.copy(status = ExportStatus.Exporting) }

        viewModelScope.launch {
            try {
                val current = _uiState.value
                val records = repository.getRecordsForExport(
                    rangeType = current.rangeType,
                    startMillis = if (current.rangeType == ExportRangeType.SELECTED_DATE) current.selectedDateMillis else current.startDateMillis,
                    endMillis = current.endDateMillis
                )

                if (records.isEmpty()) {
                    _uiState.update {
                        it.copy(status = ExportStatus.Error("No location records found for the selected range."))
                    }
                    return@launch
                }

                val result = LocationExporter.exportRecords(
                    context = getApplication(),
                    records = records,
                    format = current.format,
                    rangeType = current.rangeType,
                    startMillis = if (current.rangeType == ExportRangeType.SELECTED_DATE) current.selectedDateMillis else current.startDateMillis,
                    endMillis = current.endDateMillis
                )

                _uiState.update { it.copy(status = ExportStatus.Success(result)) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(status = ExportStatus.Error("Failed to export location data: ${e.localizedMessage ?: "Unknown error"}"))
                }
            }
        }
    }
}
