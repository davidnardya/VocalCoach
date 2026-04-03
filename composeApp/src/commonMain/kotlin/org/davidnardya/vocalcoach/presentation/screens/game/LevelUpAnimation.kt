package org.davidnardya.vocalcoach.presentation.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private val ElectricOrange = Color(0xFFFF8F00)
private val LegendaryGold = Color(0xFFFFD700)

/**
 * 200ms horizontal shake — apply [Animatable] to parent `graphicsLayer { translationX = shake.value }`.
 */
suspend fun runScreenShake200Ms(shakeOffset: Animatable<Float, *>) {
    shakeOffset.snapTo(0f)
    shakeOffset.animateTo(14f, tween(40, easing = LinearEasing))
    shakeOffset.animateTo(-12f, tween(40, easing = LinearEasing))
    shakeOffset.animateTo(9f, tween(40, easing = LinearEasing))
    shakeOffset.animateTo(-5f, tween(40, easing = LinearEasing))
    shakeOffset.animateTo(0f, tween(40, easing = LinearEasing))
}

/**
 * @param legendaryStreakMilestone When true (25× streak milestone + level-up), uses [LegendaryGold] and bonus subtitle.
 */
@Composable
fun LevelUpAnimation(
    newLevel: Int,
    modifier: Modifier = Modifier,
    legendaryStreakMilestone: Boolean = false,
) {
    val accent = if (legendaryStreakMilestone) LegendaryGold else ElectricOrange
    val pulse = rememberInfiniteTransition(label = "levelUpPulse")
    val scale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "titlePulse",
    )

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        SparkParticles(
            modifier = Modifier.fillMaxSize(),
            canvasHeight = maxHeight.value * 3f,
            accent = accent,
            legendary = legendaryStreakMilestone,
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = -2f
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (legendaryStreakMilestone) "25 DAY STREAK BONUS" else "LEVEL UP!",
                style = TextStyle(
                    color = accent,
                    fontSize = if (legendaryStreakMilestone) 44.sp else 52.sp,
                    fontWeight = FontWeight.Black,
                    shadow = Shadow(
                        color = accent.copy(alpha = 0.95f),
                        blurRadius = 28f,
                    ),
                ),
            )
            Text(
                text = "LV $newLevel",
                modifier = Modifier.padding(top = 8.dp),
                style = TextStyle(
                    color = if (legendaryStreakMilestone) LegendaryGold.copy(alpha = 0.95f) else Color(0xFFFFE082),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.6f),
                        blurRadius = 8f,
                    ),
                ),
            )
            if (legendaryStreakMilestone) {
                Text(
                    text = "+20 MILESTONE BONUS!",
                    modifier = Modifier.padding(top = 16.dp),
                    style = TextStyle(
                        color = Color(0xFFFFF8E1),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SparkParticles(
    modifier: Modifier,
    canvasHeight: Float,
    accent: Color,
    legendary: Boolean,
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1600, easing = LinearEasing),
        label = "sparkRise",
    )
    val seeds = remember { List(40) { Random.nextFloat() } }

    Canvas(modifier = modifier) {
        val bottom = size.height * 0.92f
        val sparkGold = if (legendary) LegendaryGold else accent
        seeds.forEachIndexed { i, r ->
            val x = size.width * (0.05f + (i % 10) / 10f * 0.9f) + (r - 0.5f) * 36f
            val speed = 0.5f + r
            val y = bottom - progress * canvasHeight * speed * 0.32f
            val life = (1f - progress).coerceIn(0f, 1f)
            val len = 8.dp.toPx() + r * 12.dp.toPx()
            val angle = (i * 41f) * (PI.toFloat() / 180f)
            val x2 = x + cos(angle) * len
            val y2 = y + sin(angle) * len
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        sparkGold.copy(alpha = 0.9f * life),
                        if (legendary) Color(0xFFFFA000).copy(alpha = 0.5f * life) else Color.Yellow.copy(alpha = 0.35f * life),
                        Color.Transparent,
                    ),
                    start = Offset(x, y),
                    end = Offset(x2, y2),
                ),
                start = Offset(x, y),
                end = Offset(x2, y2),
                strokeWidth = 2.2.dp.toPx(),
            )
        }
    }
}
