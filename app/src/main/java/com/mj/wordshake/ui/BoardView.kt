package com.mj.wordshake.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.wordshake.game.Board
import com.mj.wordshake.game.Grid

/**
 * The tray of dice and the trace laid over it.
 *
 * The whole grid takes one gesture handler rather than giving each die its own,
 * because a trace is a single drag that crosses many dice: hit testing against
 * the uniform grid is both simpler and more forgiving than relying on child
 * hit boxes, which drop the pointer in the gaps between dice.
 */
@Composable
fun BoardView(
    board: Board,
    path: List<Int>,
    enabled: Boolean,
    swipeRadius: Float,
    onTraceStart: (Int) -> Unit,
    onTraceMove: (Int) -> Unit,
    onTraceEnd: () -> Unit,
    onTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dim = board.dim

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val trayPad = side * 0.035f
        val gridSide = side - trayPad * 2
        val cell = gridSide / dim

        Box(
            Modifier
                .size(side)
                .shadow(18.dp, RoundedCornerShape(side * 0.07f))
                .background(Palette.Tray, RoundedCornerShape(side * 0.07f))
                .border(1.dp, Palette.TrayEdge, RoundedCornerShape(side * 0.07f))
                .padding(trayPad),
        ) {
            Box(
                Modifier
                    .size(gridSide)
                    .pointerInput(board, enabled, swipeRadius) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            // Measured per gesture, not once when this handler
                            // starts: none of its keys change on a rotation, so
                            // a grid built up front would go on mapping touches
                            // against the previous orientation's board.
                            val grid = Grid(
                                dim,
                                size.width.toFloat(),
                                size.height.toFloat(),
                                swipeRadius,
                            )
                            // A press anywhere on a die opens the word, but
                            // once travelling the stricter centre test applies.
                            val start = grid.cellAt(down.position.x, down.position.y)
                            var dragging = false
                            var last = down.position

                            while (true) {
                                val event = awaitPointerEvent()
                                val change: PointerInputChange =
                                    event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break

                                if (!dragging &&
                                    (change.position - down.position).getDistance() >
                                    viewConfiguration.touchSlop
                                ) {
                                    dragging = true
                                    start?.let(onTraceStart)
                                }
                                if (dragging) {
                                    grid.walk(
                                        last.x, last.y,
                                        change.position.x, change.position.y,
                                        onTraceMove,
                                    )
                                    last = change.position
                                    change.consume()
                                }
                            }

                            // A press that never travelled is a tap on the die
                            // it started on; anything else is a finished trace.
                            if (dragging) onTraceEnd() else start?.let(onTap)
                        }
                    },
            ) {
                TraceOverlay(dim, path, Palette.PickedFace, Modifier.matchParentSize())

                for (index in board.faces.indices) {
                    val order = path.indexOf(index)
                    Die(
                        face = board.faces[index],
                        picked = order >= 0,
                        seed = index,
                        cell = cell,
                        modifier = Modifier.offset(
                            x = cell * (index % dim),
                            y = cell * (index / dim),
                        ),
                    )
                }
            }
        }
    }
}

/** The ribbon joining picked dice, drawn behind them so letters stay readable. */
@Composable
private fun TraceOverlay(dim: Int, path: List<Int>, ink: Color, modifier: Modifier) {
    Canvas(modifier) {
        if (path.size < 2) return@Canvas
        val w = size.width / dim
        val h = size.height / dim
        fun centre(i: Int) = Offset((i % dim + 0.5f) * w, (i / dim + 0.5f) * h)

        val line = Path().apply {
            val first = centre(path.first())
            moveTo(first.x, first.y)
            for (i in 1 until path.size) {
                val p = centre(path[i])
                lineTo(p.x, p.y)
            }
        }
        drawPath(
            path = line,
            color = ink.copy(alpha = 0.55f),
            style = Stroke(width = w * 0.30f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round),
        )
    }
}

/**
 * One die. The vertical gradient plus the pale inner cap reads as a moulded
 * plastic bevel, and a fixed pseudo-random tilt keeps the tray from looking
 * like a spreadsheet without ever re-rolling on recomposition.
 */
@Composable
private fun Die(
    face: String,
    picked: Boolean,
    seed: Int,
    cell: Dp,
    modifier: Modifier = Modifier,
) {
    val corner = cell * 0.18f
    val shape = RoundedCornerShape(corner)

    val top by animateColorAsState(
        if (picked) Palette.PickedHigh else Palette.DieHigh, tween(120), label = "top",
    )
    val mid by animateColorAsState(
        if (picked) Palette.PickedFace else Palette.DieFace, tween(120), label = "mid",
    )
    val low by animateColorAsState(
        if (picked) Palette.PickedLow else Palette.DieLow, tween(120), label = "low",
    )
    val ink by animateColorAsState(
        if (picked) Palette.PickedText else Palette.DieText, tween(120), label = "ink",
    )
    val lift by animateFloatAsState(if (picked) 1.06f else 1f, tween(120), label = "lift")

    val tilt = remember(seed) { ((seed * 37 + 11) % 5 - 2) * 0.55f }

    Box(
        modifier
            .size(cell)
            .padding(cell * 0.055f)
            .graphicsLayer {
                rotationZ = tilt
                scaleX = lift
                scaleY = lift
            }
            .shadow(if (picked) 10.dp else 5.dp, shape)
            .background(Brush.verticalGradient(0f to top, 0.55f to mid, 1f to low), shape)
            .border(1.dp, low, shape),
        contentAlignment = Alignment.Center,
    ) {
        // A soft cap across the top third, the way light sits on a real die.
        Box(
            Modifier
                .matchParentSize()
                .padding(cell * 0.06f)
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.35f),
                        0.45f to Color.Transparent,
                    ),
                    RoundedCornerShape(corner * 0.7f),
                ),
        )
        Text(
            text = face.uppercase().let { if (it == "QU") "Qu" else it },
            color = ink,
            fontSize = (cell.value * if (face.length > 1) 0.30f else 0.44f).sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}
