package org.davidnardya.vocalcoach.presentation.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.davidnardya.vocalcoach.domain.models.PlayerStats

@Composable
fun DebugMenuDialog(
    visible: Boolean,
    stats: PlayerStats?,
    onClose: () -> Unit,
    onApplyChanges: (PlayerStats) -> Unit,
) {
    if (!visible) return

    var levelText by remember { mutableStateOf("") }
    var scoreText by remember { mutableStateOf("") }
    var streakText by remember { mutableStateOf("") }
    var levelsTodayText by remember { mutableStateOf("") }
    var resetLastDates by remember { mutableStateOf(false) }
    var disableContentCheck by remember { mutableStateOf(false) }

    LaunchedEffect(visible, stats) {
        if (!visible || stats == null) return@LaunchedEffect
        levelText = stats.level.toString()
        scoreText = stats.totalScore.toString()
        streakText = stats.streakCount.toString()
        levelsTodayText = stats.levelsGainedToday.toString()
        resetLastDates = false
        disableContentCheck = stats.debugDisableContentCheck
    }

    AlertDialog(
        onDismissRequest = { },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
        title = {
            Text(
                "Developer debug",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "Overrides write directly to DataStore. Level is derived from total score; " +
                        "if “Force Level” is set, score is replaced to match that level.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = levelText,
                    onValueChange = { s -> levelText = s.filter { ch -> ch.isDigit() } },
                    label = { Text("Force Level") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = scoreText,
                    onValueChange = { s -> scoreText = s.filter { ch -> ch.isDigit() || ch == '-' } },
                    label = { Text("Force Total Score") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = streakText,
                    onValueChange = { s -> streakText = s.filter { ch -> ch.isDigit() || ch == '-' } },
                    label = { Text("Force Streak") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                OutlinedTextField(
                    value = levelsTodayText,
                    onValueChange = { s -> levelsTodayText = s.filter { ch -> ch.isDigit() || ch == '-' } },
                    label = { Text("Levels Gained Today") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = resetLastDates,
                        onCheckedChange = { resetLastDates = it },
                    )
                    Text(
                        "Reset Last Dates (clears last level-up & last streak increment)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = disableContentCheck,
                        onCheckedChange = { disableContentCheck = it },
                    )
                    Text(
                        "Disable Content Check (Auto-pass growls)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val base = stats ?: PlayerStats(
                        totalScore = 0L,
                        streakCount = 0,
                        lastLevelUpDate = null,
                        lastStreakIncrementDate = null,
                        levelsGainedToday = 0,
                        levelsGainedForDate = null,
                    )
                    var total = base.totalScore
                    if (levelText.isNotBlank()) {
                        val lv = levelText.toIntOrNull()
                        if (lv != null && lv >= 1) {
                            total = PlayerStats.totalScoreForTargetLevel(lv)
                        }
                    } else if (scoreText.isNotBlank()) {
                        total = scoreText.toLongOrNull() ?: total
                    }
                    val streak = streakText.toIntOrNull() ?: base.streakCount
                    val levelsToday = levelsTodayText.toIntOrNull() ?: base.levelsGainedToday
                    val applied = base.copy(
                        totalScore = total,
                        streakCount = streak.coerceAtLeast(0),
                        levelsGainedToday = levelsToday.coerceAtLeast(0),
                        lastLevelUpDate = if (resetLastDates) null else base.lastLevelUpDate,
                        lastStreakIncrementDate = if (resetLastDates) null else base.lastStreakIncrementDate,
                        debugDisableContentCheck = disableContentCheck
                    )
                    onApplyChanges(applied)
                },
            ) {
                Text("APPLY CHANGES")
            }
        },
        dismissButton = {
            TextButton(onClick = onClose) {
                Text("CLOSE")
            }
        },
    )
}
