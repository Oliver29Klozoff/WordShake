package com.mj.wordshake

import com.mj.wordshake.game.Grid
import com.mj.wordshake.game.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How much of a drag between two dice registers as neither of them.
 *
 * This is the thing that makes a swipe feel responsive or dead, and it is the
 * reason the hit region is a square. Diagonal neighbours are 1.41 cells apart
 * against 1.0 for orthogonal ones, so a circular region — which reaches the
 * same distance whichever way you go — leaves a far longer unclaimed stretch
 * on a diagonal, and diagonals feel like they stop responding halfway across.
 */
class DeadZoneTest {

    private val cell = 100f
    private val grid = Grid(dim = 4, width = 400f, height = 400f)

    /** Fraction of a straight drag between two cell centres claimed by neither. */
    private fun deadFraction(from: Int, to: Int): Float {
        fun centre(c: Int) = Pair((c % 4 + 0.5f) * cell, (c / 4 + 0.5f) * cell)
        val (fx, fy) = centre(from)
        val (tx, ty) = centre(to)
        val samples = 2000
        var dead = 0
        for (i in 0..samples) {
            val t = i.toFloat() / samples
            val hit = grid.cellNear(fx + (tx - fx) * t, fy + (ty - fy) * t)
            if (hit == null) dead++
        }
        return dead.toFloat() / (samples + 1)
    }

    @Test fun `a diagonal drag is no deader than a straight one`() {
        val straight = deadFraction(0, 1)
        val diagonal = deadFraction(0, 5)
        assertTrue(
            "diagonal dead zone $diagonal far exceeds straight $straight",
            diagonal <= straight + 0.02f,
        )
    }

    @Test fun `neither direction goes dead for most of the drag`() {
        assertTrue("straight ${deadFraction(0, 1)}", deadFraction(0, 1) < 0.25f)
        assertTrue("diagonal ${deadFraction(0, 5)}", deadFraction(0, 5) < 0.25f)
    }

    @Test fun `every direction out of a middle cell behaves the same`() {
        // Cell 5 has all eight neighbours; none should be harder to reach.
        val fractions = grid.neighboursOf(5).map { deadFraction(5, it) }
        val spread = (fractions.max() - fractions.min())
        assertTrue("dead zone varies by $spread across directions", spread <= 0.02f)
    }

    @Test fun `the whole slider range keeps diagonals reachable`() {
        var reach = Settings.MIN_SWIPE_RADIUS
        while (reach <= Settings.MAX_SWIPE_RADIUS) {
            val g = Grid(4, 400f, 400f, reach)
            val seen = mutableListOf<Int>()
            g.walk(50f, 50f, 150f, 150f) { if (seen.lastOrNull() != it) seen.add(it) }
            assertEquals("reach $reach broke a diagonal", listOf(0, 5), seen)
            reach += 0.01f
        }
    }

    /**
     * How far a diagonal drag may wander toward the die it passes before that
     * die joins the word. With a square region the tolerance is exactly
     * `0.5 - reach` of a cell, because the drag has to get inside the passed
     * die's region on both axes at once. That is the price of the shorter dead
     * zone, and it is why the slider is worth having: a lower reach buys back
     * room for a wobbly finger.
     */
    private fun bowTolerance(reach: Float): Float {
        val g = Grid(4, 400f, 400f, reach)
        var bow = 0f
        while (bow < 50f) {
            val seen = mutableListOf<Int>()
            g.walk(50f, 50f, 100f - bow, 100f + bow) { if (seen.lastOrNull() != it) seen.add(it) }
            g.walk(100f - bow, 100f + bow, 150f, 150f) { if (seen.lastOrNull() != it) seen.add(it) }
            if (seen.distinct() != listOf(0, 5)) return bow
            bow += 1f
        }
        return 50f
    }

    @Test fun `a straight diagonal is never spliced at any reach`() {
        assertTrue("default reach splices a straight diagonal", bowTolerance(0.42f) > 0f)
        assertTrue("max reach splices a straight diagonal", bowTolerance(Settings.MAX_SWIPE_RADIUS) > 0f)
    }

    @Test fun `lowering the reach buys tolerance for a wobbly finger`() {
        val forgiving = bowTolerance(Settings.MAX_SWIPE_RADIUS)
        val precise = bowTolerance(Settings.MIN_SWIPE_RADIUS)
        assertTrue(
            "precise reach tolerates $precise, forgiving tolerates $forgiving",
            precise > forgiving,
        )
    }

    @Test fun `the default tolerates the wobble the geometry promises`() {
        // 0.5 - 0.42 = 0.08 of a 100 unit cell, so a drag may stray about
        // eight units off the line before the die it passes is claimed.
        val tolerance = bowTolerance(0.42f)
        assertTrue("default tolerates only $tolerance units", tolerance >= 7f)
    }

    private fun Grid.neighboursOf(cell: Int): List<Int> {
        val row = cell / 4
        val col = cell % 4
        val out = mutableListOf<Int>()
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val r = row + dr
            val c = col + dc
            if (r in 0..3 && c in 0..3) out.add(r * 4 + c)
        }
        return out
    }
}
