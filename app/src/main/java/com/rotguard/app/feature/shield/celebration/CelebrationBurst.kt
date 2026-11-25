package com.rotguard.app.feature.shield.celebration

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

/** Matches FrictionSessionController.EXIT_CELEBRATION_DURATION_MS (1 550 ms). */
const val CELEBRATION_SECONDS = 1.55f
private const val MILLIS_PER_SECOND = 1000f
private const val CANVAS_DP = 280
private const val BADGE_RADIUS_DP = 46

/** Plays one [variant] once from first composition: background accent → particles → badge + emblem. */
@Composable
fun CelebrationBurst(variant: CelebrationVariant, modifier: Modifier = Modifier) {
    val seconds = remember(variant) { Animatable(0f) }
    val sparks = remember(variant) { seededSparks(variant) }
    LaunchedEffect(variant) {
        seconds.animateTo(
            targetValue = CELEBRATION_SECONDS,
            animationSpec = tween((CELEBRATION_SECONDS * MILLIS_PER_SECOND).toInt(), easing = LinearEasing),
        )
    }
    Canvas(modifier = modifier.size(size = CANVAS_DP.dp)) {
        drawCelebration(variant, sparks, seconds.value)
    }
}

fun seededSparks(variant: CelebrationVariant): List<Spark> {
    val random = Random(variant.seed)
    return List(variant.effect.particleCount) {
        Spark(random.nextFloat(), random.nextFloat(), random.nextFloat(), random.nextFloat())
    }
}

/** One frame of [variant] at [seconds] since it started. */
fun DrawScope.drawCelebration(variant: CelebrationVariant, sparks: List<Spark>, seconds: Float) {
    val fx = Fx(
        t = seconds,
        center = center,
        r = size.minDimension / 2f,
        e = BADGE_RADIUS_DP.dp.toPx(),
        u = 1.dp.toPx(),
        p = variant.palette,
        sparks = sparks,
        frame = variant.frame,
    )
    drawAccent(variant.accent, fx)
    variant.effect.draw(this, fx)
    val entrance = entranceTransform(variant.entrance, seconds, fx.u)
    translate(top = entrance.dy) {
        rotate(degrees = entrance.degrees, pivot = center) {
            scale(scaleX = entrance.sx, scaleY = entrance.sy, pivot = center) {
                drawFrame(variant.frame, fx)
                drawEmblem(variant.emblem, fx)
            }
        }
    }
}
