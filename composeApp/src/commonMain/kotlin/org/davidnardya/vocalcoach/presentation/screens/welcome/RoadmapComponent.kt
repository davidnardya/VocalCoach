package org.davidnardya.vocalcoach.presentation.screens.welcome

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import org.davidnardya.vocalcoach.domain.models.VOCAL_ROADMAP_DISPLAY_MAX_LEVEL
import org.davidnardya.vocalcoach.domain.models.VocalStage
import org.davidnardya.vocalcoach.domain.models.isCompleted
import org.davidnardya.vocalcoach.domain.models.isCurrent
import org.davidnardya.vocalcoach.domain.models.isLocked
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

private val LegendaryGold = Color(0xFFFFD700)
private val ElectricOrange = Color(0xFFFF8F00)
private val MutedCharcoal = Color(0xFF2A2A2A)
private val PipInactive = Color(0xFF3D3D3D)

private fun hexagonShape(): Shape = GenericShape { size: Size, _: LayoutDirection ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = min(size.width, size.height) / 2f * 0.92f
    for (i in 0 until 6) {
        val angle = (60.0 * i - 30.0) * PI / 180.0
        val x = cx + r * cos(angle).toFloat()
        val y = cy + r * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

@Composable
fun GlobalVocalProgressBar(
    currentLevel: Int,
    modifier: Modifier = Modifier,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    val progress =
        (currentLevel.toFloat() / VOCAL_ROADMAP_DISPLAY_MAX_LEVEL.toFloat()).coerceIn(0f, 1f)
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "PATH TO LEVEL $VOCAL_ROADMAP_DISPLAY_MAX_LEVEL",
            style = MaterialTheme.typography.labelLarge,
            color = LegendaryGold.copy(alpha = 0.85f),
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Black,
        )
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(hexagonShape()),
            color = ElectricOrange,
            trackColor = MutedCharcoal.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Level $currentLevel / $VOCAL_ROADMAP_DISPLAY_MAX_LEVEL",
            style = MaterialTheme.typography.bodyMedium,
            color = onBg.copy(alpha = 0.75f),
        )
    }
}

@OptIn(ExperimentalResourceApi::class)
fun LazyListScope.vocalRoadmapItems(
    currentLevel: Int,
    onLockedStageTapped: (requiredLevel: Int) -> Unit,
    stages: List<VocalStage> = DefaultVocalRoadmapStages,
) {
    itemsIndexed(stages, key = { _, s -> s.name }) { index, stage ->
        val locked = stage.isLocked(currentLevel)
        val current = stage.isCurrent(currentLevel)
        val completed = stage.isCompleted(currentLevel)
        val stageBg = MaterialTheme.colorScheme.background

        val accent = when {
            completed -> LegendaryGold
            current -> ElectricOrange
            else -> MutedCharcoal
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(stageBg)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            if (index > 0) {
                RoadmapConnectorLine(color = MutedCharcoal.copy(alpha = 0.9f))
            }
            RoadmapStageRow(
                stage = stage,
                accent = accent,
                locked = locked,
                current = current,
                completed = completed,
                currentLevel = currentLevel,
                onLockedStageTapped = onLockedStageTapped,
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
private fun RoadmapStageRow(
    stage: VocalStage,
    accent: Color,
    locked: Boolean,
    current: Boolean,
    completed: Boolean,
    currentLevel: Int,
    onLockedStageTapped: (requiredLevel: Int) -> Unit,
) {
    val hex = remember { hexagonShape() }
    val nodeInnerBg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground
    val pulse = rememberInfiniteTransition(label = "eraPulse")
    val pulseScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "eraPulseScale",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (locked) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onLockedStageTapped(stage.levels.first) }
                } else {
                    Modifier
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(end = 14.dp),
        ) {
            val nodeModifier = Modifier
                .size(76.dp)
                .scale(if (current) pulseScale else 1f)
                .then(
                    if (current) {
                        Modifier.shadow(
                            elevation = 20.dp,
                            shape = hex,
                            spotColor = ElectricOrange.copy(alpha = 0.95f),
                            ambientColor = ElectricOrange.copy(alpha = 0.5f),
                        )
                    } else {
                        Modifier
                    },
                )
                .clip(hex)
                .background(accent.copy(alpha = if (locked) 0.35f else 0.18f))
                .padding(3.dp)
                .clip(hex)
                .background(nodeInnerBg)

            Box(modifier = nodeModifier, contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(stage.icon),
                    contentDescription = stage.name,
                    modifier = Modifier
                        .size(44.dp)
                        .padding(4.dp),
                    contentScale = ContentScale.Fit,
                    alpha = if (locked) 0.35f else 1f,
                )
                if (locked) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = null,
                            tint = onBg.copy(alpha = 0.55f),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Locked",
                            tint = onBg.copy(alpha = 0.9f),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stage.name.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                when {
                    completed -> {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MASTERED",
                            style = MaterialTheme.typography.labelSmall,
                            color = LegendaryGold,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .background(LegendaryGold.copy(alpha = 0.12f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    current -> {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "IN PROGRESS",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricOrange,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .background(ElectricOrange.copy(alpha = 0.15f), CircleShape)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
            Text(
                text = stage.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (locked) MutedCharcoal else onBg.copy(alpha = 0.82f),
            )
            Text(
                text = levelRangeLabel(stage),
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
            EraMilestonePips(
                stage = stage,
                currentLevel = currentLevel,
                locked = locked,
                current = current,
                completed = completed,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun EraMilestonePips(
    stage: VocalStage,
    currentLevel: Int,
    locked: Boolean,
    current: Boolean,
    completed: Boolean,
    modifier: Modifier = Modifier,
) {
    val eraStart = stage.levels.first
    val litCount = when {
        locked -> 0
        completed -> 5
        else -> {
            val inEra = currentLevel.coerceIn(eraStart, stage.levels.last.coerceAtLeast(eraStart))
            min(5, 1 + (inEra - eraStart) / 10)
        }
    }

    val onBg = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "MILESTONES",
            style = MaterialTheme.typography.labelSmall,
            color = onBg.copy(alpha = 0.35f),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 4.dp),
        )
        repeat(5) { i ->
            val filled = i < litCount
            val pipColor = when {
                locked -> MutedCharcoal
                completed -> LegendaryGold
                current && filled -> ElectricOrange
                current && !filled -> PipInactive
                else -> PipInactive
            }
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(pipColor),
            )
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun RoadmapComponent(
    currentLevel: Int,
    onLockedStageTapped: (requiredLevel: Int) -> Unit,
    modifier: Modifier = Modifier,
    stages: List<VocalStage> = DefaultVocalRoadmapStages,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            vocalRoadmapItems(currentLevel, onLockedStageTapped, stages)
        }
    }
}

private fun levelRangeLabel(stage: VocalStage): String =
    if (stage.levels.last == Int.MAX_VALUE) {
        "Era · Levels ${stage.levels.first}+"
    } else {
        "Era · Levels ${stage.levels.first}–${stage.levels.last}"
    }

@Composable
private fun RoadmapConnectorLine(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            drawLine(
                color = color,
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
            )
        }
    }
}
