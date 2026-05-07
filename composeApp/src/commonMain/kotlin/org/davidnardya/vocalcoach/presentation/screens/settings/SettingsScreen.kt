package org.davidnardya.vocalcoach.presentation.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.davidnardya.vocalcoach.core.util.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    permissionManager: PermissionManager,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val draftState by viewModel.draftState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showTimePicker by remember { mutableStateOf(false) }
    var showPermissionRequest by remember { mutableStateOf(false) }
    var permissionRequestForTestReminder by remember { mutableStateOf(false) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showPermissionExplanation by remember { mutableStateOf(false) }

    val hasChanges = state != draftState

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SettingsEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.message)
                is SettingsEvent.ShowPermissionExplanation -> showPermissionExplanation = true
            }
        }
    }

    if (showPermissionRequest) {
        permissionManager.AskForNotificationPermission { _ ->
            //todo - why no use?
            showPermissionRequest = false
            scope.launch {
                val hasPermission = permissionManager.hasNotificationPermission()
                if (permissionRequestForTestReminder) {
                    viewModel.onSendTestReminderClicked(hasPermission)
                    permissionRequestForTestReminder = false
                } else {
                    viewModel.onSaveClicked(hasPermission)
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            if (hasChanges) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val hasPermission = permissionManager.hasNotificationPermission()
                                if (draftState.enabled && !hasPermission) {
                                    //todo - why no use?
                                    permissionRequestForTestReminder = false
                                    showPermissionRequest = true
                                } else {
                                    viewModel.onSaveClicked(hasPermission)
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("SAVE CHANGES")
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Notification Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Reminders", style = MaterialTheme.typography.bodyLarge)
                Switch(
                    checked = draftState.enabled,
                    onCheckedChange = { viewModel.onEnabledChanged(it) }
                )
            }

            if (draftState.enabled) {
                // Time Picker
                Column {
                    Text("Reminder Time", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val formattedTime = remember(draftState.hour, draftState.minute) {
                            "${draftState.hour.toString().padStart(2, '0')}:${draftState.minute.toString().padStart(2, '0')}"
                        }
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Button(onClick = { showTimePicker = true }) {
                            Text("Change Time")
                        }
                    }
                }

                // Custom Interval Picker
                Column {
                    Text("Frequency (Hours)", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = draftState.intervalHours.toString(),
                        onValueChange = {
                            val newValue = it.toLongOrNull()
                            if (newValue != null && newValue > 0) {
                                viewModel.onIntervalChanged(newValue)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Interval in hours") }
                    )
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val hasPermission = permissionManager.hasNotificationPermission()
                            if (!hasPermission) {
                                permissionRequestForTestReminder = true
                                showPermissionRequest = true
                            } else {
                                viewModel.onSendTestReminderClicked(hasPermission)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Send Test Reminder Now")
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Reset Button
            OutlinedButton(
                onClick = { showResetConfirmation = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.error))
            ) {
                Text("Reset All Progress")
            }
        }
    }

    if (showTimePicker) {
        TimePickerDialog(
            initialHour = draftState.hour,
            initialMinute = draftState.minute,
            onConfirm = { hour, minute ->
                viewModel.onTimeChanged(hour, minute)
                showTimePicker = false//todo - why no use?
            },
            onDismiss = { showTimePicker = false }//todo - why no use?
        )
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },//todo - why no use?
            title = { Text("Reset Progress?") },
            text = { Text("Are you sure? This will delete your total score and reset your level to 1. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetProgress()
                        showResetConfirmation = false//todo - why no use?
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Yes, reset progress")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {//todo - why no use?
                    Text("No, keep progress")
                }
            }
        )
    }

    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },//todo - why no use?
            title = { Text("Notifications Disabled") },
            text = { Text("Reminders are enabled, but notification permissions are denied. Please enable them in System Settings to receive alerts.") },
            confirmButton = {
                TextButton(onClick = { showPermissionExplanation = false }) {//todo - why no use?
                    Text("OK")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Select Reminder Time") },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = timePickerState)
            }
        }
    )
}
