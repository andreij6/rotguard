@file:Suppress("MagicNumber") // Particle choreography: the numbers are tuned pixel/timing geometry, named inline.

package com.rotguard.app.feature.shield.celebration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin

/** Seeded per-particle randoms, each 0..1; every effect reads its own meaning into them. */
data class Spark(val a: Float, val b: Float, val c: Float, val d: Float)

/**
 * One frame of a celebration: [t] seconds since start, canvas [center], half-size [r], badge radius [e],
 * [u] = 1dp in px.
 */
class Fx(
    val t: Float,
    val center: Offset,
    val r: Float,
    val e: Float,
    val u: Float,
    val p: CelebrationPalette,
    val sparks: List<Spark>,
    val frame: CelebrationFrame,
) {
    fun polar(angle: Float, radius: Float): Offset =
        Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
}

internal const val TAU = (2 * PI).toFloat()
internal const val DEG = (180 / PI).toFloat()

internal fun clamp01(x: Float): Float = x.coerceIn(0f, 1f)
internal fun easeOut(x: Float): Float = 1f - (1f - clamp01(x)).pow(3)
internal fun easeInOut(x: Float): Float = clamp01(x).let { it * it * (3f - 2f * it) }
internal fun easeOutBack(x: Float): Float {
    val c = clamp01(x) - 1f
    return 1f + 2.70158f * c * c * c + 1.70158f * c * c
}

/** 0 → 1 over [inDur], holds, then 1 → 0 from [outStart] over [outDur]. */
internal fun fadeInOut(local: Float, inDur: Float, outStart: Float, outDur: Float): Float =
    clamp01(local / inDur) * (1f - clamp01((local - outStart) / outDur))

internal fun Color.a(alpha: Float): Color = copy(alpha = clamp01(alpha) * this.alpha)

/** Almond leaf centred on [c], pointing along +x. */
internal fun leafPath(c: Offset, length: Float, width: Float): Path = Path().apply {
    moveTo(c.x - length / 2f, c.y)
    quadraticTo(c.x, c.y - width, c.x + length / 2f, c.y)
    quadraticTo(c.x, c.y + width, c.x - length / 2f, c.y)
    close()
}

/** Four-point twinkle star. */
internal fun sparklePath(c: Offset, radius: Float): Path = Path().apply {
    val waist = radius * 0.28f
    moveTo(c.x, c.y - radius)
    quadraticTo(c.x + waist, c.y - waist, c.x + radius, c.y)
    quadraticTo(c.x + waist, c.y + waist, c.x, c.y + radius)
    quadraticTo(c.x - waist, c.y + waist, c.x - radius, c.y)
    quadraticTo(c.x - waist, c.y - waist, c.x, c.y - radius)
    close()
}

/** Warm motes rising and flickering like a campfire. */
internal fun DrawScope.drawEmbers(fx: Fx) = with(fx) {
    sparks.forEach { s ->
        val local = t - s.d * 0.25f
        if (local <= 0f) return@forEach
        val pos = Offset(
            center.x + (s.a - 0.5f) * 1.5f * r + sin(local * 3f + s.c * 6f) * 6f * u,
            center.y + (0.15f + s.b * 0.6f) * r - local * (0.35f + 0.5f * s.c) * r,
        )
        val alpha = (0.65f + 0.35f * sin(local * 22f + s.d * 9f)) * fadeInOut(local, 0.15f, 0.8f, 0.6f)
        val color = p.spark(s.d)
        drawCircle(color.a(0.2f * alpha), radius = (6f + 4f * s.b) * u, center = pos)
        drawCircle(color.a(alpha), radius = (1.8f + 2.4f * s.b) * u, center = pos)
    }
}

