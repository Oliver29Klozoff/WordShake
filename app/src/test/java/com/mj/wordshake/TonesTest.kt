package com.mj.wordshake

import com.mj.wordshake.sound.Note
import com.mj.wordshake.sound.Tones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * The chime waveforms. These are cheap to get subtly wrong in ways that are
 * obvious to the ear and invisible in code: a buffer that clips rasps, and one
 * that starts or ends at full amplitude clicks.
 */
class TonesTest {

    private fun ShortArray.peak() = maxOf { abs(it.toInt()) }

    @Test fun `a rendered note is the length it asked for`() {
        val pcm = Tones.render(listOf(Note(440.0, 100)))
        assertEquals(Tones.SAMPLE_RATE * 100 / 1000, pcm.size)
    }

    @Test fun `note lengths add up`() {
        val pcm = Tones.render(listOf(Note(440.0, 60), Note(880.0, 40)))
        assertEquals(Tones.SAMPLE_RATE * 100 / 1000, pcm.size)
    }

    @Test fun `nothing clips`() {
        for (pcm in allChimes()) {
            assertTrue("peaked at ${pcm.peak()}", pcm.peak() < Short.MAX_VALUE.toInt())
        }
    }

    @Test fun `every chime actually makes a sound`() {
        for (pcm in allChimes()) {
            assertTrue("silent buffer", pcm.peak() > 1000)
        }
    }

    @Test fun `chimes start from silence so they do not click`() {
        for (pcm in allChimes()) {
            assertTrue("opens at ${pcm[0]}", abs(pcm[0].toInt()) < 100)
        }
    }

    @Test fun `chimes decay to near silence so they do not click off`() {
        for (pcm in allChimes()) {
            val tail = pcm.takeLast(64).maxOf { abs(it.toInt()) }
            assertTrue("ends at $tail", tail < pcm.peak() / 8)
        }
    }

    @Test fun `a better word earns a longer flourish`() {
        val small = Tones.found(1).size
        val mid = Tones.found(4).size
        val big = Tones.found(10).size
        assertTrue("small $small, mid $mid", mid > small)
        assertTrue("mid $mid, big $big", big > mid)
    }

    @Test fun `rejection is quieter than success`() {
        assertTrue(Tones.rejected().peak() < Tones.found(4).peak())
        assertTrue(Tones.repeated().peak() < Tones.found(4).peak())
    }

    @Test fun `rendering is deterministic`() {
        assertTrue(Tones.found(4).contentEquals(Tones.found(4)))
    }

    @Test fun `an empty chime renders nothing rather than failing`() {
        assertEquals(0, Tones.render(emptyList()).size)
    }

    private fun allChimes() = listOf(
        Tones.found(1),
        Tones.found(4),
        Tones.found(10),
        Tones.repeated(),
        Tones.rejected(),
    )
}
