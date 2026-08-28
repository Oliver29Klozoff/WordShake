package com.mj.wordshake

import com.mj.wordshake.game.Board
import com.mj.wordshake.game.BoardSize
import com.mj.wordshake.game.Dice
import com.mj.wordshake.game.Solver
import com.mj.wordshake.game.WordDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.random.Random

/**
 * The playability gate on a shaken board.
 *
 * Plain tumbling — which is what the physical game does — leaves over half of
 * boards with at least one die that has no vowel on it or beside it, a third
 * with two or more, and occasionally a board with no vowels at all worth a
 * single word. These check the gate actually holds, since a regression here is
 * invisible until a player is staring at a corner of consonants.
 */
class BoardQualityTest {

    companion object {
        private lateinit var dict: WordDictionary

        @BeforeClass @JvmStatic fun load() {
            val file = listOf(
                File("src/main/assets/words.txt"),
                File("app/src/main/assets/words.txt"),
            ).first { it.exists() }
            dict = WordDictionary.load(file.inputStream())
        }
    }

    private fun shakes(size: BoardSize, count: Int = 400): List<Board> {
        val rng = Random(4242)
        return List(count) { Dice.shake(size, rng) }
    }

    @Test fun `no shaken board has a die stranded away from every vowel`() {
        for (size in BoardSize.entries) {
            for (board in shakes(size)) {
                assertEquals("stranded die on\n$board", 0, board.dryCellCount)
            }
        }
    }

    @Test fun `every shaken board carries enough vowels to build on`() {
        for (board in shakes(BoardSize.CLASSIC)) {
            assertTrue("only ${board.vowelCount} vowels on\n$board", board.vowelCount >= 4)
        }
        for (board in shakes(BoardSize.BIG)) {
            assertTrue("only ${board.vowelCount} vowels on\n$board", board.vowelCount >= 6)
        }
    }

    @Test fun `no board drowns in vowels either`() {
        for (size in BoardSize.entries) {
            for (board in shakes(size)) {
                assertTrue(
                    "${board.vowelCount} vowels on\n$board",
                    board.vowelCount <= size.cellCount / 2 + 1,
                )
            }
        }
    }

    @Test fun `the gate agrees with what shake produces`() {
        for (size in BoardSize.entries) {
            for (board in shakes(size, 200)) {
                assertTrue("shake returned a board it should reject\n$board", Dice.isPlayable(board))
            }
        }
    }

    @Test fun `a vowelless board is rejected outright`() {
        val barren = Board(BoardSize.CLASSIC, List(16) { "Z" })
        assertFalse(Dice.isPlayable(barren))
        assertEquals(16, barren.dryCellCount)
        assertEquals(0, barren.vowelCount)
    }

    @Test fun `Qu does not count as a vowel for reachability`() {
        // Qu still needs a vowel after it, so a tray of Qu is no better than
        // a tray of consonants.
        val quOnly = Board(BoardSize.CLASSIC, List(16) { "Qu" })
        assertEquals(0, quOnly.vowelCount)
        assertEquals(16, quOnly.dryCellCount)
        assertFalse(Dice.isPlayable(quOnly))
    }

    @Test fun `a vowel makes its own cell and its neighbours reachable`() {
        val one = Board(
            BoardSize.CLASSIC,
            listOf(
                "Z", "Z", "Z", "Z",
                "Z", "A", "Z", "Z",
                "Z", "Z", "Z", "Z",
                "Z", "Z", "Z", "Z",
            ),
        )
        // The A covers itself and its eight neighbours; the other seven are dry.
        assertEquals(16 - 9, one.dryCellCount)
    }

    @Test fun `gated boards are worth playing`() {
        val counts = shakes(BoardSize.CLASSIC, 120).map {
            Solver.solve(it, dict, BoardSize.CLASSIC.minWordLength).size
        }
        assertTrue("worst board had only ${counts.min()} words", counts.min() >= 10)
        assertTrue("median was ${counts.sorted()[counts.size / 2]}", counts.sorted()[counts.size / 2] >= 60)
    }

    @Test fun `the gate does not bias the dice themselves`() {
        // Only the arrangement is rejected, so every letter the real dice can
        // show must still turn up across enough shakes.
        val seen = shakes(BoardSize.CLASSIC, 1500).flatMap { it.faces }.toSet()
        val possible = Dice.CLASSIC.flatten().toSet()
        assertEquals("letters missing: ${possible - seen}", possible, seen)
    }
}
