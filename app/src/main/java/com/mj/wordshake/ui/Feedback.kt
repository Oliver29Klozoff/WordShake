package com.mj.wordshake.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.mj.wordshake.game.Verdict
import com.mj.wordshake.sound.Tones

/**
 * Buzz and chime on a submitted word.
 *
 * The waveforms are rendered once on first use and replayed from memory, so a
 * quick run of found words does not pay to synthesise the same chime each time.
 */
class Feedback(context: Context) {

    private val appContext = context.applicationContext

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Vibrator::class.java)
        }?.takeIf { it.hasVibrator() }
    }

    private val smallWin by lazy { Tones.found(1) }
    private val midWin by lazy { Tones.found(4) }
    private val bigWin by lazy { Tones.found(10) }
    private val repeated by lazy { Tones.repeated() }
    private val rejected by lazy { Tones.rejected() }

    fun play(verdict: Verdict, points: Int, haptics: Boolean, sound: Boolean) {
        if (haptics) vibrate(verdict)
        if (sound) playPcm(waveFor(verdict, points))
    }

    private fun waveFor(verdict: Verdict, points: Int): ShortArray = when (verdict) {
        Verdict.ACCEPTED -> when {
            points >= 10 -> bigWin
            points >= 4 -> midWin
            else -> smallWin
        }

        Verdict.REPEAT -> repeated
        else -> rejected
    }

    private fun vibrate(verdict: Verdict) {
        val effect = when (verdict) {
            Verdict.ACCEPTED -> VibrationEffect.createOneShot(28, ACCEPT_AMPLITUDE)
            // A double tap reads as refusal without needing to be stronger.
            else -> VibrationEffect.createWaveform(longArrayOf(0, 18, 55, 18), -1)
        }
        runCatching { vibrator?.vibrate(effect) }
    }

    /**
     * Each play gets its own track, released when the buffer runs out. Audio
     * hardware can refuse a track when the device is short of voices, and a
     * missing chime must never take the round down with it.
     */
    private fun playPcm(pcm: ShortArray) {
        runCatching {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(Tones.SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(pcm.size * 2)
                .build()

            track.write(pcm, 0, pcm.size)
            track.notificationMarkerPosition = pcm.size
            track.setPlaybackPositionUpdateListener(
                object : AudioTrack.OnPlaybackPositionUpdateListener {
                    override fun onMarkerReached(playing: AudioTrack) {
                        runCatching { playing.release() }
                    }

                    override fun onPeriodicNotification(playing: AudioTrack) = Unit
                },
            )
            track.play()
        }
    }

    /** Nothing is held open between plays, so there is nothing to release. */
    fun release() = Unit

    private companion object {
        const val ACCEPT_AMPLITUDE = 120
    }
}
