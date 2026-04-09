package org.davidnardya.vocalcoach.presentation.screens.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.runtime.LaunchedEffect
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
private val BloodRed = Color(0xFFB22222)

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

@Composable
fun LevelUpAnimation(
    newLevel: Int,
    modifier: Modifier = Modifier,
    legendaryStreakMilestone: Boolean = false,
) {
    val accent = if (legendaryStreakMilestone) LegendaryGold else BloodRed
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
                text = if (legendaryStreakMilestone) "LEGENDARY STREAK" else "LEVEL UP!",
                style = TextStyle(
                    color = accent,
                    fontSize = if (legendaryStreakMilestone) 44.sp else 52.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
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
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.8f),
                        blurRadius = 12f,
                    ),
                ),
            )
            Text(
                text = "YOU ARE EVOLVING!",
                modifier = Modifier.padding(top = 12.dp),
                style = TextStyle(
                    color = ElectricOrange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                ),
            )
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
    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(2000, easing = LinearEasing)
        )
    }
    
    val seeds = remember { List(50) { Random.nextFloat() } }

    Canvas(modifier = modifier) {
        val progress = progressAnim.value
        val bottom = size.height * 0.92f
        val sparkColor = if (legendary) LegendaryGold else accent
        seeds.forEachIndexed { i, r ->
            val x = size.width * (0.05f + (i % 10) / 10f * 0.9f) + (r - 0.5f) * 40f
            val speed = 0.4f + r
            val y = bottom - progress * canvasHeight * speed * 0.4f
            val life = (1f - progress).coerceIn(0f, 1f)
            val len = 10.dp.toPx() + r * 15.dp.toPx()
            val angle = (i * 37f) * (PI.toFloat() / 180f)
            val x2 = x + cos(angle) * len
            val y2 = y + sin(angle) * len
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        sparkColor.copy(alpha = 0.9f * life),
                        ElectricOrange.copy(alpha = 0.5f * life),
                        Color.Transparent,
                    ),
                    start = Offset(x, y),
                    end = Offset(x2, y2),
                ),
                start = Offset(x, y),
                end = Offset(x2, y2),
                strokeWidth = 2.5.dp.toPx(),
            )
        }
    }
}
