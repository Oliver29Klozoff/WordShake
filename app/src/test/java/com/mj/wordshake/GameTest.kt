package com.mj.wordshake

import com.mj.wordshake.game.Board
import com.mj.wordshake.game.BoardSize
import com.mj.wordshake.game.Dice
import com.mj.wordshake.game.Scoring
import com.mj.wordshake.game.Solver
import com.mj.wordshake.game.WordDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameTest {

    private fun board(vararg faces: String) =
        Board(BoardSize.CLASSIC, faces.toList())

    /** A fixed 4x4 board used across the solver tests. */
    private val fixed = board(
        "C", "A", "T", "S",
        "O", "R", "E", "D",
        "N", "I", "L", "P",
        "G", "H", "U", "M",
    )

    // --- dice -------------------------------------------------------------

    @Test fun `classic set has sixteen six-sided dice`() {
        assertEquals(16, Dice.CLASSIC.size)
        assertTrue(Dice.CLASSIC.all { it.size == 6 })
    }

    @Test fun `big set has twenty-five six-sided dice`() {
        assertEquals(25, Dice.BIG.size)
        assertTrue(Dice.BIG.all { it.size == 6 })
    }

    @Test fun `both sets carry exactly one Qu face`() {
        assertEquals(1, Dice.CLASSIC.count { "Qu" in it })
        assertEquals(1, Dice.BIG.count { "Qu" in it })
    }

    @Test fun `shake fills the board from the right die set`() {
        for (size in BoardSize.entries) {
            val b = Dice.shake(size, Random(7))
            assertEquals(size.cellCount, b.faces.size)
            assertTrue(b.faces.all { it == "Qu" || (it.length == 1 && it[0] in 'A'..'Z') })
        }
    }

    @Test fun `shake is deterministic for a given seed`() {
        assertEquals(
            Dice.shake(BoardSize.CLASSIC, Random(42)).faces,
            Dice.shake(BoardSize.CLASSIC, Random(42)).faces,
        )
    }

    // --- geometry ---------------------------------------------------------

    @Test fun `corner cells have three neighbours and centre cells have eight`() {
        assertEquals(3, fixed.neighbours[0].size)
        assertEquals(3, fixed.neighbours[15].size)
        assertEquals(5, fixed.neighbours[1].size)
        assertEquals(8, fixed.neighbours[5].size)
    }

    @Test fun `adjacency includes diagonals but not self or distant cells`() {
        assertTrue(fixed.isAdjacent(5, 0))
        assertTrue(fixed.isAdjacent(5, 10))
        assertFalse(fixed.isAdjacent(5, 5))
        assertFalse(fixed.isAdjacent(5, 7))
    }

    @Test fun `adjacency does not wrap around row edges`() {
        assertFalse(fixed.isAdjacent(3, 4))
    }

    @Test fun `legal paths step to fresh adjacent cells`() {
        assertTrue(fixed.isLegalPath(listOf(0, 1, 2, 3)))
        assertFalse(fixed.isLegalPath(listOf(0, 1, 0)))
        assertFalse(fixed.isLegalPath(listOf(0, 2)))
        assertFalse(fixed.isLegalPath(emptyList()))
    }

    @Test fun `wordFor expands Qu to two letters`() {
        val b = board(
            "Qu", "I", "T", "S",
            "A", "B", "C", "D",
            "E", "F", "G", "H",
            "I", "J", "K", "L",
        )
        assertEquals("QUIT", b.wordFor(listOf(0, 1, 2)))
    }

    // --- dictionary -------------------------------------------------------

    private val small = WordDictionary.of(listOf("cat", "cats", "core", "rat", "rate", "tar", "quit"))

    @Test fun `dictionary finds whole words only`() {
        assertTrue(small.contains("cat"))
        assertTrue(small.contains("quit"))
        assertFalse(small.contains("ca"))
        assertFalse(small.contains("catx"))
        assertFalse(small.contains(""))
    }

    @Test fun `dictionary reports live prefixes`() {
        assertTrue(small.hasPrefix("ca"))
        assertTrue(small.hasPrefix("cat"))
        assertFalse(small.hasPrefix("cz"))
        assertFalse(small.hasPrefix("zz"))
    }

    @Test fun `probe reports exact and prefix together`() {
        assertEquals(WordDictionary.EXACT or WordDictionary.PREFIX, small.probe("cat"))
        assertEquals(WordDictionary.PREFIX, small.probe("ca"))
        assertEquals(WordDictionary.NONE, small.probe("cz"))
        assertEquals(WordDictionary.EXACT or WordDictionary.PREFIX, small.probe("rate"))
    }

    @Test fun `dictionary preserves every word it was given`() {
        assertEquals(7, small.size)
        assertEquals(
            listOf("cat", "cats", "core", "quit", "rat", "rate", "tar"),
            (0 until small.size).map { small.wordAt(it) },
        )
    }

    @Test fun `loader tolerates CRLF and blank lines`() {
        val d = WordDictionary.load("cat\r\n\r\ndog\r\n".toByteArray().inputStream())
        assertEquals(2, d.size)
        assertTrue(d.contains("cat"))
        assertTrue(d.contains("dog"))
    }

    // --- solver -----------------------------------------------------------

    @Test fun `solver finds words whose path is traceable`() {
        val found = Solver.solve(fixed, small)
        assertTrue("CAT" in found)
        assertTrue("CATS" in found)
        assertTrue("CORE" in found)
        assertTrue("RAT" in found)
        assertTrue("RATE" in found)
        assertTrue("TAR" in found)
    }

    @Test fun `solver rejects words whose letters are not adjacent`() {
        // CORD is spellable letter-by-letter but R and D are two columns apart.
        val d = WordDictionary.of(listOf("cord"))
        assertTrue(d.contains("cord"))
        assertTrue(Solver.solve(fixed, d).isEmpty())
    }

    @Test fun `solver returns paths that the board agrees spell the word`() {
        for ((word, path) in Solver.solve(fixed, small)) {
            assertTrue("$word has an illegal path", fixed.isLegalPath(path))
            assertEquals(word, fixed.wordFor(path))
        }
    }

    @Test fun `solver never reuses a die within one word`() {
        val d = WordDictionary.of(listOf("cocoa"))
        val b = board(
            "C", "O", "A", "A",
            "A", "A", "A", "A",
            "A", "A", "A", "A",
            "A", "A", "A", "A",
        )
        assertTrue(Solver.solve(b, d).isEmpty())
    }

    @Test fun `solver walks through a Qu die`() {
        val b = board(
            "Qu", "I", "T", "S",
            "A", "B", "C", "D",
            "E", "F", "G", "H",
            "I", "J", "K", "L",
        )
        val found = Solver.solve(b, WordDictionary.of(listOf("quit", "quits")))
        assertEquals(listOf(0, 1, 2), found["QUIT"])
        assertTrue("QUITS" in found)
    }

    @Test fun `solver honours the per-size minimum length`() {
        val d = WordDictionary.of(listOf("cat", "cats"))
        val big = Board(BoardSize.BIG, List(25) { "C" }.toMutableList().also {
            it[0] = "C"; it[1] = "A"; it[2] = "T"; it[3] = "S"
        })
        // CAT is three letters, below Big Boggle's four-letter floor.
        val found = Solver.solve(big, d)
        assertFalse("CAT" in found)
        assertTrue("CATS" in found)
    }

    // --- scoring ----------------------------------------------------------

    @Test fun `scoring follows the standard table`() {
        assertEquals(0, Scoring.score("AT"))
        assertEquals(1, Scoring.score("CAT"))
        assertEquals(1, Scoring.score("CATS"))
        assertEquals(2, Scoring.score("CRATE"))
        assertEquals(3, Scoring.score("CRATES"))
        assertEquals(5, Scoring.score("CREATES"))
        assertEquals(11, Scoring.score("CREATING"))
        assertEquals(11, Scoring.score("CREATIVELY"))
    }

    @Test fun `scoring counts Qu as two letters`() {
        assertEquals(1, Scoring.score("QUIT"))
        assertEquals(2, Scoring.score("QUITS"))
    }
}
