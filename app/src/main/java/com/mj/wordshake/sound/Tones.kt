package com.mj.wordshake.sound

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** One note in a chime. */
data class Note(val hz: Double, val ms: Int, val gain: Double = 1.0)

/**
 * Chimes, rendered to PCM at runtime.
 *
 * Synthesised rather than shipped as audio files: these are a few short blips,
 * and generating them keeps the APK free of assets while letting the reward
 * scale with the word — a long word gets a longer flourish, which a fixed clip
 * could not do without shipping several of them.
 *
 * Deliberately free of Android types so the waveform can be unit-tested; a
 * buffer that clips or that starts abruptly is audible as a click, and that is
 * cheaper to catch here than by ear.
 */
object Tones {

    const val SAMPLE_RATE = 44_100

    /** Peak amplitude, well short of full scale so a chord cannot clip. */
    private const val PEAK = 0.30 * Short.MAX_VALUE

    // A rising major triad reads as approval in a way a flat beep does not.
    private const val A5 = 880.0
    private const val CS6 = 1108.73
    private const val E6 = 1318.51
    private const val A6 = 1760.0

    /**
     * The reward for a found word. Longer words earn more of the arpeggio, so
     * a long word sounds different from a three-letter one.
     */
    fun found(points: Int): ShortArray {
        val notes = when {
            points >= 10 -> listOf(Note(A5, 60), Note(CS6, 60), Note(E6, 60), Note(A6, 150))
            points >= 4 -> listOf(Note(A5, 60), Note(CS6, 60), Note(E6, 130))
            else -> listOf(Note(A5, 60), Note(E6, 120))
        }
        return render(notes)
    }

    /** Already found: neutral, not a telling-off. */
    fun repeated(): ShortArray = render(listOf(Note(659.25, 70, gain = 0.5)))

    /** Not a word: two notes falling, soft rather than harsh. */
    fun rejected(): ShortArray =
        render(listOf(Note(233.08, 70, gain = 0.7), Note(185.0, 110, gain = 0.7)))

    /** Renders [notes] back to back into 16-bit mono PCM. */
    fun render(notes: List<Note>): ShortArray {
        val total = notes.sumOf { samplesFor(it.ms) }
        val out = ShortArray(total)
        var offset = 0
        for (note in notes) {
            val count = samplesFor(note.ms)
            for (i in 0 until count) {
                val seconds = i.toDouble() / SAMPLE_RATE
                val value = sin(2.0 * PI * note.hz * seconds) *
                    envelope(i, count) * note.gain * PEAK
                out[offset + i] = value.toInt().coerceIn(MIN, MAX).toShort()
            }
            offset += count
        }
        return out
    }

    private fun samplesFor(ms: Int) = ms * SAMPLE_RATE / 1000

    /**
     * A short ramp in and an exponential fall away. Both ends matter: starting
     * or stopping at full amplitude puts a step in the waveform, which is heard
     * as a click rather than as a note.
     */
    private fun envelope(index: Int, count: Int): Double {
        val attack = (SAMPLE_RATE * ATTACK_SECONDS).toInt().coerceAtLeast(1)
        val rise = if (index < attack) index.toDouble() / attack else 1.0
        return rise * exp(-DECAY * index.toDouble() / count)
    }

    private const val ATTACK_SECONDS = 0.004
    private const val DECAY = 4.5
    private const val MIN = -32_768
    private const val MAX = 32_767
}
