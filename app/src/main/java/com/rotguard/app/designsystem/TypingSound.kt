package com.rotguard.app.designsystem

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.rotguard.app.R
import kotlin.random.Random

private const val MIN_GAP_NANOS = 70_000_000L
private const val TAP_VOLUME = 0.22f
private const val PITCH_SPREAD = 0.08f
private const val SKIP_CHANCE = 0.15f

/**
 * Soft key-tap played while [TypewriterText] types. Quiet, throttled to ~14 taps/s, pitch-varied with the odd
 * skipped tap so it sounds organic rather than mechanical. Silent when the phone is on silent or vibrate.
 */
class TypingSoundPlayer(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val pool = SoundPool.Builder()
        .setMaxStreams(2)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val soundId = pool.load(context, R.raw.typing_tick, 1)
    private var loaded = false
    private var lastTapNanos = 0L

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (id == soundId && status == 0) loaded = true }
    }

    fun onCharacterTyped(character: Char) {
        if (!loaded || character.isWhitespace()) return
        if (audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL) return
        val now = System.nanoTime()
        if (now - lastTapNanos < MIN_GAP_NANOS || Random.nextFloat() < SKIP_CHANCE) return
        lastTapNanos = now
        val rate = 1f + (Random.nextFloat() * 2f - 1f) * PITCH_SPREAD
        pool.play(soundId, TAP_VOLUME, TAP_VOLUME, 0, 0, rate)
    }

    fun release() = pool.release()
}

/** Provided by screens that want typing sounds (onboarding); null elsewhere, so TypewriterText stays silent. */
val LocalTypingSound = staticCompositionLocalOf<TypingSoundPlayer?> { null }

@Composable
fun rememberTypingSoundPlayer(): TypingSoundPlayer {
    val appContext = LocalContext.current.applicationContext
    val player = remember { TypingSoundPlayer(appContext) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
