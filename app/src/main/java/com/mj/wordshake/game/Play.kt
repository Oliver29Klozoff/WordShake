package com.mj.wordshake.game

/** How a submitted word was judged. */
enum class Verdict { ACCEPTED, UNKNOWN, REPEAT, TOO_SHORT }

/** What a tap on a die should do next. */
sealed interface TapOutcome {
    data class Trace(val path: List<Int>) : TapOutcome
    data object Submit : TapOutcome
}

/**
 * The rules governing a trace in progress. These live apart from the ViewModel
 * because they are the fiddliest part of the game to get right and the easiest
 * to get wrong: every branch here is a gesture that a player will make by
 * accident within the first minute of playing.
 */
object Tracing {

    /**
     * Where a drag that has reached [cell] leaves the trace.
     *
     * Sliding back onto the previous die retracts the last step, so a player
     * who overshoots can correct without lifting a finger. Every other illegal
     * move — a jump, a revisit — leaves the trace untouched rather than
     * breaking it, because the finger is often just passing over a die on its
     * way somewhere legal.
     */
    fun dragTo(board: Board, path: List<Int>, cell: Int): List<Int> = when {
        path.isEmpty() -> listOf(cell)
        cell == path.last() -> path
        path.size >= 2 && cell == path[path.size - 2] -> path.dropLast(1)
        cell in path -> path
        !board.isAdjacent(path.last(), cell) -> path
        else -> path + cell
    }

    /**
     * Tap input, for players who would rather not drag: the first tap opens a
     * word, each further tap extends it, and tapping the last die again submits
     * it. A tap that cannot extend the word starts a new one, which is what
     * someone reaching for a fresh word actually means by it.
     */
    fun tapOn(board: Board, path: List<Int>, cell: Int): TapOutcome = when {
        path.isEmpty() -> TapOutcome.Trace(listOf(cell))
        cell == path.last() -> TapOutcome.Submit
        cell !in path && board.isAdjacent(path.last(), cell) -> TapOutcome.Trace(path + cell)
        else -> TapOutcome.Trace(listOf(cell))
    }
}

/**
 * Judges a submitted path. Order matters: length and repetition are checked
 * before the dictionary so the player gets the specific reason rather than a
 * blanket rejection.
 */
fun judge(
    board: Board,
    dictionary: WordDictionary?,
    alreadyFound: Set<String>,
    path: List<Int>,
): Verdict {
    val word = board.wordFor(path)
    return when {
        word.length < board.size.minWordLength -> Verdict.TOO_SHORT
        word in alreadyFound -> Verdict.REPEAT
        !board.isLegalPath(path) -> Verdict.UNKNOWN
        dictionary == null -> Verdict.UNKNOWN
        !dictionary.contains(WordDictionary.normalise(word)) -> Verdict.UNKNOWN
        else -> Verdict.ACCEPTED
    }
}
