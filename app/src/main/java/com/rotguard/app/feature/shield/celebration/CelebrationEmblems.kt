@file:Suppress("MagicNumber") // Badge/emblem geometry on a unit grid scaled by the badge radius.

package com.rotguard.app.feature.shield.celebration

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val ROSETTE_BUMPS = 12
private const val ROSETTE_SAMPLES = 96

/** The badge silhouette at [radius]; also used by SHAPE_PULSE so pulses echo the badge. */
fun CelebrationFrame.outline(center: Offset, radius: Float): Path = Path().apply {
    when (this@outline) {
        CelebrationFrame.CIRCLE, CelebrationFrame.RING -> addOval(Rect(center, radius))
        CelebrationFrame.SQUIRCLE -> addRoundRect(RoundRect(Rect(center, radius * 0.9f), CornerRadius(radius * 0.42f)))
        CelebrationFrame.HEXAGON -> polarLoop(center, 6) { k -> -PI.toFloat() / 2f + k * TAU / 6f to radius }
        CelebrationFrame.ROSETTE -> polarLoop(center, ROSETTE_SAMPLES) { k ->
            val angle = k * TAU / ROSETTE_SAMPLES
            angle to radius * (0.96f + 0.07f * cos(ROSETTE_BUMPS * angle))
        }
    }
    close()
}

