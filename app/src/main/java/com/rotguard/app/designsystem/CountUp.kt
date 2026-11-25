package com.rotguard.app.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlin.math.roundToInt

private const val COUNT_UP_MILLIS = 900

/**
 * A headline number that counts up from 0 when it first appears, then glides to each new value.
 * Callers format the returned value themselves, so localized digits and units stay intact.
 */
@Composable
fun animatedCount(target: Int, durationMillis: Int = COUNT_UP_MILLIS): Int {
    val shown = remember { Animatable(0f) }
    LaunchedEffect(target) {
        shown.animateTo(
            targetValue = target.toFloat(),
            animationSpec = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
        )
    }
    return shown.value.roundToInt()
}
