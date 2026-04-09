package org.davidnardya.vocalcoach.presentation.screens.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.davidnardya.vocalcoach.core.util.PermissionManager
import org.davidnardya.vocalcoach.di.createScoreRepository
import org.davidnardya.vocalcoach.di.createStreakManagementUseCase
import org.davidnardya.vocalcoach.di.createUpdateStreakUseCase
import org.davidnardya.vocalcoach.domain.developer.DeveloperDebugSettings
import org.davidnardya.vocalcoach.domain.models.TrainingStage

private val MetalGray = Color(0xFF2C2C2C)
private val DarkSteel = Color(0xFF1A1A1A)
private val IndustrialOrange = Color(0xFFFF8F00)
private val BloodRed = Color(0xFFB22222)

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
            createUpdateStreakUseCase(),
        )
    },
) {
    val stats by viewModel.playerStats.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val roadmap by viewModel.roadmapState.collectAsState()
    val penaltyMessage by viewModel.penaltyMessage.collectAsState()
    val milestoneBonusMessage by viewModel.milestoneBonusMessage.collectAsState()
    val welcomeUiState by viewModel.uiState.collectAsState()
    val showDeveloperDialog = welcomeUiState.showDeveloperDialog
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showOnboardingDialog by remember { mutableStateOf(false) }
    /** Local tap counter for the secret menu; visibility lives in [WelcomeUiState.showDeveloperDialog]. */
    var tapCount by remember { mutableStateOf(0) }
    var devScoreInput by remember { mutableStateOf("") }
    var devLevelInput by remember { mutableStateOf("") }
    var devStreakInput by remember { mutableStateOf("") }
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

    LaunchedEffect(showDeveloperDialog) {
        if (showDeveloperDialog) {
            devScoreInput = (stats?.totalScore?.toLong() ?: 0L).toString()
            devLevelInput = (stats?.level ?: 1).toString()
            devStreakInput = currentStreak.toString()
        }
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
                    "PERMISSIONS REQUIRED",
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Text(
                    "MICROPHONE ACCESS IS REQUIRED FOR LIVE GROWL FEEDBACK. " +
                        "NOTIFICATIONS KEEP YOUR STREAK REMINDERS — GRANT BOTH TO ENTER THE ARENA.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { triggerMic = true }) {
                        Text("ALLOW MICROPHONE", fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { triggerNotif = true }) {
                        Text("ALLOW NOTIFICATIONS", fontWeight = FontWeight.Bold)
                    }
                }
            },
        )
    }

    if (showDeveloperDialog) {
        SecretDeveloperDialog(
            devScoreInput = devScoreInput,
            devLevelInput = devLevelInput,
            devStreakInput = devStreakInput,
            onDevScoreChange = { devScoreInput = it },
            onDevLevelChange = { devLevelInput = it },
            onDevStreakChange = { devStreakInput = it },
            onDismiss = viewModel::onDismissDeveloperMenu,
            onApplyProgress = { score, level, streak ->
                viewModel.setTestProgress(score, level, streak)
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
                    val titleInteractionSource = remember { MutableInteractionSource() }
                    Text(
                        "METAL VOCAL COACH",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = titleInteractionSource,
                            ) {
                                tapCount += 1
                                println("[WelcomeScreen] secret dev tapCount=$tapCount/20")
                                if (tapCount == 20) {
                                    viewModel.onDeveloperMenuTriggered()
                                    tapCount = 0
                                }
                            }
                            .padding(vertical = 8.dp),
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = IndustrialOrange)
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
        ) {
            // Stats Header
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSteel,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MetalGray)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "DAILY STREAK",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = IndustrialOrange,
                            modifier = Modifier.size(32.dp).scale(fireScale),
                        )
                        Text(
                            text = "$currentStreak",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                            color = IndustrialOrange,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "TOTAL SCORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray,
                    )
                    Text(
                        text = "${stats?.totalScore?.toLong() ?: 0L}",
                        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Black),
                    )
                    Text(
                        text = "LEVEL ${stats?.level ?: 1}",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = BloodRed,
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Roadmap
            RoadmapSection(
                items = roadmap.items,
                progressInCurrentStage = roadmap.progressInCurrentStage,
                onStageSelected = { stage ->
                    viewModel.selectStage(stage)
                },
            )

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    if (!canEnterGame) {
                        showOnboardingDialog = true
                    } else {
                        onStartGame(roadmap.currentStage)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .border(2.dp, BloodRed, RoundedCornerShape(8.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Black,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    "ENTER THE ARENA",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    letterSpacing = 2.sp
                )
            }
            
            if (!canEnterGame && micGranted != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "PERMISSIONS REQUIRED",
                    color = BloodRed,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "v1.0.0",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun SecretDeveloperDialog(
    devScoreInput: String,
    devLevelInput: String,
    devStreakInput: String,
    onDevScoreChange: (String) -> Unit,
    onDevLevelChange: (String) -> Unit,
    onDevStreakChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onApplyProgress: (score: Int, level: Int, streak: Int) -> Unit,
) {
    val stageBoost by DeveloperDebugSettings.maxUnlockedStageBoost.collectAsState()
    val mockGemini by DeveloperDebugSettings.mockGeminiSuccess.collectAsState()
    val numberKeyboard = KeyboardOptions(keyboardType = KeyboardType.Number)
    val scroll = rememberScrollState()

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text("SECRET DEVELOPER", fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Max unlocked stage (Stages 3–5)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "Applies immediately to the roadmap and arena (tiered scoring). Uses max(level, boost).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StageBoostChip(
                            label = "Auto",
                            selected = stageBoost == null,
                            onClick = { DeveloperDebugSettings.setMaxUnlockedStageBoost(null) },
                            modifier = Modifier.weight(1f),
                        )
                        StageBoostChip(
                            label = "3",
                            selected = stageBoost == TrainingStage.Stage3,
                            onClick = { DeveloperDebugSettings.setMaxUnlockedStageBoost(TrainingStage.Stage3) },
                            modifier = Modifier.weight(1f),
                        )
                        StageBoostChip(
                            label = "4",
                            selected = stageBoost == TrainingStage.Stage4,
                            onClick = { DeveloperDebugSettings.setMaxUnlockedStageBoost(TrainingStage.Stage4) },
                            modifier = Modifier.weight(1f),
                        )
                        StageBoostChip(
                            label = "5",
                            selected = stageBoost == TrainingStage.Stage5,
                            onClick = { DeveloperDebugSettings.setMaxUnlockedStageBoost(TrainingStage.Stage5) },
                            modifier = Modifier.weight(1f),
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Mock Gemini success", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "No network — hardcoded successful JSON.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray,
                            )
                        }
                        Switch(
                            checked = mockGemini,
                            onCheckedChange = { DeveloperDebugSettings.setMockGeminiSuccess(it) },
                        )
                    }

                    Text(
                        "Score / level / streak",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = devScoreInput,
                        onValueChange = { onDevScoreChange(it.filter { ch -> ch.isDigit() }) },
                        label = { Text("Score") },
                        singleLine = true,
                        keyboardOptions = numberKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = devLevelInput,
                        onValueChange = { onDevLevelChange(it.filter { ch -> ch.isDigit() }) },
                        label = { Text("Level") },
                        singleLine = true,
                        keyboardOptions = numberKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = devStreakInput,
                        onValueChange = { onDevStreakChange(it.filter { ch -> ch.isDigit() }) },
                        label = { Text("Day streak") },
                        singleLine = true,
                        keyboardOptions = numberKeyboard,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                    TextButton(
                        onClick = {
                            val score = devScoreInput.toIntOrNull() ?: 0
                            val level = devLevelInput.toIntOrNull() ?: 1
                            val streak = devStreakInput.toIntOrNull() ?: 0
                            onApplyProgress(score, level, streak)
                        },
                    ) {
                        Text("APPLY PROGRESS")
                    }
                }
            }
        }
    }
}

