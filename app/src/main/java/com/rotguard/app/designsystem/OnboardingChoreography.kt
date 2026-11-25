package com.rotguard.app.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val DEFAULT_CHAR_DELAY_MS = 26L
private const val MAX_TYPING_DURATION_MS = 2_000L
private const val FLY_IN_DURATION_MS = 650
private const val FLY_IN_START_SCALE = 0.98f
private const val FLY_IN_SPRING_DAMPING = 1f

/** Roughly when a fly-in's spring has visibly settled after it starts. */
private const val FLY_IN_SETTLE_MS = 900L

/**
 * Collects when each animated element on a screen finishes appearing, so a call-to-action can wait for all of
 * them. [TypewriterText] and [StaggeredFlyIn] report into the nearest [LocalRevealTimeline] automatically.
 */
@Stable
class RevealTimeline {
    var finishMillis by mutableLongStateOf(0L)
        private set

    fun report(endMillis: Long) {
        if (endMillis > finishMillis) finishMillis = endMillis
    }
}

val LocalRevealTimeline = staticCompositionLocalOf<RevealTimeline?> { null }

fun typewriterDurationMillis(text: String, maxDurationMillis: Long = MAX_TYPING_DURATION_MS): Long {
    if (text.isEmpty()) return 0L
    return (text.length.toLong() * DEFAULT_CHAR_DELAY_MS).coerceAtMost(maxDurationMillis)
}

@Composable
fun TypewriterText(
    text: String,
    style: TextStyle,
    color: Color,
    animationKey: Any,
    modifier: Modifier = Modifier,
    startDelayMillis: Long = 0L,
    textAlign: TextAlign = TextAlign.Start,
) {
    var visibleCount by remember(animationKey) { mutableIntStateOf(0) }
    val latestText by rememberUpdatedState(text)
    val revealTimeline = LocalRevealTimeline.current
    SideEffect { revealTimeline?.report(startDelayMillis + typewriterDurationMillis(text)) }
    val typingSound = LocalTypingSound.current

    LaunchedEffect(animationKey) {
        visibleCount = 0
        if (startDelayMillis > 0L) {
            delay(startDelayMillis)
        }
        val targetLength = latestText.length
        if (targetLength == 0) return@LaunchedEffect
        val perCharMs = (MAX_TYPING_DURATION_MS / targetLength).coerceIn(4L, DEFAULT_CHAR_DELAY_MS)
        for (index in 1..targetLength) {
            visibleCount = index
            typingSound?.onCharacterTyped(latestText[index - 1])
            delay(perCharMs)
        }
        visibleCount = latestText.length
    }

    val displayedText = if (visibleCount >= text.length) {
        text
    } else {
        text.take(visibleCount)
    }

    Box(modifier = modifier) {
        Text(
            text = text,
            style = style,
            color = Color.Transparent,
            textAlign = textAlign,
            modifier = Modifier.alpha(0f),
        )
        Text(
            text = displayedText,
            style = style,
            color = color,
            textAlign = textAlign,
        )
    }
}

@Composable
fun StaggeredFlyIn(
    animationKey: Any,
    delayMillis: Long,
    modifier: Modifier = Modifier,
    fromOffsetY: Dp = 24.dp,
    fromOffsetX: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val initialX = with(density) { fromOffsetX.toPx() }
    val initialY = with(density) { fromOffsetY.toPx() }

    val revealTimeline = LocalRevealTimeline.current
    SideEffect { revealTimeline?.report(delayMillis + FLY_IN_SETTLE_MS) }

    val alphaAnim = remember(animationKey) { Animatable(0f) }
    val offsetXAnim = remember(animationKey) { Animatable(initialX) }
    val offsetYAnim = remember(animationKey) { Animatable(initialY) }
    val scaleAnim = remember(animationKey) { Animatable(FLY_IN_START_SCALE) }

    LaunchedEffect(animationKey) {
        alphaAnim.snapTo(0f)
        offsetXAnim.snapTo(initialX)
        offsetYAnim.snapTo(initialY)
        scaleAnim.snapTo(FLY_IN_START_SCALE)
        if (delayMillis > 0L) {
            delay(delayMillis)
        }
        val moveSpec = spring<Float>(
            dampingRatio = FLY_IN_SPRING_DAMPING,
            stiffness = Spring.StiffnessLow,
        )
        launch { alphaAnim.animateTo(1f, tween(durationMillis = FLY_IN_DURATION_MS)) }
        launch { offsetXAnim.animateTo(0f, moveSpec) }
        launch { offsetYAnim.animateTo(0f, moveSpec) }
        launch { scaleAnim.animateTo(1f, moveSpec) }
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetXAnim.value.roundToInt(), offsetYAnim.value.roundToInt()) }
            .scale(scaleAnim.value)
            .alpha(alphaAnim.value),
    ) {
        content()
    }
}
