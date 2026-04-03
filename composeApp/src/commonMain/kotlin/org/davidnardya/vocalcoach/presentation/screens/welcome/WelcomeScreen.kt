package org.davidnardya.vocalcoach.presentation.screens.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.davidnardya.vocalcoach.core.util.PermissionManager
import org.davidnardya.vocalcoach.di.createScoreRepository
import org.davidnardya.vocalcoach.di.createStreakManagementUseCase
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun WelcomeScreen(
    permissionManager: PermissionManager,
    onStartGame: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: WelcomeViewModel = viewModel {
        WelcomeViewModel(
            createScoreRepository(),
            createStreakManagementUseCase(),
        )
    },
) {
    val stats by viewModel.playerStats.collectAsState()
    val penaltyMessage by viewModel.penaltyMessage.collectAsState()
    val milestoneBonusMessage by viewModel.milestoneBonusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showOnboardingDialog by remember { mutableStateOf(false) }
    var triggerMic by remember { mutableStateOf(false) }
    var triggerNotif by remember { mutableStateOf(false) }

    var micGranted by remember { mutableStateOf<Boolean?>(null) }
    var notifGranted by remember { mutableStateOf<Boolean?>(null) }

    var headerClicks by remember { mutableIntStateOf(0) }
    var showDebugDialog by remember { mutableStateOf(false) }

    val currentLevel = stats?.level ?: 1
    val onBackground = MaterialTheme.colorScheme.onBackground

    LaunchedEffect(Unit) {
        micGranted = permissionManager.hasRecordAudioPermission()
        notifGranted = permissionManager.hasNotificationPermission()
    }

    LaunchedEffect(micGranted, notifGranted) {
        val m = micGranted ?: return@LaunchedEffect
        val n = notifGranted ?: return@LaunchedEffect
        showOnboardingDialog = !m || !n
    }

    LaunchedEffect(penaltyMessage) {
        val msg = penaltyMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumePenaltyMessage()
    }

    LaunchedEffect(milestoneBonusMessage) {
        val msg = milestoneBonusMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeMilestoneBonusMessage()
    }

    if (triggerMic) {
        permissionManager.AskForRecordAudioPermission { granted ->
            micGranted = granted
            triggerMic = false
        }
    }
    if (triggerNotif) {
        permissionManager.AskForNotificationPermission { granted ->
            notifGranted = granted
            triggerNotif = false
        }
    }

    val canEnterGame = micGranted == true && notifGranted == true

    val resetDebugTrigger = {
        showDebugDialog = false
        headerClicks = 0
    }

    DebugMenuDialog(
        visible = showDebugDialog,
        stats = stats,
        onClose = resetDebugTrigger,
        onApplyChanges = { newStats ->
            viewModel.updateStatsManually(newStats)
            resetDebugTrigger()
        },
    )

    if (showOnboardingDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    "Permissions",
                    fontWeight = FontWeight.Black,
                )
            },
            text = {
                Text(
                    "Microphone access is required for live growl feedback. " +
                        "Notifications keep your streak reminders — grant both to enter the arena.",
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { triggerMic = true }) {
                        Text("Allow microphone")
                    }
                    TextButton(onClick = { triggerNotif = true }) {
                        Text("Allow notifications")
                    }
                }
            },
        )
    }

    val firePulse = rememberInfiniteTransition(label = "fire")
    val fireScale by firePulse.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "firePulse",
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "METAL VOCAL TRAINER",
                            style = MaterialTheme.typography.labelLarge,
                            letterSpacing = 2.sp,
                            color = onBackground.copy(alpha = 0.92f),
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                            ) {
                                if (showDebugDialog) return@clickable
                                headerClicks++
                                if (headerClicks >= 20) {
                                    showDebugDialog = true
                                }
                            },
                        )
                    },
                    actions = {
                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Settings",
                                tint = onBackground.copy(alpha = 0.85f),
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                item {
                    Text(
                        text = "STREAK",
                        style = MaterialTheme.typography.labelLarge,
                        color = onBackground.copy(alpha = 0.55f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(56.dp)
                                .scale(fireScale),
                        )
                        Text(
                            text = "${stats?.streakCount ?: 0}",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "TOTAL SCORE",
                        style = MaterialTheme.typography.labelLarge,
                        color = onBackground.copy(alpha = 0.55f),
                    )
                    Text(
                        text = "${stats?.totalScore ?: 0L}",
                        style = MaterialTheme.typography.displayLarge,
                        color = onBackground.copy(alpha = 0.95f),
                    )
                    Text(
                        text = "LEVEL $currentLevel",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    GlobalVocalProgressBar(
                        currentLevel = currentLevel,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "EVOLUTION ROADMAP",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFFFD700).copy(alpha = 0.85f),
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                    )
                }

                vocalRoadmapItems(
                    currentLevel = currentLevel,
                    onLockedStageTapped = { requiredLevel ->
                        scope.launch {
                            snackbarHostState.showSnackbar("Requires Level $requiredLevel")
                        }
                    },
                )

                item {
                    Spacer(modifier = Modifier.height(28.dp))
                    Button(
                        onClick = {
                            if (!canEnterGame) {
                                showOnboardingDialog = true
                                scope.launch {
                                    micGranted = permissionManager.hasRecordAudioPermission()
                                    notifGranted = permissionManager.hasNotificationPermission()
                                }
                            } else {
                                onStartGame()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        Text(
                            "START GAME",
                            fontWeight = FontWeight.Black,
                        )
                    }
                }

                if (!canEnterGame && micGranted != null) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                        TextButton(
                            onClick = { showOnboardingDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Grant microphone & notifications to play")
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