/** Joins [steps] + 1 polar points (angle, radius) around [center]. */
private fun Path.polarLoop(center: Offset, steps: Int, point: (Int) -> Pair<Float, Float>) {
    for (k in 0..steps) {
        val (angle, rad) = point(k)
        val x = center.x + cos(angle) * rad
        val y = center.y + sin(angle) * rad
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
}

/** Soft background layer, blooming in over the first half-second. */
internal fun DrawScope.drawAccent(accent: CelebrationAccent, fx: Fx) = with(fx) {
    val appear = easeOut(t / 0.5f)
    when (accent) {
        CelebrationAccent.AURORA -> {
            glow(p.primary.a(0.6f), r * appear)
            glow(p.accent.a(0.22f), 0.65f * r * appear)
        }
        CelebrationAccent.HALO -> {
            glow(p.primary.a(0.4f), 0.8f * r * appear)
            dashedRing(p.accent.a(0.6f * appear), e * 1.55f, t * 40f, u)
            dashedRing(p.sparkA.a(0.32f * appear), e * 2.05f, -t * 30f, u)
        }
        CelebrationAccent.RAY_FAN -> rotate(degrees = t * 25f, pivot = center) {
            for (k in 0 until 12) {
                val start = k * 30f - 6f
                val wedge = Path().apply {
                    moveTo(center.x, center.y)
                    arcTo(Rect(center, r * appear), start, 12f, false)
                    close()
                }
                drawPath(wedge, Brush.radialGradient(listOf(p.accent.a(0.3f), Color.Transparent), center, r))
            }
        }
        CelebrationAccent.BLOOM_PETALS -> rotate(degrees = t * 15f, pivot = center) {
            glow(p.primary.a(0.35f), 0.7f * r * appear)
            for (k in 0 until 8) {
                val angle = k * TAU / 8f
                val pos = polar(angle, e * 1.35f * appear)
                rotate(degrees = angle * DEG, pivot = pos) {
                    drawPath(leafPath(pos, e * 1.7f * appear, e * 0.55f * appear), p.sparkA.a(0.18f))
                }
            }
        }
        CelebrationAccent.MOON_GLOW -> {
            glow(p.ink.a(0.16f), 0.88f * r * appear)
            glow(p.primary.a(0.45f), 0.55f * r * appear)
        }
    }
}

private fun DrawScope.glow(color: Color, radius: Float) {
    if (radius <= 0f) return
    drawCircle(Brush.radialGradient(listOf(color, Color.Transparent), center, radius), radius, center)
}

private fun DrawScope.dashedRing(color: Color, radius: Float, degrees: Float, u: Float) = rotate(degrees, center) {
    val dashes = PathEffect.dashPathEffect(floatArrayOf(8f * u, 6.4f * u))
    drawCircle(color, radius, center, style = Stroke(1.4f * u, pathEffect = dashes))
}

/** Translate/rotate/scale for the badge as it arrives. */
data class EntranceTransform(val dy: Float, val degrees: Float, val sx: Float, val sy: Float)

internal fun entranceTransform(entrance: CelebrationEntrance, t: Float, u: Float): EntranceTransform = when (entrance) {
    CelebrationEntrance.POP -> easeOutBack(t / 0.42f).let { EntranceTransform(0f, 0f, it, it) }
    CelebrationEntrance.SPIN -> easeOut(t / 0.55f).let {
        EntranceTransform(0f, -160f * (1f - it), 0.3f + 0.7f * it, 0.3f + 0.7f * it)
    }
    CelebrationEntrance.DROP -> EntranceTransform(-(1f - easeOutBounce(t / 0.65f)) * 80f * u, 0f, 1f, 1f)
    CelebrationEntrance.BLOOM -> (0.55f + 0.45f * easeOutBack(t / 0.5f)).let { EntranceTransform(0f, 0f, it, it) }
    CelebrationEntrance.FLIP -> EntranceTransform(
        dy = 0f,
        degrees = 0f,
        sx = abs(cos((1f - easeOut(t / 0.6f)) * 3f * PI.toFloat())).coerceAtLeast(0.02f),
        sy = 1f,
    )
}

private fun easeOutBounce(x: Float): Float {
    val c = clamp01(x)
    val n = 7.5625f
    val d = 2.75f
    return when {
        c < 1f / d -> n * c * c
        c < 2f / d -> (c - 1.5f / d).let { n * it * it + 0.75f }
        c < 2.5f / d -> (c - 2.25f / d).let { n * it * it + 0.9375f }
        else -> (c - 2.625f / d).let { n * it * it + 0.984375f }
    }
}

/** The filled badge (RING stays outline-only). */
internal fun DrawScope.drawFrame(frame: CelebrationFrame, fx: Fx) = with(fx) {
    val shape = frame.outline(center, e)
    if (frame == CelebrationFrame.RING) {
        drawPath(shape, p.primary.a(0.3f))
        drawPath(shape, p.accent, style = Stroke(2.6f * u))
        drawCircle(p.sparkA.a(0.6f), e * 0.84f, center, style = Stroke(1.2f * u))
        return@with
    }
    drawPath(shape, p.primary)
    drawPath(shape, p.accent, style = Stroke(2f * u, join = StrokeJoin.Round))
    if (frame == CelebrationFrame.ROSETTE) drawCircle(p.accent.a(0.5f), e * 0.8f, center, style = Stroke(1.1f * u))
}

/** Draws [path] from its start up to [fraction] of its length. */
private fun DrawScope.drawRevealed(path: Path, fraction: Float, color: Color, width: Float) {
    val f = clamp01(fraction)
    if (f <= 0f) return
    val measure = PathMeasure().apply { setPath(path, false) }
    val part = Path()
    measure.getSegment(0f, measure.length * f, part, true)
    drawPath(part, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

/** cubicTo with control points on the emblem's unit grid (scaled by [s] around the centre). */
private fun Path.unitCubic(fx: Fx, s: Float, vararg xy: Float) {
    fun x(i: Int) = fx.center.x + xy[i] * s
    fun y(i: Int) = fx.center.y + xy[i] * s
    cubicTo(x(0), y(1), x(2), y(3), x(4), y(5))
}

private fun Path.unitMove(fx: Fx, s: Float, x: Float, y: Float) = moveTo(fx.center.x + x * s, fx.center.y + y * s)
private fun Path.unitLine(fx: Fx, s: Float, x: Float, y: Float) = lineTo(fx.center.x + x * s, fx.center.y + y * s)

private fun Fx.unit(points: List<Pair<Float, Float>>, size: Float, closed: Boolean = false) = Path().apply {
    points.forEachIndexed { i, (x, y) ->
        val px = center.x + x * size
        val py = center.y + y * size
        if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
    if (closed) close()
}

/** The symbol inside the badge, revealing itself from ~0.18 s. */
internal fun DrawScope.drawEmblem(emblem: CelebrationEmblem, fx: Fx) = with(fx) {
    val q = clamp01((t - 0.18f) / 0.55f)
    val ink = if (frame == CelebrationFrame.RING) p.accent else p.ink
    val big = e * 0.55f
    when (emblem) {
        CelebrationEmblem.CHECK ->
            drawRevealed(unit(listOf(-0.62f to 0.02f, -0.15f to 0.48f, 0.7f to -0.42f), big), q / 0.8f, ink, 4.6f * u)
        CelebrationEmblem.SPROUT -> drawSprout(this@with, q, ink, big)
        CelebrationEmblem.STAR -> drawStarEmblem(this@with, q, ink, big)
        CelebrationEmblem.LEAF -> drawLeafEmblem(this@with, q, ink, big)
        CelebrationEmblem.SHIELD -> drawShieldEmblem(this@with, q, ink, big)
        CelebrationEmblem.SUN -> drawSunEmblem(this@with, q, ink, big)
        CelebrationEmblem.FLOWER -> drawFlowerEmblem(this@with, q, ink, big)
        CelebrationEmblem.HEART -> drawHeartEmblem(this@with, q, ink, big)
        CelebrationEmblem.PEAK_FLAG -> drawPeakEmblem(this@with, q, ink, big)
        CelebrationEmblem.MOON -> drawMoonEmblem(this@with, q, ink, big)
    }
}

private fun DrawScope.drawSprout(fx: Fx, q: Float, ink: Color, s: Float) = with(fx) {
    drawRevealed(unit(listOf(0f to 0.62f, 0f to -0.05f), s), q / 0.45f, ink, 3.6f * u)
    listOf(Triple(0.25f, 205f, 0.4f), Triple(0f, -25f, 0.55f)).forEach { (attachY, degrees, start) ->
        val pop = easeOutBack((q - start) / 0.3f)
        if (pop <= 0f) return@forEach
        val attach = Offset(center.x, center.y + attachY * s)
        val rad = degrees / DEG
        val mid = attach + Offset(cos(rad), sin(rad)) * (0.32f * s * pop)
        rotate(degrees, mid) { drawPath(leafPath(mid, 0.64f * s * pop, 0.25f * s * pop), ink) }
    }
}

private fun DrawScope.drawStarEmblem(fx: Fx, q: Float, ink: Color, s: Float) = with(fx) {
    val points = (0..10).map { k ->
        val angle = -PI.toFloat() / 2f + k * PI.toFloat() / 5f
        val rad = if (k % 2 == 0) 0.78f else 0.33f
        cos(angle) * rad to sin(angle) * rad
    }
    val star = unit(points, s, closed = true)
    drawPath(star, ink.a((q - 0.5f) / 0.35f))
    drawRevealed(star, q / 0.6f, ink, 2.8f * u)
}

private fun DrawScope.drawLeafEmblem(fx: Fx, q: Float, ink: Color, s: Float) = with(fx) {
    rotate(-45f, center) {
        val leaf = leafPath(center, 1.55f * s, 0.6f * s)
        drawPath(leaf, ink.a(0.4f * (q - 0.55f) / 0.3f))
        drawRevealed(leaf, q / 0.6f, ink, 3f * u)
        drawRevealed(unit(listOf(-0.78f to 0f, 0.7f to 0f), s), (q - 0.5f) / 0.35f, ink, 2f * u)
    }
}

private fun DrawScope.drawShieldEmblem(fx: Fx, q: Float, ink: Color, s: Float) = with(fx) {
    val shield = Path().apply {
        unitMove(fx, s, 0f, -0.82f)
        unitCubic(fx, s, 0.32f, -0.62f, 0.55f, -0.56f, 0.76f, -0.56f)
        unitLine(fx, s, 0.76f, 0f)
        unitCubic(fx, s, 0.76f, 0.45f, 0.4f, 0.74f, 0f, 0.9f)
        unitCubic(fx, s, -0.4f, 0.74f, -0.76f, 0.45f, -0.76f, 0f)
        unitLine(fx, s, -0.76f, -0.56f)
        unitCubic(fx, s, -0.55f, -0.56f, -0.32f, -0.62f, 0f, -0.82f)
        close()
    }
    drawPath(shield, ink.a((q - 0.45f) / 0.25f))
    drawRevealed(shield, q / 0.55f, ink, 2.8f * u)
    val innerCheck = unit(listOf(-0.32f to 0.05f, -0.08f to 0.3f, 0.36f to -0.22f), s)
    drawRevealed(innerCheck, (q - 0.65f) / 0.3f, p.primary, 3.2f * u)
}

private fun DrawScope.drawSunEmblem(fx: Fx, q: Float, ink: Color, s: Float) = with(fx) {
    drawCircle(ink, radius = 0.36f * s * easeOutBack(q / 0.45f), center = center)
    val ray = easeOut((q - 0.35f) / 0.4f)
    if (ray <= 0f) return@with
    rotate(t * 30f, center) {
        for (k in 0 until 8) {
            val angle = k * TAU / 8f
            drawLine(ink, polar(angle, 0.54f * s), polar(angle, (0.54f + 0.34f * ray) * s), 3f * u, StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawFlowerEmblem(fx: Fx, q: Float, ink: Color, s: Float) = with(fx) {
    for (k in 0 until 6) {
        val pop = easeOutBack((q - k * 0.07f) / 0.35f)
        if (pop <= 0f) continue
        val angle = -PI.toFloat() / 2f + k * TAU / 6f + t * 0.3f
        val pos = polar(angle, 0.4f * s * pop)
        rotate(angle * DEG, pos) { drawPath(leafPath(pos, 0.62f * s * pop, 0.24f * s * pop), ink) }
    }
    val heart = easeOutBack((q - 0.55f) / 0.3f)
    val core = if (frame == CelebrationFrame.RING) p.ink else p.accent
    if (heart > 0f) drawCircle(core, radius = 0.2f * s * heart, center = center)
}

private fun DrawScope.drawHeartEmblem(fx: Fx, q: Float, ink: Color, s: Float) = with(fx) {
    val beat = 1f + 0.12f * sin(PI.toFloat() * clamp01((t - 0.95f) / 0.3f))
    scale(beat, center) {
        val heart = Path().apply {
            unitMove(fx, s, 0f, 0.72f)
            unitCubic(fx, s, -0.95f, 0.12f, -0.72f, -0.72f, 0f, -0.3f)
            unitCubic(fx, s, 0.72f, -0.72f, 0.95f, 0.12f, 0f, 0.72f)
            close()
        }
        drawPath(heart, ink.a((q - 0.5f) / 0.3f))
        drawRevealed(heart, q / 0.6f, ink, 2.8f * u)
    }
}

private fun DrawScope.drawPeakEmblem(fx: Fx, q: Float, ink: Color, s: Float) = with(fx) {
    val mountain = unit(listOf(-0.82f to 0.55f, -0.2f to -0.2f, 0f to -0.42f, 0.82f to 0.55f), s)
    val massif = unit(listOf(-0.82f to 0.55f, 0f to -0.42f, 0.82f to 0.55f), s, closed = true)
    drawPath(massif, ink.a(0.28f * (q - 0.4f) / 0.3f))
    drawRevealed(mountain, q / 0.5f, ink, 3f * u)
    drawRevealed(unit(listOf(0f to -0.42f, 0f to -0.98f), s), (q - 0.45f) / 0.2f, ink, 2f * u)
    val flagAlpha = clamp01((q - 0.6f) / 0.2f)
    if (flagAlpha <= 0f) return@with
    val flag = Path().apply {
        val top = center.y - 0.98f * s
        moveTo(center.x, top)
        for (i in 1..10) {
            val x = i / 10f * 0.5f
            lineTo(center.x + x * s, top + sin(x * 12f - t * 10f) * 0.05f * s * x * 2f)
        }
        for (i in 10 downTo 0) {
            val x = i / 10f * 0.5f
            lineTo(center.x + x * s, top + 0.28f * s + sin(x * 12f - t * 10f) * 0.05f * s * x * 2f)
        }
        close()
    }
    drawPath(flag, (if (frame == CelebrationFrame.RING) p.ink else p.accent).a(flagAlpha))
}

private fun DrawScope.drawMoonEmblem(fx: Fx, q: Float, ink: Color, s: Float) = with(fx) {
    val pop = easeOutBack(q / 0.5f)
    if (pop <= 0f) return@with
    val moon = Path().apply {
        op(
            Path().apply { addOval(Rect(center, 0.62f * s * pop)) },
            Path().apply { addOval(Rect(center + Offset(0.3f, -0.22f) * (s * pop), 0.52f * s * pop)) },
            PathOperation.Difference,
        )
    }
    drawPath(moon, ink.a(q / 0.4f))
    val twinkle = easeOutBack((q - 0.55f) / 0.3f)
    if (twinkle > 0f) drawPath(sparklePath(center + Offset(0.42f, -0.18f) * s, 0.2f * s * twinkle), ink)
}