/** A radial burst of fluttering paper strips that slow down and fall. */
internal fun DrawScope.drawConfetti(fx: Fx) = with(fx) {
    sparks.forEach { s ->
        val local = t - s.d * 0.08f
        if (local <= 0f) return@forEach
        val angle = s.a * TAU
        val dist = (0.9f + 1.1f * s.b) * r / 2.6f * (1f - exp(-2.6f * local))
        val pos = Offset(center.x + cos(angle) * dist, center.y + sin(angle) * dist + 0.5f * r * local * local)
        val flutter = abs(cos(local * 9f + s.c * 6f)).coerceAtLeast(0.15f)
        rotate(degrees = s.c * 360f + local * (s.c - 0.5f) * 900f, pivot = pos) {
            drawRect(
                color = p.spark(s.d).a(fadeInOut(local, 0.05f, 0.9f, 0.55f)),
                topLeft = Offset(pos.x - 4.5f * u, pos.y - 2.2f * u * flutter),
                size = Size(9f * u, 4.4f * u * flutter),
            )
        }
    }
}

/** Petals drifting down from above, swaying and turning. */
internal fun DrawScope.drawPetals(fx: Fx) = with(fx) {
    sparks.forEach { s ->
        val local = t - s.d * 0.35f
        if (local <= 0f) return@forEach
        val pos = Offset(
            center.x + (s.a - 0.5f) * 1.8f * r + sin(local * 2.6f + s.d * 6f) * 16f * u,
            center.y - 0.95f * r - s.b * 0.3f * r + local * (0.55f + 0.45f * s.c) * r,
        )
        val spin = s.c * 360f + local * 160f * (if (s.c > 0.5f) 1f else -1f)
        val alpha = fadeInOut(local, 0.2f, 1.0f, 0.5f)
        rotate(degrees = spin, pivot = pos) {
            drawOval(p.spark(s.d).a(alpha), Offset(pos.x - 6.5f * u, pos.y - 3.6f * u), Size(13f * u, 7.2f * u))
            drawOval(p.ink.a(0.35f * alpha), Offset(pos.x - 3f * u, pos.y - 1.2f * u), Size(6f * u, 2.4f * u))
        }
    }
}

/** Fireflies wandering on lissajous paths with soft pulsing glows. */
internal fun DrawScope.drawFireflies(fx: Fx) = with(fx) {
    sparks.forEach { s ->
        val angle = s.a * TAU + t * 0.5f * (s.c - 0.5f)
        val radius = (0.42f + 0.52f * s.b) * r
        val pos = Offset(
            center.x + cos(angle) * radius + sin(t * (1.6f + s.c * 1.4f) + s.d * 6f) * 18f * u,
            center.y + sin(angle) * radius + cos(t * (1.3f + s.b * 1.2f) + s.a * 6f) * 14f * u,
        )
        val pulse = 0.5f + 0.5f * sin(t * (5f + 3f * s.c) + s.d * 10f)
        val alpha = clamp01((t - s.d * 0.3f) / 0.3f)
        val color = p.spark(s.d)
        drawCircle(
            brush = Brush.radialGradient(listOf(color.a(0.55f * pulse * alpha), Color.Transparent), pos, 13f * u),
            radius = 13f * u,
            center = pos,
        )
        drawCircle(p.ink.a((0.55f + 0.45f * pulse) * alpha), radius = 2.2f * u, center = pos)
    }
}

/** Dandelion seeds lifting off the badge and riding the breeze up and to the right. */
internal fun DrawScope.drawDandelion(fx: Fx) = with(fx) {
    sparks.forEach { s ->
        val local = t - s.d * 0.35f
        if (local <= 0f) return@forEach
        val origin = polar(-PI.toFloat() / 2f + (s.a - 0.5f) * PI.toFloat() * 1.2f, e * 1.05f)
        val pos = Offset(
            origin.x + local * (0.35f + 0.45f * s.b) * r + sin(local * 3f + s.d * 6f) * 8f * u,
            origin.y - local * (0.25f + 0.35f * s.c) * r,
        )
        val alpha = fadeInOut(local, 0.15f, 1.0f, 0.45f)
        val color = p.spark(s.d * 0.7f).a(alpha)
        rotate(degrees = sin(local * 2.5f + s.a * 6f) * 22f, pivot = pos) {
            drawLine(color, pos, Offset(pos.x, pos.y + 8f * u), strokeWidth = 1.1f * u)
            for (k in -3..3) {
                val ray = -PI.toFloat() / 2f + k * 0.3f
                drawLine(color.a(0.85f), pos, Offset(pos.x + cos(ray) * 5f * u, pos.y + sin(ray) * 5f * u), 0.8f * u)
            }
            drawCircle(p.ink.a(alpha), radius = 1.1f * u, center = Offset(pos.x, pos.y + 8f * u))
        }
    }
}

