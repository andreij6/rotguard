@file:Suppress("MagicNumber") // Particle choreography: the numbers are tuned pixel/timing geometry, named inline.

package com.rotguard.app.feature.shield.celebration

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Tapered rays shooting out of the badge, with a shorter second wave. */
internal fun DrawScope.drawStarburst(fx: Fx) = with(fx) {
    sparks.forEachIndexed { i, s ->
        val angle = (i.toFloat() / sparks.size + (s.a - 0.5f) * 0.03f) * TAU
        listOf(0f to 1f, 0.42f to 0.6f).forEachIndexed { wave, (delay, reach) ->
            if (wave == 1 && i % 2 == 1) return@forEachIndexed
            val local = (t - delay - s.d * 0.12f) / (0.65f + 0.3f * s.b)
            if (local <= 0f || local >= 1f) return@forEachIndexed
            val head = e * 1.1f + (r * 0.95f - e * 1.1f) * reach * easeOut(local)
            val tail = (head - (22f + 18f * s.c) * u * (1f - local)).coerceAtLeast(e * 1.05f)
            drawLine(
                color = (if (i % 2 == 0) p.accent else p.sparkB).a(1f - local),
                start = polar(angle, tail),
                end = polar(angle, head),
                strokeWidth = (3.4f * (1f - local) + 0.4f) * u,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Three shells bursting around the badge: trailing sparks that sag with gravity, plus a flash. */
internal fun DrawScope.drawFireworks(fx: Fx) = with(fx) {
    val shells = listOf(Offset(-0.5f, -0.45f), Offset(0.52f, -0.3f), Offset(-0.05f, 0.6f))
    val shellCenters = shells.map { Offset(center.x + it.x * r, center.y + it.y * r) }
    shellCenters.forEachIndexed { k, shell ->
        val local = t - (0.05f + k * 0.3f)
        if (local in 0f..0.16f) {
            val f = local / 0.16f
            drawCircle(p.ink.a(0.7f * (1f - f)), radius = 24f * u * f, center = shell)
        }
    }
    sparks.forEachIndexed { i, s ->
        val k = i % shells.size
        val local = t - (0.05f + k * 0.3f)
        if (local <= 0f || local >= 0.95f) return@forEachIndexed
        val dir = Offset(cos(s.a * TAU), sin(s.a * TAU))
        val speed = (0.28f + 0.22f * s.b) * r
        fun at(time: Float) =
            shellCenters[k] + dir * (speed * easeOut(time / 0.7f)) + Offset(0f, 0.14f * r * time * time)
        val alpha = 1f - (local / 0.95f).let { it * it }
        val color = if (k == 1) p.accent else p.spark(s.d)
        val head = at(local)
        drawLine(color.a(0.6f * alpha), at((local - 0.14f).coerceAtLeast(0f)), head, 2.4f * u, StrokeCap.Round)
        drawCircle(color.a(0.25f * alpha), radius = 6f * u, center = head)
        drawCircle(color.a(alpha), radius = 2.8f * u, center = head)
        if (local > 0.35f && i % 2 == 0) {
            drawPath(sparklePath(head, 4.5f * u * (0.6f + 0.4f * sin(local * 30f + i))), p.ink.a(alpha))
        }
    }
}

/** Twelve stars blink on around the badge, then lines join them into a ring. */
internal fun DrawScope.drawConstellation(fx: Fx) = with(fx) {
    val points = sparks.mapIndexed { i, s ->
        polar((i.toFloat() / sparks.size + (s.a - 0.5f) * 0.05f) * TAU - PI.toFloat() / 2f, (0.6f + 0.3f * s.b) * r)
    }
    fun appear(i: Int) = 0.05f + i * 0.07f
    points.forEachIndexed { i, point ->
        val next = (i + 1) % points.size
        val frac = clamp01((t - appear(if (next == 0) points.size else next)) / 0.18f)
        if (frac > 0f) {
            val end = point + (points[next] - point) * frac
            drawLine(p.accent.a(0.55f), point, end, strokeWidth = 1.2f * u, cap = StrokeCap.Round)
        }
    }
    points.forEachIndexed { i, point ->
        val pop = easeOutBack((t - appear(i)) / 0.25f)
        if (pop <= 0f) return@forEachIndexed
        val size = (3f + 2f * sparks[i].c) * u * pop * (0.8f + 0.2f * sin(t * 9f + i))
        drawCircle(p.sparkB.a(0.25f), radius = size * 2.2f, center = point)
        drawPath(sparklePath(point, size * 1.6f), p.ink)
    }
}

/** Four-point twinkles popping in and out all around the badge. */
internal fun DrawScope.drawSparkles(fx: Fx) = with(fx) {
    sparks.forEach { s ->
        val local = (t - s.d * 1.0f) / 0.55f
        if (local <= 0f || local >= 1f) return@forEach
        val pos = polar(s.a * TAU, e * 1.25f + s.b * (r * 0.95f - e * 1.25f))
        val scale = sin(PI.toFloat() * local)
        rotate(degrees = 45f * local, pivot = pos) {
            drawPath(sparklePath(pos, (6f + 8f * s.c) * u * scale), p.spark(s.d))
        }
    }
}

/** Comets orbiting the badge in alternating directions, trailing fading tails. */
internal fun DrawScope.drawComets(fx: Fx) = with(fx) {
    sparks.forEachIndexed { k, s ->
        val orbit = e * 1.35f + k * 0.14f * r
        val speed = (2.4f + 1.2f * s.c) * (if (k % 2 == 0) 1f else -1f)
        val appear = clamp01((t - k * 0.1f) / 0.25f)
        val color = if (k % 2 == 0) p.accent else p.sparkA
        for (j in 14 downTo 0) {
            val pos = polar(s.a * TAU + speed * (t - j * 0.018f), orbit)
            drawCircle(color.a((1f - j / 15f) * appear), radius = (3.6f - j * 0.2f) * u, center = pos)
        }
        val head = polar(s.a * TAU + speed * t, orbit)
        drawCircle(
            brush = Brush.radialGradient(listOf(p.ink.a(0.6f * appear), Color.Transparent), head, 10f * u),
            radius = 10f * u,
            center = head,
        )
    }
}

/** Glinting streaks falling like golden rain. */
internal fun DrawScope.drawGoldenRain(fx: Fx) = with(fx) {
    sparks.forEach { s ->
        val local = t - s.d * 0.7f
        if (local <= 0f) return@forEach
        val head = Offset(
            center.x + (s.a - 0.5f) * 1.9f * r + local * 0.12f * r,
            center.y - r + local * (1.0f + 0.7f * s.b) * r,
        )
        if (head.y > center.y + r) return@forEach
        val length = (18f + 16f * s.c) * u
        val tail = Offset(head.x - length * 0.12f, head.y - length)
        val color = p.spark(s.d * 0.8f + 0.2f).a(0.55f + 0.45f * sin(local * 18f + s.a * 10f))
        val streak = Brush.linearGradient(listOf(Color.Transparent, color), tail, head)
        drawLine(streak, tail, head, 2.8f * u, StrokeCap.Round)
        drawCircle(p.ink.a(color.alpha * 0.9f), radius = 1.8f * u, center = head)
    }
}

/** Three spiral arms of light swirling out from the badge. */
internal fun DrawScope.drawGalaxy(fx: Fx) = with(fx) {
    sparks.forEachIndexed { i, s ->
        val reveal = clamp01((t * 1.3f - s.b) / 0.2f)
        if (reveal <= 0f) return@forEachIndexed
        val arm = i % 3
        val angle = arm * TAU / 3f + s.b * 4.2f + t * 1.8f + (s.c - 0.5f) * 0.3f
        val radius = e * 1.15f + s.b * (r * 0.92f - e * 1.15f)
        val color = when (arm) {
            0 -> p.accent
            1 -> p.sparkA
            else -> p.sparkB
        }
        val dotRadius = (1.3f + 2.6f * (1f - s.b)) * u
        drawCircle(color.a(reveal * (0.5f + 0.5f * s.d)), radius = dotRadius, center = polar(angle, radius))
    }
}

/** Badge-shaped outlines pulsing outward, alternating spin direction. */
internal fun DrawScope.drawShapePulse(fx: Fx) = with(fx) {
    for (k in 0 until 3) {
        val local = (t - k * 0.3f) / 0.9f
        if (local <= 0f || local >= 1f) continue
        val radius = e * (1.05f + 1.15f * easeOut(local))
        val color = if (k % 2 == 0) p.accent else p.sparkA
        rotate(degrees = 30f * local * (if (k % 2 == 0) 1f else -1f), pivot = center) {
            val width = (2.6f * (1f - local) + 0.5f) * u
            drawPath(frame.outline(center, radius), color.a(1f - local), style = Stroke(width))
        }
    }
    sparks.forEach { s ->
        val local = (t - 0.2f - s.d * 0.6f) / 0.5f
        if (local <= 0f || local >= 1f) return@forEach
        val pos = polar(s.a * TAU, e * (1.3f + 0.9f * s.b))
        drawCircle(p.sparkB.a(sin(PI.toFloat() * local)), radius = 2f * u, center = pos)
    }
}

/** Twinkling six-arm flakes drifting slowly down. */
internal fun DrawScope.drawSnow(fx: Fx) = with(fx) {
    sparks.forEach { s ->
        val local = t - s.d * 0.5f
        if (local <= 0f) return@forEach
        val pos = Offset(
            center.x + (s.a - 0.5f) * 1.9f * r + sin(local * 1.8f + s.c * 6f) * 10f * u,
            center.y - r + s.b * 0.5f * r + local * (0.3f + 0.3f * s.b) * r,
        )
        val alpha = (0.6f + 0.4f * sin(local * 8f + s.d * 9f)) * clamp01(local / 0.2f)
        val size = (3f + 3.5f * s.c) * u
        rotate(degrees = local * 120f * (s.c - 0.5f), pivot = pos) {
            for (k in 0 until 3) {
                val dir = Offset(cos(k * PI.toFloat() / 3f), sin(k * PI.toFloat() / 3f)) * size
                drawLine(p.spark(s.d * 0.75f + 0.25f).a(alpha), pos - dir, pos + dir, 1.3f * u, StrokeCap.Round)
            }
        }
    }
}

/** Diagonal shooting stars with bright sparkle heads, staggered across the sky. */
internal fun DrawScope.drawShootingStars(fx: Fx) = with(fx) {
    val dir = Offset(1f, 0.55f) / sqrt(1f + 0.55f * 0.55f)
    sparks.forEachIndexed { i, s ->
        val local = (t - i * 0.13f - s.d * 0.08f) / 0.6f
        if (local <= 0f || local >= 1f) return@forEachIndexed
        val start = Offset(center.x - r * 0.95f + s.a * r * 0.7f, center.y - r * 0.9f + s.b * r * 0.6f)
        val head = start + dir * (1.3f * r * easeOut(local))
        val tail = head - dir * ((70f + 30f * s.c) * u * (1f - 0.3f * local))
        val color = if (i % 2 == 0) p.ink else p.sparkB
        val streak = Brush.linearGradient(listOf(Color.Transparent, color.a(1f - local)), tail, head)
        drawLine(streak, tail, head, 3.2f * u, StrokeCap.Round)
        drawCircle(color.a(0.3f * (1f - local)), radius = 9f * u, center = head)
        drawPath(sparklePath(head, 9f * u * (1f - 0.5f * local)), color.a(1f - local * 0.6f))
    }
}
