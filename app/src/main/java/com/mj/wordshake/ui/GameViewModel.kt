package com.mj.wordshake.ui

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mj.wordshake.game.Board
import com.mj.wordshake.game.BoardSize
import com.mj.wordshake.game.Dice
import com.mj.wordshake.game.FoundWord
import com.mj.wordshake.game.Scoring
import com.mj.wordshake.game.Solver
import com.mj.wordshake.game.TapOutcome
import com.mj.wordshake.game.Tracing
import com.mj.wordshake.game.Verdict
import com.mj.wordshake.game.WordDictionary
import com.mj.wordshake.game.judge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.ceil

enum class Phase { LOADING, READY, PLAYING, PAUSED, RESULTS }

/** The outcome of the last submission, shown briefly over the tray. */
data class Flash(val id: Long, val word: String, val verdict: Verdict, val points: Int)

data class UiState(
    val phase: Phase = Phase.LOADING,
    val boardSize: BoardSize = BoardSize.CLASSIC,
    val roundSeconds: Int = DEFAULT_ROUND,
    val board: Board? = null,
    val secondsLeft: Int = DEFAULT_ROUND,
    val path: List<Int> = emptyList(),
    val found: List<FoundWord> = emptyList(),
    val score: Int = 0,
    val best: Int = 0,
    val flash: Flash? = null,
    val solution: Map<String, List<Int>> = emptyMap(),
    val solving: Boolean = true,
) {
    val currentWord: String get() = board?.wordFor(path).orEmpty()
    val minWordLength: Int get() = boardSize.minWordLength
    val canSubmit: Boolean get() = currentWord.length >= minWordLength

    /** Words that were on the board but never found. */
    val missed: List<String>
        get() {
            val got = found.mapTo(HashSet()) { it.word }
            return solution.keys.filterNot { it in got }
        }

    companion object {
        const val DEFAULT_ROUND = 180
    }
}

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val prefs = app.getSharedPreferences("wordshake", Context.MODE_PRIVATE)

    private var dictionary: WordDictionary? = null
    private var timerJob: Job? = null
    private var solveJob: Job? = null
    private var flashJob: Job? = null
    private var deadline = 0L
    private var flashSeq = 0L

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                WordDictionary.load(getApplication<Application>().assets.open(WORD_ASSET))
            }
            dictionary = loaded
            shake()
        }
    }

    // --- round setup ------------------------------------------------------

    /** Tumble a fresh board and return to the pre-round screen. */
    fun shake() {
        timerJob?.cancel()
        flashJob?.cancel()
        val size = _state.value.boardSize
        val board = Dice.shake(size)
        _state.update {
            it.copy(
                phase = Phase.READY,
                board = board,
                secondsLeft = it.roundSeconds,
                path = emptyList(),
                found = emptyList(),
                score = 0,
                flash = null,
                solution = emptyMap(),
                solving = true,
                best = prefs.getInt(bestKey(size, it.roundSeconds), 0),
            )
        }
        solve(board)
    }

    fun setBoardSize(size: BoardSize) {
        if (size == _state.value.boardSize) return
        _state.update { it.copy(boardSize = size) }
        shake()
    }

    fun setRoundSeconds(seconds: Int) {
        if (seconds == _state.value.roundSeconds) return
        _state.update {
            it.copy(
                roundSeconds = seconds,
                secondsLeft = seconds,
                best = prefs.getInt(bestKey(it.boardSize, seconds), 0),
            )
        }
    }

    /**
     * Grades the board in the background while the player works on it, so the
     * results screen can appear the moment the clock stops.
     */
    private fun solve(board: Board) {
        val dict = dictionary ?: return
        solveJob?.cancel()
        solveJob = viewModelScope.launch {
            val words = withContext(Dispatchers.Default) { Solver.solve(board, dict) }
            _state.update {
                if (it.board === board) it.copy(solution = words, solving = false) else it
            }
        }
    }

    // --- clock ------------------------------------------------------------

    fun start() {
        if (_state.value.phase != Phase.READY) return
        deadline = SystemClock.elapsedRealtime() + _state.value.roundSeconds * 1000L
        _state.update { it.copy(phase = Phase.PLAYING) }
        tick()
    }

    fun pause() {
        if (_state.value.phase != Phase.PLAYING) return
        timerJob?.cancel()
        _state.update { it.copy(phase = Phase.PAUSED, path = emptyList()) }
    }

    fun resume() {
        if (_state.value.phase != Phase.PAUSED) return
        deadline = SystemClock.elapsedRealtime() + _state.value.secondsLeft * 1000L
        _state.update { it.copy(phase = Phase.PLAYING) }
        tick()
    }

    /**
     * Polls a deadline rather than counting down tick by tick, so the clock
     * stays true even when a coroutine is delayed or the round is paused.
     */
    private fun tick() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val left = ceil((deadline - SystemClock.elapsedRealtime()) / 1000.0)
                    .toInt().coerceAtLeast(0)
                _state.update { if (it.secondsLeft == left) it else it.copy(secondsLeft = left) }
                if (left == 0) {
                    finish()
                    return@launch
                }
                delay(TICK_MS)
            }
        }
    }

    /** Ends the round early; also called when the clock runs out. */
    fun finish() {
        timerJob?.cancel()
        val s = _state.value
        if (s.phase == Phase.RESULTS || s.phase == Phase.READY) return
        val key = bestKey(s.boardSize, s.roundSeconds)
        if (s.score > prefs.getInt(key, 0)) {
            prefs.edit().putInt(key, s.score).apply()
        }
        _state.update {
            it.copy(
                phase = Phase.RESULTS,
                path = emptyList(),
                flash = null,
                best = prefs.getInt(key, 0),
            )
        }
    }

    // --- tracing ----------------------------------------------------------

    /** A drag has begun on [cell]; discard whatever was being built. */
    fun onTraceStart(cell: Int) {
        if (_state.value.phase != Phase.PLAYING) return
        _state.update { it.copy(path = listOf(cell)) }
    }

    /** The finger has moved over [cell]. */
    fun onTraceMove(cell: Int) {
        val s = _state.value
        if (s.phase != Phase.PLAYING) return
        val board = s.board ?: return
        val next = Tracing.dragTo(board, s.path, cell)
        if (next != s.path) _state.update { it.copy(path = next) }
    }

    fun onTraceEnd() {
        if (_state.value.phase != Phase.PLAYING) return
        submit()
    }

    fun onTap(cell: Int) {
        val s = _state.value
        if (s.phase != Phase.PLAYING) return
        val board = s.board ?: return
        when (val outcome = Tracing.tapOn(board, s.path, cell)) {
            is TapOutcome.Submit -> submit()
            is TapOutcome.Trace -> _state.update { it.copy(path = outcome.path) }
        }
    }

    fun clearPath() = _state.update { it.copy(path = emptyList()) }

    fun submit() {
        val s = _state.value
        val board = s.board ?: return
        // A one-die trace is almost always a stray touch rather than an
        // attempt at a word, so it clears without scolding the player.
        if (s.path.size < 2) {
            if (s.path.isNotEmpty()) _state.update { it.copy(path = emptyList()) }
            return
        }
        val word = board.wordFor(s.path)
        val verdict = judge(
            board = board,
            dictionary = dictionary,
            alreadyFound = s.found.mapTo(HashSet()) { it.word },
            path = s.path,
        )

        val points = if (verdict == Verdict.ACCEPTED) Scoring.score(word) else 0
        val flash = Flash(flashSeq++, word, verdict, points)

        _state.update {
            it.copy(
                path = emptyList(),
                flash = flash,
                found = if (verdict == Verdict.ACCEPTED) {
                    it.found + FoundWord(word, s.path, points)
                } else it.found,
                score = it.score + points,
            )
        }

        flashJob?.cancel()
        flashJob = viewModelScope.launch {
            delay(FLASH_MS)
            _state.update { if (it.flash?.id == flash.id) it.copy(flash = null) else it }
        }
    }

    private fun bestKey(size: BoardSize, seconds: Int) = "best_${size.name}_$seconds"

    private companion object {
        const val WORD_ASSET = "words.txt"
        const val TICK_MS = 200L
        const val FLASH_MS = 1100L
    }
}
