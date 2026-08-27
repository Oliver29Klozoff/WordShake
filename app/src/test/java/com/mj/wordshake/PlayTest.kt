package com.mj.wordshake

import com.mj.wordshake.game.Board
import com.mj.wordshake.game.BoardSize
import com.mj.wordshake.game.TapOutcome
import com.mj.wordshake.game.Tracing
import com.mj.wordshake.game.Verdict
import com.mj.wordshake.game.WordDictionary
import com.mj.wordshake.game.judge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The gesture rules. Every case here is something a thumb does by accident.
 */
class PlayTest {

    //  0 C   1 A   2 T   3 S
    //  4 O   5 R   6 E   7 D
    //  8 N   9 I  10 L  11 P
    // 12 G  13 H  14 U  15 M
    private val board = Board(
        BoardSize.CLASSIC,
        listOf(
            "C", "A", "T", "S",
            "O", "R", "E", "D",
            "N", "I", "L", "P",
            "G", "H", "U", "M",
        ),
    )

    private val dict = WordDictionary.of(listOf("cat", "cats", "rat", "core"))

    // --- dragging ---------------------------------------------------------

    @Test fun `a drag on an empty trace opens the word`() {
        assertEquals(listOf(5), Tracing.dragTo(board, emptyList(), 5))
    }

    @Test fun `a drag onto an adjacent die extends the word`() {
        assertEquals(listOf(0, 1), Tracing.dragTo(board, listOf(0), 1))
        assertEquals(listOf(0, 1, 2), Tracing.dragTo(board, listOf(0, 1), 2))
    }

    @Test fun `a drag onto a diagonal neighbour extends the word`() {
        assertEquals(listOf(0, 5), Tracing.dragTo(board, listOf(0), 5))
    }

    @Test fun `staying on the same die changes nothing`() {
        assertEquals(listOf(0, 1), Tracing.dragTo(board, listOf(0, 1), 1))
    }

    @Test fun `sliding back onto the previous die retracts a step`() {
        assertEquals(listOf(0, 1), Tracing.dragTo(board, listOf(0, 1, 2), 1))
    }

    @Test fun `retracting repeatedly walks the whole word back`() {
        var path = listOf(0, 1, 2, 3)
        path = Tracing.dragTo(board, path, 2)
        path = Tracing.dragTo(board, path, 1)
        path = Tracing.dragTo(board, path, 0)
        assertEquals(listOf(0), path)
    }

    @Test fun `crossing a die already in the word is ignored`() {
        // 0 is in the trace but is not the step before 2, so it must not retract.
        assertEquals(listOf(0, 1, 2), Tracing.dragTo(board, listOf(0, 1, 2), 0))
    }

    @Test fun `crossing a non-adjacent die is ignored rather than breaking the word`() {
        assertEquals(listOf(0, 1), Tracing.dragTo(board, listOf(0, 1), 15))
    }

    @Test fun `a drag never produces an illegal path`() {
        // Walk a long, deliberately awkward route and check every state.
        var path = listOf(0)
        for (cell in listOf(1, 2, 3, 7, 6, 5, 4, 8, 9, 10, 11, 15, 14, 13, 12, 0, 9, 2)) {
            path = Tracing.dragTo(board, path, cell)
            assertTrue("illegal after reaching $cell: $path", board.isLegalPath(path))
        }
    }

    // --- tapping ----------------------------------------------------------

    @Test fun `the first tap opens a word`() {
        assertEquals(TapOutcome.Trace(listOf(5)), Tracing.tapOn(board, emptyList(), 5))
    }

    @Test fun `a tap on an adjacent die extends the word`() {
        assertEquals(TapOutcome.Trace(listOf(0, 1)), Tracing.tapOn(board, listOf(0), 1))
    }

    @Test fun `a second tap on the last die submits`() {
        assertEquals(TapOutcome.Submit, Tracing.tapOn(board, listOf(0, 1), 1))
    }

    @Test fun `a tap on an unreachable die starts a new word`() {
        assertEquals(TapOutcome.Trace(listOf(15)), Tracing.tapOn(board, listOf(0, 1), 15))
    }

    @Test fun `a tap on a die already used starts a new word`() {
        assertEquals(TapOutcome.Trace(listOf(0)), Tracing.tapOn(board, listOf(0, 1), 0))
    }

    // --- judging ----------------------------------------------------------

    @Test fun `a real word on a legal path is accepted`() {
        assertEquals(Verdict.ACCEPTED, judge(board, dict, emptySet(), listOf(0, 1, 2)))
    }

    @Test fun `a word already found is a repeat`() {
        assertEquals(Verdict.REPEAT, judge(board, dict, setOf("CAT"), listOf(0, 1, 2)))
    }

    @Test fun `a word under the minimum length is rejected for length`() {
        assertEquals(Verdict.TOO_SHORT, judge(board, dict, emptySet(), listOf(0, 1)))
    }

    @Test fun `length is checked before the dictionary`() {
        // "CA" is neither long enough nor a word; the player should be told the
        // useful thing, which is that it is too short.
        assertEquals(Verdict.TOO_SHORT, judge(board, dict, emptySet(), listOf(0, 1)))
    }

    @Test fun `a non-word is unknown`() {
        assertEquals(Verdict.UNKNOWN, judge(board, dict, emptySet(), listOf(4, 5, 6)))
    }

    @Test fun `a real word on a broken path is rejected`() {
        // C-A-T spelled by jumping to a non-adjacent T.
        val jumbled = Board(
            BoardSize.CLASSIC,
            listOf(
                "C", "A", "X", "T",
                "X", "X", "X", "X",
                "X", "X", "X", "X",
                "X", "X", "X", "X",
            ),
        )
        assertEquals(Verdict.UNKNOWN, judge(jumbled, dict, emptySet(), listOf(0, 1, 3)))
    }

    @Test fun `judging without a dictionary rejects rather than crashes`() {
        assertEquals(Verdict.UNKNOWN, judge(board, null, emptySet(), listOf(0, 1, 2)))
    }

    @Test fun `the big board raises the minimum length`() {
        val big = Board(BoardSize.BIG, List(25) { "C" })
        assertEquals(3, BoardSize.CLASSIC.minWordLength)
        assertEquals(4, big.size.minWordLength)
        assertEquals(Verdict.TOO_SHORT, judge(big, dict, emptySet(), listOf(0, 1, 5)))
    }
}