@Composable
private fun StageBoostChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selected) {
        FilledTonalButton(onClick = onClick, modifier = modifier) {
            Text(label, maxLines = 1)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(label, maxLines = 1)
        }
    }
}

@Composable
private fun RoadmapSection(
    items: List<StageRoadmapItem>,
    progressInCurrentStage: Float,
    onStageSelected: (TrainingStage) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "EVOLUTION ROADMAP",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp
            ),
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        items.forEachIndexed { index, item ->
            val isLast = index == items.lastIndex
            val progressVal = if (item.isMaxUnlocked) {
                progressInCurrentStage
            } else {
                val maxUnlockedIdx = items.indexOfFirst { it.isMaxUnlocked }
                if (maxUnlockedIdx == -1 || index < maxUnlockedIdx) 1f else 0f
            }

            StageNode(
                item = item,
                isCurrent = item.isCurrent,
                progress = progressVal,
                showConnector = !isLast,
                onClick = { if (item.isUnlocked) onStageSelected(item.stage) }
            )
        }
    }
}

@Composable
private fun StageNode(
    item: StageRoadmapItem,
    isCurrent: Boolean,
    progress: Float,
    showConnector: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = item.isUnlocked, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Left Column: Indicator & Connector
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (item.isUnlocked) BloodRed else Color.DarkGray)
                    .border(2.dp, if (isCurrent) IndustrialOrange else Color.Transparent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!item.isUnlocked) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                } else {
                    Text(
                        text = (item.stage.ordinal + 1).toString(),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
            
            if (showConnector) {
                // Progress Dots Connector
                Column(
                    modifier = Modifier.padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(5) { i ->
                        val dotProgressThreshold = (i + 1) * 0.2f
                        val isFull = progress >= dotProgressThreshold
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isFull) IndustrialOrange else Color.DarkGray)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Right Column: Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (showConnector) 24.dp else 0.dp)
        ) {
            Text(
                text = item.stage.title.uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = if (item.isUnlocked) Color.White else Color.Gray
            )
            Text(
                text = item.stage.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isUnlocked) Color.LightGray else Color.DarkGray
            )
            
            if (item.isCurrent) {
                Surface(
                    color = IndustrialOrange.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(
                        text = "ACTIVE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = IndustrialOrange
                    )
                }
            } else if (!item.isUnlocked && item.requirementText != null) {
                Text(
                    text = item.requirementText,
                    style = MaterialTheme.typography.labelSmall,
                    color = BloodRed.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
