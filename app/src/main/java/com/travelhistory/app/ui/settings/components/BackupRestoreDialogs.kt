package com.travelhistory.app.ui.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelhistory.app.data.backup.BackupPayload
import com.travelhistory.app.data.backup.RestoreStrategy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog prompting for backup creation options, including optional AES-256 password protection.
 */
@Composable
fun CreateBackupDialog(
    onDismiss: () -> Unit,
    onConfirm: (password: String?) -> Unit
) {
    var enableEncryption by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Create Backup",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Backup includes all your local GPS locations, trips, and tracking preferences.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Encryption Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { enableEncryption = !enableEncryption }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = enableEncryption,
                        onCheckedChange = { enableEncryption = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Encrypt with password (AES-256)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Protects sensitive location history",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (enableEncryption) {
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            validationError = null
                        },
                        label = { Text("Backup Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility"
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            validationError = null
                        },
                        label = { Text("Confirm Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "If you forget this password, the backup cannot be recovered.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Unencrypted backups contain readable location data. Keep your backup file in a safe place.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }

                if (validationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (enableEncryption) {
                        if (password.isBlank()) {
                            validationError = "Please enter a password."
                            return@Button
                        }
                        if (password != confirmPassword) {
                            validationError = "Passwords do not match."
                            return@Button
                        }
                        onConfirm(password)
                    } else {
                        onConfirm(null)
                    }
                }
            ) {
                Text("Select Save Location")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog prompting for password to decrypt an encrypted backup file during restore.
 */
@Composable
fun DecryptBackupDialog(
    errorMessage: String? = null,
    onDismiss: () -> Unit,
    onDecrypt: (password: String) -> Unit
) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Encrypted Backup", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "This backup is encrypted with AES-256. Enter the password you used when creating it to proceed.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility"
                            )
                        }
                    }
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (password.isNotBlank()) {
                        onDecrypt(password)
                    }
                },
                enabled = password.isNotBlank()
            ) {
                Text("Unlock Backup")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog showing validated backup summary and prompting user to select Merge vs Replace.
 */
@Composable
fun RestoreConfirmDialog(
    payload: BackupPayload,
    isEncrypted: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (strategy: RestoreStrategy) -> Unit
) {
    var selectedStrategy by remember { mutableStateOf(RestoreStrategy.MERGE) }
    val formattedDate = remember(payload.createdAt) {
        SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault()).format(Date(payload.createdAt))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Restore Backup", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Summary Card
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Backup Details",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Created: $formattedDate",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Locations: ${payload.locations.size} fixes",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Trips: ${payload.trips.size} trips",
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (isEncrypted) {
                        Text(
                            text = "Security: AES-256 Encrypted",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Select Restore Strategy:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Strategy 1: MERGE (Default)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = if (selectedStrategy == RestoreStrategy.MERGE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedStrategy = RestoreStrategy.MERGE }
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = selectedStrategy == RestoreStrategy.MERGE,
                        onClick = { selectedStrategy = RestoreStrategy.MERGE }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Merge with existing data (Recommended)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Inserts new records and skips duplicates. Your existing history is preserved.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Strategy 2: REPLACE
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = if (selectedStrategy == RestoreStrategy.REPLACE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedStrategy = RestoreStrategy.REPLACE }
                        .padding(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    RadioButton(
                        selected = selectedStrategy == RestoreStrategy.REPLACE,
                        onClick = { selectedStrategy = RestoreStrategy.REPLACE }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Replace existing data",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Wipes current history and replaces it with the contents of this backup.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedStrategy) },
                colors = if (selectedStrategy == RestoreStrategy.REPLACE) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(if (selectedStrategy == RestoreStrategy.REPLACE) "Replace & Restore" else "Merge & Restore")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