/** Leaves spiralling outward around the badge, each turned along its path. */
internal fun DrawScope.drawLeafVortex(fx: Fx) = with(fx) {
    sparks.forEach { s ->
        val local = t - s.d * 0.15f
        if (local <= 0f) return@forEach
        val dir = if (s.c > 0.5f) 1f else -1f
        val radius = e * 1.15f + (0.35f + 0.6f * s.b) * (r - e * 1.15f) * easeOut(local / 1.2f)
        val angle = s.a * TAU + dir * local * (2.2f + 1.6f * s.c)
        val pos = polar(angle, radius)
        val alpha = fadeInOut(local, 0.2f, 1.05f, 0.45f)
        rotate(degrees = angle * DEG + 90f * dir, pivot = pos) {
            drawPath(leafPath(pos, (10f + 5f * s.b) * u, (4f + 2f * s.b) * u), p.spark(s.d).a(alpha))
        }
    }
}

/** Six vines curling out of the badge, leaves popping open as each one grows past. */
internal fun DrawScope.drawVines(fx: Fx) = with(fx) {
    sparks.forEachIndexed { i, s ->
        val base = (i.toFloat() / sparks.size + (s.a - 0.5f) * 0.12f) * TAU
        val length = (0.5f + 0.32f * s.b) * r
        val grow = easeOut((t - s.d * 0.15f) / 0.9f)
        if (grow <= 0f) return@forEachIndexed
        fun pointAt(f: Float) =
            polar(base + sin(f * 3.2f + s.d * 6f) * 0.28f + (s.c - 0.5f) * 1.2f * f, e * 1.05f + f * length)
        val vine = Path().apply {
            moveTo(pointAt(0f).x, pointAt(0f).y)
            var f = 0.05f
            while (f <= grow) {
                lineTo(pointAt(f).x, pointAt(f).y)
                f += 0.05f
            }
        }
        drawPath(vine, p.sparkA, style = Stroke(width = 2.2f * u, cap = StrokeCap.Round))
        listOf(0.35f, 0.62f, 0.9f).forEachIndexed { k, at ->
            val pop = easeOutBack((grow - at) / 0.15f)
            if (pop <= 0f) return@forEachIndexed
            val pos = pointAt(at)
            rotate(degrees = base * DEG + if (k % 2 == 0) 55f else -55f, pivot = pos) {
                drawPath(leafPath(Offset(pos.x + 5f * u * pop, pos.y), 10f * u * pop, 4f * u * pop), p.sparkB)
            }
        }
    }
}

