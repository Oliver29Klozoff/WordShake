package com.mj.wordshake

import com.mj.wordshake.update.UpdateChecker.Companion.isNewer
import com.mj.wordshake.update.UpdateChecker.Companion.plainText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Version comparison. Getting this wrong is quiet in both directions: either
 * the app never offers an update that exists, or it offers one forever.
 */
class UpdateTest {

    @Test fun `a later version is newer`() {
        assertTrue(isNewer("v0.5", "0.4"))
        assertTrue(isNewer("v1.0", "0.9"))
        assertTrue(isNewer("v0.10", "0.9"))
    }

    @Test fun `the same version is not newer`() {
        assertFalse(isNewer("v0.4", "0.4"))
        assertFalse(isNewer("0.4", "0.4"))
    }

    @Test fun `an earlier version is not newer`() {
        assertFalse(isNewer("v0.3", "0.4"))
        assertFalse(isNewer("v0.9", "1.0"))
    }

    @Test fun `a leading v on either side is ignored`() {
        assertTrue(isNewer("v0.5", "v0.4"))
        assertFalse(isNewer("0.4", "v0.4"))
        assertTrue(isNewer("V0.5", "0.4"))
    }

    @Test fun `a longer version beats its own prefix`() {
        assertTrue(isNewer("v0.4.1", "0.4"))
        assertFalse(isNewer("v0.4", "0.4.1"))
    }

    @Test fun `trailing junk in a part does not derail the compare`() {
        // Tags like "v0.5-beta" should still read as 0.5 rather than 0.
        assertTrue(isNewer("v0.5-beta", "0.4"))
        assertFalse(isNewer("v0.4-beta", "0.4"))
    }

    @Test fun `a garbage tag never offers an update`() {
        assertFalse(isNewer("", "0.4"))
        assertFalse(isNewer("latest", "0.4"))
    }

    // --- release notes ----------------------------------------------------

    @Test fun `bold and italic markers are stripped`() {
        assertEquals("Diagonals went dead.", plainText("**Diagonals went dead.**"))
        assertEquals("a word", plainText("*a word*"))
        assertEquals("code", plainText("`code`"))
    }

    @Test fun `emphasis inside a sentence is unwrapped in place`() {
        assertEquals(
            "Two things were wrong. Diagonals died in the middle.",
            plainText("Two things were wrong. **Diagonals died in the middle.**"),
        )
    }

    @Test fun `links keep their text and lose their target`() {
        assertEquals("the release", plainText("[the release](https://example.com/x)"))
    }

    @Test fun `headings and bullets become plain lines`() {
        assertEquals("Fixes\n• one\n• two", plainText("## Fixes\n- one\n- two"))
    }

    @Test fun `runs of blank lines collapse and trailing space goes`() {
        assertEquals("a\n\nb", plainText("a\n\n\n\nb  "))
        assertEquals("a\nb", plainText("a  \nb"))
    }

    @Test fun `plain notes are left alone`() {
        assertEquals("Nothing to strip here.", plainText("Nothing to strip here."))
        assertEquals("", plainText(""))
    }

    @Test fun `a real release body reads cleanly`() {
        val body = """
            Another go at diagonal swiping.

            **Diagonals went dead in the middle.** The area of a die that
            counted as "touching it" was a circle.
        """.trimIndent()
        val out = plainText(body)
        assertFalse("asterisks survived: $out", out.contains("*"))
        assertTrue(out.startsWith("Another go at diagonal swiping."))
    }

    @Test fun `every shipped version so far is ordered`() {
        val released = listOf("v0.1", "v0.2", "v0.3", "v0.4")
        for (i in 0 until released.size - 1) {
            assertTrue(
                "${released[i + 1]} should follow ${released[i]}",
                isNewer(released[i + 1], released[i]),
            )
            assertFalse(isNewer(released[i], released[i + 1]))
        }
    }
}
