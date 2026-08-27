package com.mj.wordshake

import com.mj.wordshake.game.Board
import com.mj.wordshake.game.BoardSize
import com.mj.wordshake.game.Grid
import com.mj.wordshake.game.Tracing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Touch-to-cell mapping. A 4x4 grid 400 units square, so each cell is 100 and
 * centres land on 50, 150, 250, 350.
 */
class GridTest {

    private val grid = Grid(dim = 4, width = 400f, height = 400f)

    private fun walked(fromX: Float, fromY: Float, toX: Float, toY: Float): List<Int> {
        val seen = mutableListOf<Int>()
        grid.walk(fromX, fromY, toX, toY) { if (seen.lastOrNull() != it) seen.add(it) }
        return seen
    }

    // --- plain hit testing ------------------------------------------------

    @Test fun `cellAt claims the whole square`() {
        assertEquals(0, grid.cellAt(50f, 50f))
        assertEquals(0, grid.cellAt(1f, 1f))
        assertEquals(0, grid.cellAt(99f, 99f))
        assertEquals(5, grid.cellAt(150f, 150f))
        assertEquals(15, grid.cellAt(399f, 399f))
    }

    @Test fun `cellAt rejects positions off the board`() {
        assertNull(grid.cellAt(-1f, 50f))
        assertNull(grid.cellAt(50f, -1f))
        assertNull(grid.cellAt(400f, 50f))
        assertNull(grid.cellAt(50f, 400f))
    }

    @Test fun `cellNear claims a cell at its centre`() {
        assertEquals(0, grid.cellNear(50f, 50f))
        assertEquals(5, grid.cellNear(150f, 150f))
        assertEquals(15, grid.cellNear(350f, 350f))
    }

    @Test fun `cellNear leaves a dead zone between centres`() {
        // The corner where four cells meet belongs to none of them.
        assertNull(grid.cellNear(100f, 100f))
        // Halfway along an edge, too far from either centre.
        assertNull(grid.cellNear(100f, 50f))
    }

    @Test fun `cellNear still claims a comfortable margin around the centre`() {
        // 40 units out of a 100 cell is inside the radius; a player aiming at
        // the die rather than its exact centre must still register.
        assertEquals(0, grid.cellNear(50f + 40f, 50f))
        assertEquals(0, grid.cellNear(50f, 50f + 40f))
    }

    // --- the diagonal bug -------------------------------------------------

    @Test fun `a diagonal drag does not pick up the cells it passes between`() {
        // Cell 0 to cell 5. Cells 1 and 4 are adjacent to both and their
        // corners lie on the route; neither may be spliced into the word.
        val path = walked(50f, 50f, 150f, 150f)
        assertEquals(listOf(0, 5), path)
    }

    @Test fun `every diagonal step on the board joins exactly two cells`() {
        val centre = { cell: Int -> Pair((cell % 4 + 0.5f) * 100f, (cell / 4 + 0.5f) * 100f) }
        for (from in 0 until 16) {
            for (to in 0 until 16) {
                val dRow = to / 4 - from / 4
                val dCol = to % 4 - from % 4
                if (dRow == 0 || dCol == 0) continue
                if (dRow * dRow != 1 || dCol * dCol != 1) continue
                val (fx, fy) = centre(from)
                val (tx, ty) = centre(to)
                assertEquals("diagonal $from to $to", listOf(from, to), walked(fx, fy, tx, ty))
            }
        }
    }

    @Test fun `a diagonal drag produces a legal two-cell path`() {
        val board = Board(BoardSize.CLASSIC, List(16) { "A" })
        var path = listOf(0)
        for (cell in walked(50f, 50f, 150f, 150f)) {
            path = Tracing.dragTo(board, path, cell)
        }
        assertEquals(listOf(0, 5), path)
        assertTrue(board.isLegalPath(path))
    }

    // --- straight and fast drags ------------------------------------------

    @Test fun `a drag along a row collects every cell in order`() {
        assertEquals(listOf(0, 1, 2, 3), walked(50f, 50f, 350f, 50f))
    }

    @Test fun `a drag down a column collects every cell in order`() {
        assertEquals(listOf(0, 4, 8, 12), walked(50f, 50f, 50f, 350f))
    }

    @Test fun `a long diagonal collects the whole diagonal`() {
        assertEquals(listOf(0, 5, 10, 15), walked(50f, 50f, 350f, 350f))
    }

    @Test fun `a fast flick between distant samples does not skip cells`() {
        // One pointer sample spanning three cells still yields each of them,
        // so the word does not stall when the player moves quickly.
        val path = walked(50f, 50f, 350f, 50f)
        assertEquals(listOf(0, 1, 2, 3), path)
    }

    @Test fun `walking within one cell reports it without repeating`() {
        assertEquals(listOf(0), walked(40f, 40f, 60f, 60f))
    }

    @Test fun `a five by five board scales its hit radius`() {
        val big = Grid(dim = 5, width = 500f, height = 500f)
        assertEquals(0, big.cellNear(50f, 50f))
        assertNull(big.cellNear(100f, 100f))
        val seen = mutableListOf<Int>()
        big.walk(50f, 50f, 150f, 150f) { if (seen.lastOrNull() != it) seen.add(it) }
        assertEquals(listOf(0, 6), seen)
    }
}