/** A row of sprouts growing up from a gentle hill, every third one opening a flower. */
internal fun DrawScope.drawMeadow(fx: Fx) = with(fx) {
    val count = sparks.size
    sparks.forEachIndexed { i, s ->
        val across = (i.toFloat() / (count - 1)) * 2f - 1f
        val base = Offset(center.x + across * 0.82f * r, center.y + 0.8f * r - (1f - across * across) * 0.14f * r)
        val grow = easeOutBack((t - 0.1f - i * 0.05f - s.d * 0.08f) / 0.4f)
        if (grow <= 0f) return@forEachIndexed
        val height = (16f + 14f * s.c) * u * grow
        rotate(degrees = sin(t * 5f + i) * 7f, pivot = base) {
            val tip = Offset(base.x, base.y - height)
            drawLine(p.sparkA, base, tip, strokeWidth = 2f * u, cap = StrokeCap.Round)
            val leafMid = Offset(base.x, base.y - height * 0.55f)
            for (side in listOf(-1f, 1f)) {
                rotate(40f * side, leafMid) {
                    drawPath(leafPath(Offset(leafMid.x + 4f * u * side, leafMid.y), 9f * u, 3.5f * u), p.sparkA)
                }
            }
            if (i % 3 == 0 && grow > 0.8f) {
                val bloom = easeOutBack((t - 0.55f - i * 0.05f) / 0.3f)
                for (k in 0 until 5) {
                    val petal = tip + Offset(cos(k * TAU / 5f), sin(k * TAU / 5f)) * (3.5f * u * bloom)
                    drawCircle(p.sparkB, radius = 2.6f * u * bloom, center = petal)
                }
                drawCircle(p.accent, radius = 2f * u * bloom, center = tip)
            }
        }
    }
}

/** Bubbles rising with a wobble, each popping into a little starburst. */
internal fun DrawScope.drawBubbles(fx: Fx) = with(fx) {
    sparks.forEach { s ->
        val local = t - s.d * 0.4f
        if (local <= 0f) return@forEach
        val life = 0.7f + 0.55f * s.c
        val pos = Offset(
            center.x + (s.a - 0.5f) * 1.6f * r + sin(local * 4f + s.d * 6f) * 7f * u,
            center.y + (0.35f + 0.5f * s.b) * r - local * (0.45f + 0.35f * s.c) * r,
        )
        val radius = (4f + 6.5f * s.b) * u
        val color = p.spark(s.d)
        if (local < life) {
            val alpha = clamp01(local / 0.15f)
            drawCircle(color.a(0.12f * alpha), radius = radius, center = pos)
            drawCircle(color.a(0.9f * alpha), radius = radius, center = pos, style = Stroke(1.4f * u))
            val shine = pos + Offset(-radius * 0.38f, -radius * 0.38f)
            drawCircle(p.ink.a(0.75f * alpha), radius = radius * 0.22f, center = shine)
        } else {
            drawBubblePop(pos, radius, (local - life) / 0.2f, color, u)
        }
    }
}

private fun DrawScope.drawBubblePop(pos: Offset, radius: Float, pop: Float, color: Color, u: Float) {
    if (pop >= 1f) return
    for (k in 0 until 6) {
        val dir = Offset(cos(k * TAU / 6f), sin(k * TAU / 6f))
        drawLine(color.a(1f - pop), pos + dir * radius, pos + dir * radius * (1f + 0.9f * pop), 1.3f * u)
    }
}

/** Four rings spreading like a pebble in a pond, droplets hopping as each ring passes. */
internal fun DrawScope.drawRipples(fx: Fx) = with(fx) {
    for (k in 0 until 4) {
        val local = (t - k * 0.22f) / 1.1f
        if (local <= 0f || local >= 1f) continue
        val radius = e * 1.05f + (r * 0.95f - e * 1.05f) * easeOut(local)
        val color = if (k % 2 == 0) p.accent else p.sparkA
        drawCircle(color.a((1f - local) * 0.9f), radius, center, style = Stroke((3.2f * (1f - local) + 0.6f) * u))
    }
    sparks.forEachIndexed { i, s ->
        val ring = i % 4
        val local = (t - ring * 0.22f) / 1.1f
        if (local <= 0f || local >= 1f) return@forEachIndexed
        val ringRadius = e * 1.05f + (r * 0.95f - e * 1.05f) * easeOut(local)
        val hop = sin(PI.toFloat() * clamp01(local * 2.2f)) * (6f + 6f * s.b) * u
        val pos = polar(s.a * TAU, ringRadius) + Offset(0f, -hop)
        drawCircle(p.spark(s.d).a(1f - local), radius = (1.6f + 1.4f * s.c) * u, center = pos)
    }
}
