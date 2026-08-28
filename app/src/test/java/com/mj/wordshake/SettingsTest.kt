package com.mj.wordshake

import com.mj.wordshake.game.Board
import com.mj.wordshake.game.BoardSize
import com.mj.wordshake.game.Grid
import com.mj.wordshake.game.Settings
import com.mj.wordshake.game.Solver
import com.mj.wordshake.game.Verdict
import com.mj.wordshake.game.WordDictionary
import com.mj.wordshake.game.judge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The settings that change the rules, rather than just the paint. */
class SettingsTest {

    private val board = Board(
        BoardSize.CLASSIC,
        listOf(
            "C", "A", "T", "S",
            "O", "R", "E", "D",
            "N", "I", "L", "P",
            "G", "H", "U", "M",
        ),
    )
    private val dict = WordDictionary.of(listOf("cat", "cats", "core", "rat", "rate", "tar"))

    // --- minimum word length ----------------------------------------------

    @Test fun `auto length follows the board`() {
        assertEquals(3, Settings(boardSize = BoardSize.CLASSIC).effectiveMinLength)
        assertEquals(4, Settings(boardSize = BoardSize.BIG).effectiveMinLength)
    }

    @Test fun `an explicit length overrides the board default`() {
        assertEquals(5, Settings(boardSize = BoardSize.CLASSIC, minWordLength = 5).effectiveMinLength)
        assertEquals(3, Settings(boardSize = BoardSize.BIG, minWordLength = 3).effectiveMinLength)
    }

    @Test fun `raising the minimum rejects words that were fine before`() {
        val cat = listOf(0, 1, 2)
        assertEquals(Verdict.ACCEPTED, judge(board, dict, emptySet(), cat, minLength = 3))
        assertEquals(Verdict.TOO_SHORT, judge(board, dict, emptySet(), cat, minLength = 4))
    }

    @Test fun `the solver honours the minimum it is given`() {
        val atThree = Solver.solve(board, dict, minLength = 3)
        val atFive = Solver.solve(board, dict, minLength = 5)
        assertTrue("CAT" in atThree)
        assertFalse("CAT" in atFive)
        assertTrue("CATS" in atThree)
        assertFalse("CATS" in atFive)
        assertTrue(atFive.keys.all { it.length >= 5 })
    }

    @Test fun `lowering the minimum on a big board finds shorter words`() {
        val big = Board(BoardSize.BIG, List(25) { "X" }.toMutableList().also {
            it[0] = "C"; it[1] = "A"; it[2] = "T"
        })
        assertFalse("CAT" in Solver.solve(big, dict))
        assertTrue("CAT" in Solver.solve(big, dict, minLength = 3))
    }

    // --- swipe radius -----------------------------------------------------

    @Test fun `the default swipe radius is inside the allowed range`() {
        val d = Settings().swipeRadius
        assertTrue(d in Settings.MIN_SWIPE_RADIUS..Settings.MAX_SWIPE_RADIUS)
    }

    @Test fun `the radius ceiling stays clear of the diagonal danger point`() {
        // A diagonal drag passes 0.707 of a cell from the centre it goes by.
        // The ceiling must leave real margin or the fixed bug comes back.
        assertTrue(
            "ceiling ${Settings.MAX_SWIPE_RADIUS} is too close to 0.707",
            Settings.MAX_SWIPE_RADIUS < 0.6f,
        )
    }

    @Test fun `a forgiving radius claims more of the cell`() {
        val precise = Grid(4, 400f, 400f, Settings.MIN_SWIPE_RADIUS)
        val forgiving = Grid(4, 400f, 400f, Settings.MAX_SWIPE_RADIUS)
        // 45 units from the centre of a 100 cell.
        assertNull(precise.cellNear(95f, 50f))
        assertEquals(0, forgiving.cellNear(95f, 50f))
    }

    @Test fun `diagonals stay clean at every radius the slider allows`() {
        var radius = Settings.MIN_SWIPE_RADIUS
        while (radius <= Settings.MAX_SWIPE_RADIUS) {
            val grid = Grid(4, 400f, 400f, radius)
            val seen = mutableListOf<Int>()
            grid.walk(50f, 50f, 150f, 150f) { if (seen.lastOrNull() != it) seen.add(it) }
            assertEquals("radius $radius spliced a die into a diagonal", listOf(0, 5), seen)
            radius += 0.01f
        }
    }

    // --- defaults ---------------------------------------------------------

    @Test fun `defaults match the game as it shipped`() {
        val s = Settings()
        assertEquals(BoardSize.CLASSIC, s.boardSize)
        assertEquals(180, s.roundSeconds)
        assertNull(s.minWordLength)
        assertFalse(s.showWordsRemaining)
        assertTrue(s.haptics)
        assertTrue("the chime should be on out of the box", s.sound)
    }

    @Test fun `every offered round length is a whole number of minutes`() {
        assertTrue(Settings.ROUND_CHOICES.all { it % 60 == 0 && it > 0 })
    }
}
