package com.rotguard.app.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rotguard.app.R
import java.util.Locale
import kotlin.math.ceil

private const val MILLIS_PER_TENTH = 100.0
private const val TENTHS_PER_SECOND = 10

// Unlock animation, as fractions of UNLOCK_ANIMATION_MILLIS: shackle swings open, then the lock hands over to the label.
private const val SHACKLE_OPEN_END = 0.6f
private const val HANDOVER_START = 0.45f
private const val LOCK_EXIT_SCALE_GAIN = 0.3f

// Padlock geometry on a 16x20 unit grid.
private const val GLYPH_WIDTH_DP = 16
private const val GLYPH_HEIGHT_DP = 20
private const val GLYPH_UNITS = 16f
private const val BODY_TOP = 9f
private const val BODY_LEFT = 1f
private const val BODY_WIDTH = 14f
private const val BODY_HEIGHT = 11f
private const val BODY_CORNER = 2.5f
private const val SHACKLE_LEFT = 4f
private const val SHACKLE_RIGHT = 12f
private const val SHACKLE_ARC_TOP = 1f
private const val SHACKLE_SHOULDER = 5f
private const val SHACKLE_STROKE = 2.2f
private const val SHACKLE_LIFT = 3f
private const val SHACKLE_SWING_DEGREES = 38f
private const val KEYHOLE_X = 8f
private const val KEYHOLE_DROP = 5f
private const val KEYHOLE_RADIUS = 1.6f

/**
 * Where a hold button is in its lock → countdown → unlock intro. Pure function of elapsed time so it is unit-tested.
 *
 * - [isLocked]: input is ignored and the countdown shows.
 * - [countdownTenths]: time left, in tenths of a second, rounded up (10 = "1.0s").
 * - [unlockFraction]: 0 until the lock ends, then runs to 1 over [UNLOCK_ANIMATION_MILLIS]; 1 = normal label only.
 */
@Immutable
data class HoldUnlockPhase(
    val isLocked: Boolean,
    val countdownTenths: Int,
    val unlockFraction: Float,
) {
    companion object {
        const val UNLOCK_ANIMATION_MILLIS = 450L
        val UNLOCKED = HoldUnlockPhase(isLocked = false, countdownTenths = 0, unlockFraction = 1f)

        fun at(elapsedMillis: Long, lockMillis: Long): HoldUnlockPhase {
            if (lockMillis <= 0L) return UNLOCKED
            val remainingMillis = (lockMillis - elapsedMillis).coerceAtLeast(0L)
            return HoldUnlockPhase(
                isLocked = remainingMillis > 0L,
                countdownTenths = ceil(remainingMillis / MILLIS_PER_TENTH).toInt(),
                unlockFraction = ((elapsedMillis - lockMillis).toFloat() / UNLOCK_ANIMATION_MILLIS).coerceIn(0f, 1f),
            )
        }
    }
}

/** Runs the lock clock from first composition; a new [lockMillis] restarts it. */
@Composable
fun rememberHoldUnlockPhase(lockMillis: Long): HoldUnlockPhase {
    if (lockMillis <= 0L) return HoldUnlockPhase.UNLOCKED
    val totalMillis = lockMillis + HoldUnlockPhase.UNLOCK_ANIMATION_MILLIS
    val elapsedMillis = remember(lockMillis) { Animatable(0f) }
    LaunchedEffect(lockMillis) {
        elapsedMillis.animateTo(
            targetValue = totalMillis.toFloat(),
            animationSpec = tween(durationMillis = totalMillis.toInt(), easing = LinearEasing),
        )
    }
    return HoldUnlockPhase.at(elapsedMillis.value.toLong(), lockMillis)
}

/** The hold button's center plaque: padlock + countdown while locked, then the shackle opens into [label]. */
@Composable
fun HoldPlaqueContent(
    label: String,
    phase: HoldUnlockPhase,
    textColor: Color,
    lockColor: Color,
    keyholeColor: Color,
    modifier: Modifier = Modifier,
) {
    val handover = ((phase.unlockFraction - HANDOVER_START) / (1f - HANDOVER_START)).coerceIn(0f, 1f)
    val shackleOpen = FastOutSlowInEasing.transform((phase.unlockFraction / SHACKLE_OPEN_END).coerceIn(0f, 1f))
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .alpha(handover),
        )
        if (phase.unlockFraction < 1f) {
            Row(
                modifier = Modifier
                    .alpha(1f - handover)
                    .scale(1f + LOCK_EXIT_SCALE_GAIN * handover),
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PadlockGlyph(openFraction = shackleOpen, color = lockColor, keyholeColor = keyholeColor)
                if (phase.isLocked) {
                    val tenths = phase.countdownTenths
                    Text(
                        text = stringResource(
                            R.string.shield_seconds_short,
                            String.format(Locale.getDefault(), "%.1f", tenths / TENTHS_PER_SECOND.toFloat()),
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                    )
                }
            }
        }
    }
}

/** A padlock whose shackle lifts and swings open on its right leg as [openFraction] goes 0 → 1. */
@Composable
private fun PadlockGlyph(openFraction: Float, color: Color, keyholeColor: Color) {
    Canvas(modifier = Modifier.size(width = GLYPH_WIDTH_DP.dp, height = GLYPH_HEIGHT_DP.dp)) {
        val u = size.width / GLYPH_UNITS
        val legBottom = (BODY_TOP + 1f) * u
        translate(top = -SHACKLE_LIFT * u * openFraction) {
            rotate(degrees = SHACKLE_SWING_DEGREES * openFraction, pivot = Offset(SHACKLE_RIGHT * u, legBottom)) {
                val shackle = Path().apply {
                    moveTo(SHACKLE_LEFT * u, legBottom)
                    lineTo(SHACKLE_LEFT * u, SHACKLE_SHOULDER * u)
                    arcTo(
                        rect = Rect(SHACKLE_LEFT * u, SHACKLE_ARC_TOP * u, SHACKLE_RIGHT * u, BODY_TOP * u),
                        startAngleDegrees = 180f,
                        sweepAngleDegrees = 180f,
                        forceMoveTo = false,
                    )
                    lineTo(SHACKLE_RIGHT * u, legBottom)
                }
                drawPath(shackle, color, style = Stroke(width = SHACKLE_STROKE * u, cap = StrokeCap.Round))
            }
        }
        drawRoundRect(
            color = color,
            topLeft = Offset(BODY_LEFT * u, BODY_TOP * u),
            size = Size(BODY_WIDTH * u, BODY_HEIGHT * u),
            cornerRadius = CornerRadius(BODY_CORNER * u),
        )
        drawCircle(
            color = keyholeColor,
            radius = KEYHOLE_RADIUS * u,
            center = Offset(KEYHOLE_X * u, (BODY_TOP + KEYHOLE_DROP) * u),
        )
    }
}
