package com.travelhistory.app.ui.settings

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.travelhistory.app.TravelHistoryApplication
import com.travelhistory.app.data.IntervalPreferences
import com.travelhistory.app.data.backup.BackupManager
import com.travelhistory.app.data.backup.BackupPayload
import com.travelhistory.app.data.backup.BackupValidationResult
import com.travelhistory.app.data.backup.RestoreResult
import com.travelhistory.app.data.backup.RestoreStrategy
import com.travelhistory.app.security.AppLockManager
import com.travelhistory.app.security.BiometricAvailability
import com.travelhistory.app.ui.home.HomeViewModel
import com.travelhistory.app.ui.settings.components.BatteryOptimizationDialog
import com.travelhistory.app.ui.settings.components.CreateBackupDialog
import com.travelhistory.app.ui.settings.components.DecryptBackupDialog
import com.travelhistory.app.ui.settings.components.ExportDataDialog
import com.travelhistory.app.ui.settings.components.IntervalSelectionDialog
import com.travelhistory.app.ui.settings.components.RestoreConfirmDialog
import com.travelhistory.app.ui.settings.components.SettingsItem
import com.travelhistory.app.ui.settings.components.SettingsSection
import com.travelhistory.app.ui.theme.StatusOffText
import com.travelhistory.app.ui.theme.StatusOnText
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = viewModel(),
    onNavigateToOfflineMaps: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val uiState by homeViewModel.uiState.collectAsState()
    val intervalPreferences = remember { IntervalPreferences(context) }

    var showIntervalDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showBatteryDialog by remember { mutableStateOf(false) }

    // App Lock State
    var appLockEnabled by remember { mutableStateOf(AppLockManager.isAppLockEnabled(context)) }
    var showBiometricErrorDialog by remember { mutableStateOf(false) }
    var biometricErrorMessage by remember { mutableStateOf("") }

    // Backup & Restore State
    var lastBackupTimestamp by remember { mutableStateOf(BackupManager.getLastBackupTimestamp(context)) }
    var showCreateBackupDialog by remember { mutableStateOf(false) }
    var pendingBackupPassword by remember { mutableStateOf<String?>(null) }

    var showDecryptDialog by remember { mutableStateOf(false) }
    var decryptErrorMessage by remember { mutableStateOf<String?>(null) }
    var pendingEncryptedBytes by remember { mutableStateOf<ByteArray?>(null) }

    var showRestoreConfirmDialog by remember { mutableStateOf(false) }
    var pendingRestorePayload by remember { mutableStateOf<BackupPayload?>(null) }
    var pendingIsEncrypted by remember { mutableStateOf(false) }

    var showRestoreErrorDialog by remember { mutableStateOf(false) }
    var restoreErrorMessage by remember { mutableStateOf("") }

    val resumeAfterReboot by homeViewModel.resumeAfterReboot.collectAsState()

    // SAF Document Creator launcher for saving backup
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val app = context.applicationContext as? TravelHistoryApplication
                    val repo = app?.locationRepository
                    if (repo != null) {
                        val payload = BackupManager.createBackupPayload(repo, intervalPreferences)
                        val bytes = BackupManager.serializeBackup(payload, pendingBackupPassword)
                        val success = BackupManager.writeBackupToUri(context, uri, bytes)
                        if (success) {
                            val now = System.currentTimeMillis()
                            BackupManager.setLastBackupTimestamp(context, now)
                            lastBackupTimestamp = now
                            Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to write backup file", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Backup failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    pendingBackupPassword = null
                }
            }
        } else {
            pendingBackupPassword = null
        }
    }

    // SAF Document Picker launcher for selecting backup to restore
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val bytes = BackupManager.readBackupFromUri(context, uri)
                    if (bytes == null || bytes.isEmpty()) {
                        restoreErrorMessage = "Selected backup file is empty or unreadable."
                        showRestoreErrorDialog = true
                        return@launch
                    }

                    when (val validation = BackupManager.validateBackup(bytes)) {
                        is BackupValidationResult.RequiresPassword -> {
                            pendingEncryptedBytes = bytes
                            decryptErrorMessage = null
                            showDecryptDialog = true
                        }
                        is BackupValidationResult.Valid -> {
                            pendingRestorePayload = validation.payload
                            pendingIsEncrypted = validation.isEncrypted
                            showRestoreConfirmDialog = true
                        }
                        is BackupValidationResult.Invalid -> {
                            restoreErrorMessage = validation.reason
                            showRestoreErrorDialog = true
                        }
                    }
                } catch (e: Exception) {
                    restoreErrorMessage = e.message ?: "Failed to open backup file."
                    showRestoreErrorDialog = true
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Top Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "App preferences and security options",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Section 1: SECURITY
        SettingsSection(title = "SECURITY") {
            SettingsItem(
                title = "App Lock",
                subtitle = "Fingerprint / Face",
                value = if (appLockEnabled) "ON" else "OFF",
                valueColor = if (appLockEnabled) StatusOnText else StatusOffText,
                showChevron = true,
                showDivider = true,
                onClick = {
                    if (!appLockEnabled) {
                        val avail = AppLockManager.checkBiometricAvailability(context)
                        if (avail is BiometricAvailability.Available) {
                            AppLockManager.setAppLockEnabled(context, true)
                            appLockEnabled = true
                            Toast.makeText(context, "App Lock enabled", Toast.LENGTH_SHORT).show()
                        } else if (avail is BiometricAvailability.Unavailable) {
                            biometricErrorMessage = avail.reason
                            showBiometricErrorDialog = true
                        }
                    } else {
                        AppLockManager.setAppLockEnabled(context, false)
                        appLockEnabled = false
                        Toast.makeText(context, "App Lock disabled", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            SettingsItem(
                title = "Export Protection",
                subtitle = "Biometric required",
                value = "ON",
                valueColor = StatusOnText,
                showChevron = false,
                showDivider = false
            )
        }

        // Section 2: BACKUP & RESTORE
        SettingsSection(title = "BACKUP & RESTORE") {
            SettingsItem(
                title = "Create Backup",
                subtitle = "Save local locations, trips, and settings",
                showChevron = true,
                showDivider = true,
                onClick = {
                    showCreateBackupDialog = true
                }
            )
            SettingsItem(
                title = "Restore Backup",
                subtitle = "Restore history with duplicate protection",
                showChevron = true,
                showDivider = true,
                onClick = {
                    AppLockManager.temporarilySuppressLock()
                    openDocumentLauncher.launch(arrayOf("*/*"))
                }
            )
            SettingsItem(
                title = "Last Backup",
                value = BackupManager.formatLastBackupTimestamp(lastBackupTimestamp),
                showChevron = false,
                showDivider = false
            )
        }

        // Section 3: MAPS & NAVIGATION
        SettingsSection(title = "MAPS & NAVIGATION") {
            SettingsItem(
                title = "Dual-Map Engine",
                subtitle = "Google Maps online, MapLibre offline",
                value = "Auto",
                showChevron = false,
                showDivider = true
            )
            SettingsItem(
                title = "Offline Maps",
                subtitle = "Manage downloaded regions and tile storage",
                showChevron = true,
                showDivider = false,
                onClick = onNavigateToOfflineMaps
            )
        }

        // Section 4: TRACKING
        SettingsSection(title = "TRACKING") {
            SettingsItem(
                title = "Location Tracking",
                value = if (uiState.isTracking) "ON" else "OFF",
                valueColor = if (uiState.isTracking) StatusOnText else StatusOffText,
                showChevron = false,
                showDivider = true
            )
            SettingsItem(
                title = "Recording Interval",
                value = uiState.selectedInterval.label,
                showChevron = false,
                showDivider = true
            )
            SettingsItem(
                title = "Change Interval",
                showChevron = true,
                showDivider = false,
                onClick = { showIntervalDialog = true }
            )
        }

        // Section 5: BACKGROUND TRACKING
        SettingsSection(title = "BACKGROUND TRACKING") {
            SettingsItem(
                title = "Resume after device restart",
                subtitle = "Resume tracking service automatically after reboot",
                value = if (resumeAfterReboot) "ON" else "OFF",
                valueColor = if (resumeAfterReboot) StatusOnText else StatusOffText,
                showChevron = false,
                showDivider = false,
                onClick = {
                    homeViewModel.setResumeAfterReboot(!resumeAfterReboot)
                }
            )
        }

        // Section 6: BATTERY & OPTIMIZATION
        SettingsSection(title = "BATTERY & OPTIMIZATION") {
            val isIgnoringOptimizations = remember {
                com.travelhistory.app.location.LocationTracker(context).isIgnoringBatteryOptimizations()
            }
            SettingsItem(
                title = "Battery Optimization Status",
                subtitle = if (isIgnoringOptimizations) {
                    "Exempted (Background tracking protected)"
                } else {
                    "Optimized (May stop background tracking)"
                },
                value = if (isIgnoringOptimizations) "Exempt" else "Optimized",
                valueColor = if (isIgnoringOptimizations) StatusOnText else StatusOffText,
                showChevron = !isIgnoringOptimizations,
                showDivider = true,
                onClick = {
                    if (!isIgnoringOptimizations) {
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            val fallbackIntent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                            context.startActivity(fallbackIntent)
                        }
                    }
                }
            )
            SettingsItem(
                title = "Battery Optimization Guide",
                subtitle = "How background intervals and OEM settings affect tracking",
                showChevron = true,
                showDivider = false,
                onClick = { showBatteryDialog = true }
            )
        }

        // Section 7: PRIVACY
        SettingsSection(title = "PRIVACY") {
            SettingsItem(
                title = "Location Data Storage",
                subtitle = "Stored locally on device SQLite only",
                value = "Local Only",
                showChevron = false,
                showDivider = true
            )
            SettingsItem(
                title = "Cloud Synchronization",
                subtitle = "Location history is never uploaded to any server",
                value = "Disabled",
                showChevron = false,
                showDivider = false
            )
        }

        // Section 8: DATA
        SettingsSection(title = "DATA") {
            SettingsItem(
                title = "Export Data",
                subtitle = "Export raw GPS records to CSV or JSON",
                showChevron = true,
                showDivider = true,
                onClick = {
                    showExportDialog = true
                }
            )
            SettingsItem(
                title = "Delete History",
                isDestructive = true,
                showChevron = true,
                showDivider = false,
                onClick = {
                    showDeleteConfirmDialog = true
                }
            )
        }
    }

    // Dialogs
    if (showCreateBackupDialog) {
        CreateBackupDialog(
            onDismiss = { showCreateBackupDialog = false },
            onConfirm = { password ->
                showCreateBackupDialog = false
                pendingBackupPassword = password
                val suggestedName = BackupManager.generateSuggestedBackupFileName(password != null)
                AppLockManager.temporarilySuppressLock()
                createDocumentLauncher.launch(suggestedName)
            }
        )
    }

    if (showDecryptDialog && pendingEncryptedBytes != null) {
        DecryptBackupDialog(
            errorMessage = decryptErrorMessage,
            onDismiss = {
                showDecryptDialog = false
                pendingEncryptedBytes = null
            },
            onDecrypt = { pwd ->
                val bytes = pendingEncryptedBytes ?: return@DecryptBackupDialog
                when (val validation = BackupManager.validateBackup(bytes, pwd)) {
                    is BackupValidationResult.Valid -> {
                        showDecryptDialog = false
                        decryptErrorMessage = null
                        pendingRestorePayload = validation.payload
                        pendingIsEncrypted = true
                        showRestoreConfirmDialog = true
                    }
                    is BackupValidationResult.Invalid -> {
                        decryptErrorMessage = validation.reason
                    }
                    is BackupValidationResult.RequiresPassword -> {
                        decryptErrorMessage = "Please enter a valid password."
                    }
                }
            }
        )
    }

    if (showRestoreConfirmDialog && pendingRestorePayload != null) {
        val payload = pendingRestorePayload!!
        RestoreConfirmDialog(
            payload = payload,
            isEncrypted = pendingIsEncrypted,
            onDismiss = {
                showRestoreConfirmDialog = false
                pendingRestorePayload = null
            },
            onConfirm = { strategy ->
                scope.launch {
                    try {
                        val app = context.applicationContext as? TravelHistoryApplication
                        val repo = app?.locationRepository
                        if (repo != null) {
                            val result = BackupManager.restoreBackup(
                                payload = payload,
                                strategy = strategy,
                                locationRepository = repo,
                                intervalPreferences = intervalPreferences
                            )
                            when (result) {
                                is RestoreResult.Success -> {
                                    showRestoreConfirmDialog = false
                                    pendingRestorePayload = null
                                    Toast.makeText(
                                        context,
                                        "Restore successful: ${result.locationsRestored} locations and ${result.tripsRestored} trips restored (${result.duplicatesSkipped} duplicates skipped).",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                is RestoreResult.Failure -> {
                                    restoreErrorMessage = result.error
                                    showRestoreErrorDialog = true
                                }
                            }
                        }
                    } catch (e: Exception) {
                        restoreErrorMessage = e.message ?: "Failed to restore backup."
                        showRestoreErrorDialog = true
                    }
                }
            }
        )
    }

    if (showBiometricErrorDialog) {
        AlertDialog(
            onDismissRequest = { showBiometricErrorDialog = false },
            title = {
                Text(
                    text = "Biometric Security Unavailable",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = biometricErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(onClick = { showBiometricErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showRestoreErrorDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreErrorDialog = false },
            title = {
                Text(
                    text = "Restore Error",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = restoreErrorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            },
            confirmButton = {
                Button(onClick = { showRestoreErrorDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    if (showBatteryDialog) {
        BatteryOptimizationDialog(
            onDismiss = { showBatteryDialog = false }
        )
    }

    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false }
        )
    }

    if (showIntervalDialog) {
        IntervalSelectionDialog(
            currentInterval = uiState.selectedInterval,
            onIntervalSelected = { newInterval ->
                homeViewModel.updateInterval(newInterval)
            },
            onDismiss = { showIntervalDialog = false }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = {
                Text(
                    text = "Delete all location history?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all stored GPS location records from your device. This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        homeViewModel.deleteAllHistory {
                            Toast.makeText(context, "Location history deleted", Toast.LENGTH_SHORT).show()
                        }
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
