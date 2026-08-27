package com.mj.wordshake.game

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Maps touch positions onto board cells.
 *
 * The subtle part is [cellNear]. Claiming the whole cell square breaks diagonal
 * traces: a straight drag from one die to its diagonal neighbour clips the
 * corner of an orthogonal neighbour on the way, and because that neighbour is
 * genuinely adjacent it gets spliced into the word — swipe A to D, get A-B-D.
 *
 * So a drag only claims a cell once it comes within [HIT_RADIUS] of that cell's
 * centre. On a perfect diagonal the nearest the finger ever gets to the wrong
 * centre is 0.707 of a cell, comfortably outside the radius, while the two
 * cells actually being joined are passed through dead-on. The gaps between
 * those circles are dead zones, which is what makes the gesture feel precise
 * rather than twitchy.
 */
class Grid(
    private val dim: Int,
    private val width: Float,
    private val height: Float,
    hitRadius: Float = DEFAULT_HIT_RADIUS,
) {
    private val cellWidth = width / dim
    private val cellHeight = height / dim
    private val radius = min(cellWidth, cellHeight) * hitRadius

    /** The cell containing [x], [y], however near its edge. Used for taps. */
    fun cellAt(x: Float, y: Float): Int? {
        if (x < 0f || y < 0f || x >= width || y >= height) return null
        val col = (x / cellWidth).toInt().coerceIn(0, dim - 1)
        val row = (y / cellHeight).toInt().coerceIn(0, dim - 1)
        return row * dim + col
    }

    /** The cell whose centre [x], [y] is close to, or null in the gaps. */
    fun cellNear(x: Float, y: Float): Int? {
        val cell = cellAt(x, y) ?: return null
        val dx = x - (cell % dim + 0.5f) * cellWidth
        val dy = y - (cell / dim + 0.5f) * cellHeight
        return if (dx * dx + dy * dy <= radius * radius) cell else null
    }

    /**
     * Reports the cells a drag crosses between two pointer samples.
     *
     * Pointer events arrive too far apart to trust individually: a quick flick
     * can jump a whole die, and with [cellNear] the targets are smaller, so a
     * skipped sample means a stalled word. Walking the segment in sub-cell
     * steps keeps a fast trace as reliable as a slow one.
     */
    fun walk(fromX: Float, fromY: Float, toX: Float, toY: Float, onCell: (Int) -> Unit) {
        val step = min(cellWidth, cellHeight) / 4f
        val dx = toX - fromX
        val dy = toY - fromY
        val distance = kotlin.math.sqrt(dx * dx + dy * dy)
        val steps = max(1, ceil(distance / step).toInt())
        // From zero, not one: the starting point is part of the segment, and
        // at a tight radius the first step alone can already clear the cell the
        // finger is standing on. Re-reporting a cell is harmless, since the
        // trace rules ignore a move onto the die already at the end.
        for (i in 0..steps) {
            val t = i.toFloat() / steps
            cellNear(fromX + dx * t, fromY + dy * t)?.let(onCell)
        }
    }

    companion object {
        /**
         * Fraction of a cell, from its centre, that a drag must reach. Sits
         * just inside the drawn die, so the rule is simply "be on the die".
         */
        const val DEFAULT_HIT_RADIUS = 0.42f
    }
}
