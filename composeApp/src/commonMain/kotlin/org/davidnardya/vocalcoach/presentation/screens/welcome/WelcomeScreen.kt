package org.davidnardya.vocalcoach.presentation.screens.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import kotlinx.datetime.Clock
import org.davidnardya.vocalcoach.core.util.PermissionManager
import org.davidnardya.vocalcoach.di.createScoreRepository
import org.davidnardya.vocalcoach.di.createStreakManagementUseCase
import org.davidnardya.vocalcoach.domain.models.TrainingStage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    permissionManager: PermissionManager,
    onStartGame: (TrainingStage) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: WelcomeViewModel = viewModel {
        WelcomeViewModel(
            createScoreRepository(),
            createStreakManagementUseCase(),
        )
    },
) {
    val stats by viewModel.playerStats.collectAsState()
    val roadmap by viewModel.roadmapState.collectAsState()
    val penaltyMessage by viewModel.penaltyMessage.collectAsState()
    val milestoneBonusMessage by viewModel.milestoneBonusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showOnboardingDialog by remember { mutableStateOf(false) }
    var showDevDialog by remember { mutableStateOf(false) }
    var titleTapCount by remember { mutableStateOf(0) }
    var titleTapWindowStartMs by remember { mutableStateOf(0L) }
    var devScoreInput by remember { mutableStateOf("") }
    var devLevelInput by remember { mutableStateOf("") }
    var triggerMic by remember { mutableStateOf(false) }
    var triggerNotif by remember { mutableStateOf(false) }

    var micGranted by remember { mutableStateOf<Boolean?>(null) }
    var notifGranted by remember { mutableStateOf<Boolean?>(null) }

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

    if (showDevDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Developer Controls", fontWeight = FontWeight.Black) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Set score and level for stage testing.")
                    TextField(
                        value = devScoreInput,
                        onValueChange = { devScoreInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Score") },
                        singleLine = true,
                    )
                    TextField(
                        value = devLevelInput,
                        onValueChange = { devLevelInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Level") },
                        singleLine = true,
                    )
                    Text(
                        "Level maps to minimum score: (level - 1) * 10",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val score = devScoreInput.toIntOrNull() ?: 0
                        val level = devLevelInput.toIntOrNull() ?: 1
                        viewModel.setTestProgress(score, level)
                    },
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            devScoreInput = "0"
                            devLevelInput = "1"
                            viewModel.setTestProgress(0, 1)
                        },
                    ) { Text("Reset") }
                    TextButton(onClick = { showDevDialog = false }) { Text("Close") }
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

    Scaffold(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "METAL VOCAL TRAINER",
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 2.sp,
                        modifier = Modifier.clickable {
                            val now = Clock.System.now().toEpochMilliseconds()
                            if (titleTapWindowStartMs == 0L || now - titleTapWindowStartMs > 6000L) {
                                titleTapWindowStartMs = now
                                titleTapCount = 0
                            }
                            titleTapCount += 1
                            if (titleTapCount >= 20) {
                                showDevDialog = true
                                titleTapCount = 0
                                titleTapWindowStartMs = 0L
                                devScoreInput = (stats?.totalScore ?: 0L).toString()
                                devLevelInput = (stats?.level ?: 1).toString()
                            }
                        },
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "STREAK",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "TOTAL SCORE",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${stats?.totalScore ?: 0L}",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "LEVEL ${stats?.level ?: 1}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(modifier = Modifier.height(40.dp))

            RoadmapSection(
                items = roadmap.items,
                onStageSelected = { stage ->
                    viewModel.selectStage(stage)
                },
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (!canEnterGame) {
                        showOnboardingDialog = true
                        scope.launch {
                            micGranted = permissionManager.hasRecordAudioPermission()
                            notifGranted = permissionManager.hasNotificationPermission()
                        }
                    } else {
                        onStartGame(roadmap.currentStage)
                    }
                },
                modifier = Modifier.height(56.dp),
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

            if (!canEnterGame && micGranted != null) {
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { showOnboardingDialog = true }) {
                    Text("Grant microphone & notifications to play")
                }
            }
        }
    }
}

@Composable
private fun RoadmapSection(
    items: List<StageRoadmapItem>,
    onStageSelected: (TrainingStage) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 8.dp),
    ) {
        Text(
            text = "ROADMAP",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
        )
        items.forEach { item ->
            val cardColor = when {
                !item.isUnlocked -> MaterialTheme.colorScheme.surfaceVariant
                item.isCurrent -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = cardColor,
                tonalElevation = if (item.isCurrent) 3.dp else 0.dp,
                modifier = Modifier,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${item.stage.title} - ${item.stage.description}",
                            fontWeight = if (item.isCurrent) FontWeight.Bold else FontWeight.Medium,
                        )
                        item.requirementText?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Box {
                        val status = when {
                            !item.isUnlocked -> "LOCKED"
                            item.isCurrent -> "ACTIVE"
                            else -> "COMPLETED"
                        }
                        TextButton(
                            enabled = item.isUnlocked,
                            onClick = { onStageSelected(item.stage) },
                        ) {
                            Text(status)
                        }
                    }
                }
            }
        }
    }
}
