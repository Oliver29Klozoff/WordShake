package com.mj.wordshake.ui

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.mj.wordshake.game.Verdict

/**
 * Buzz and beep on a submitted word.
 *
 * Tones are generated rather than shipped as assets: the app needs four short
 * blips, which is not worth the APK weight or the decode. [ToneGenerator] holds
 * a real audio track, so it is created on first use and must be released.
 */
class Feedback(context: Context) {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(VibratorManager::class.java)
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Vibrator::class.java)
        }?.takeIf { it.hasVibrator() }
    }

    private var tones: ToneGenerator? = null

    private fun toneGenerator(): ToneGenerator? {
        tones?.let { return it }
        // Can throw if the device is out of audio resources; a missing beep
        // must never take the round down with it.
        return runCatching {
            ToneGenerator(AudioManager.STREAM_MUSIC, TONE_VOLUME)
        }.getOrNull()?.also { tones = it }
    }

    fun play(verdict: Verdict, haptics: Boolean, sound: Boolean) {
        if (haptics) vibrate(verdict)
        if (sound) beep(verdict)
    }

    private fun vibrate(verdict: Verdict) {
        val effect = when (verdict) {
            Verdict.ACCEPTED -> VibrationEffect.createOneShot(28, ACCEPT_AMPLITUDE)
            // A double tap reads as refusal without needing to be stronger.
            else -> VibrationEffect.createWaveform(longArrayOf(0, 18, 55, 18), -1)
        }
        runCatching { vibrator?.vibrate(effect) }
    }

    private fun beep(verdict: Verdict) {
        val tone = when (verdict) {
            Verdict.ACCEPTED -> ToneGenerator.TONE_PROP_BEEP
            Verdict.REPEAT -> ToneGenerator.TONE_PROP_ACK
            else -> ToneGenerator.TONE_PROP_NACK
        }
        runCatching { toneGenerator()?.startTone(tone, TONE_MS) }
    }

    fun release() {
        runCatching { tones?.release() }
        tones = null
    }

    private companion object {
        const val TONE_VOLUME = 70
        const val TONE_MS = 120
        const val ACCEPT_AMPLITUDE = 120
    }
}
