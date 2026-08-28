package com.mj.wordshake.game

import kotlin.random.Random

/** "Qu" is not here: it needs a vowel after it, so it builds like a consonant. */
private const val VOWELS = "AEIOU"

/**
 * Board geometry. Boggle-style boards are square; the two sizes here mirror the
 * classic 4x4 game and the 5x5 "big" variant, which raises the minimum word
 * length because the larger board makes short words trivial to find.
 */
enum class BoardSize(val dim: Int, val minWordLength: Int, val label: String) {
    CLASSIC(4, 3, "4 x 4"),
    BIG(5, 4, "5 x 5");

    val cellCount: Int get() = dim * dim
}

/**
 * A rolled board. [faces] is row-major and each entry is a single upper-case
 * letter except for "Qu", which occupies one die but counts as two letters.
 */
class Board(val size: BoardSize, val faces: List<String>) {

    init {
        require(faces.size == size.cellCount) {
            "expected ${size.cellCount} faces for $size, got ${faces.size}"
        }
    }

    val dim: Int get() = size.dim

    fun row(index: Int): Int = index / dim
    fun col(index: Int): Int = index % dim

    /** Neighbour indices for every cell, including diagonals. */
    val neighbours: Array<IntArray> = Array(size.cellCount) { i ->
        val r = i / dim
        val c = i % dim
        val out = ArrayList<Int>(8)
        for (dr in -1..1) for (dc in -1..1) {
            if (dr == 0 && dc == 0) continue
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until dim && nc in 0 until dim) out.add(nr * dim + nc)
        }
        out.toIntArray()
    }

    fun isAdjacent(a: Int, b: Int): Boolean = b in neighbours[a]

    /** The word a path spells, upper-case, with "Qu" expanded to two letters. */
    fun wordFor(path: List<Int>): String = buildString {
        for (i in path) append(faces[i])
    }.uppercase()

    /**
     * Faces that are a plain vowel. "Qu" is deliberately not one: it still
     * needs a vowel after it to spell anything, so it behaves like a consonant
     * when you are looking for something to build on.
     */
    val vowelCount: Int get() = faces.count { it.length == 1 && it[0] in VOWELS }

    /**
     * Cells with no vowel on them and none on any neighbour. A die like that
     * is dead weight — nothing can be spelled through it — and a cluster of
     * them is a corner of the board the player simply cannot use.
     */
    val dryCellCount: Int
        get() = (0 until size.cellCount).count { cell ->
            !isVowel(cell) && neighbours[cell].none { isVowel(it) }
        }

    private fun isVowel(cell: Int) = faces[cell].length == 1 && faces[cell][0] in VOWELS

    /** A path is legal when every step moves to a fresh, adjacent cell. */
    fun isLegalPath(path: List<Int>): Boolean {
        if (path.isEmpty()) return false
        if (path.any { it !in 0 until size.cellCount }) return false
        if (path.toHashSet().size != path.size) return false
        for (i in 1 until path.size) {
            if (!isAdjacent(path[i - 1], path[i])) return false
        }
        return true
    }

    override fun toString(): String =
        (0 until size.cellCount).chunked(dim).joinToString("\n") { r ->
            r.joinToString(" ") { faces[it].padEnd(2) }
        }
}

/**
 * The physical dice. These are the real distributions: [CLASSIC] is the 1983
 * Boggle revision and [BIG] is the 25-die Big Boggle set. They matter — the
 * letter frequencies are tuned so that a shaken board is nearly always
 * playable, which a naive weighted-random fill does not achieve.
 */
object Dice {

    private val FACE = Regex("Qu|[A-Z]")

    private fun die(spec: String): List<String> = FACE.findAll(spec).map { it.value }.toList()

    val CLASSIC: List<List<String>> = listOf(
        "AAEEGN", "ABBJOO", "ACHOPS", "AFFKPS",
        "AOOTTW", "CIMOTU", "DEILRX", "DELRVY",
        "DISTTY", "EEGHNW", "EEINSU", "EHRTVW",
        "EIOSST", "ELRTTY", "HIMNQuU", "HLNNRZ",
    ).map(::die)

    val BIG: List<List<String>> = listOf(
        "AAAFRS", "AAEEEE", "AAFIRS", "ADENNN", "AEEEEM",
        "AEEGMU", "AEGMNN", "AFIRSY", "BJKQuXZ", "CCNSTW",
        "CEIILT", "CEILPT", "CEIPST", "DDHNOT", "DHHLOR",
        "DHHNOT", "DHLNOR", "EIIITT", "EMOTTT", "ENSSSU",
        "FIPRSY", "GORRVW", "HIPRRY", "NOOTUW", "OOOTTU",
    ).map(::die)

    fun forSize(size: BoardSize): List<List<String>> = when (size) {
        BoardSize.CLASSIC -> CLASSIC
        BoardSize.BIG -> BIG
    }

    /**
     * Rolls the dice and tumbles them into the tray, re-tumbling while the
     * result is unplayable.
     *
     * The real game has the same flaw and simply lives with it: over half of
     * plain shakes leave at least one die with no vowel on it or beside it, a
     * third leave two or more, and a board can come up with no vowels at all
     * and a single findable word. On a table you shrug and reshake; on a phone
     * it just looks broken. So the tray is tumbled again rather than dealt.
     *
     * Only the arrangement is rejected, never a die face, so the letter
     * frequencies stay those of the real dice.
     */
    fun shake(size: BoardSize, rng: Random = Random.Default): Board {
        var fallback: Board? = null
        var fewestDry = Int.MAX_VALUE

        repeat(MAX_TUMBLES) {
            val board = tumble(size, rng)
            if (isPlayable(board)) return board
            // Keep the least bad, so a run of poor luck can never leave the
            // player worse off than a single plain shake would have.
            if (board.dryCellCount < fewestDry) {
                fewestDry = board.dryCellCount
                fallback = board
            }
        }
        return fallback ?: tumble(size, rng)
    }

    /** One roll of every die, tumbled into position. */
    private fun tumble(size: BoardSize, rng: Random): Board {
        val rolled = forSize(size).mapTo(ArrayList()) { it[rng.nextInt(it.size)] }
        rolled.shuffle(rng)
        return Board(size, rolled)
    }

    /**
     * Every die must have a vowel within reach, and the board must hold enough
     * vowels to build on without drowning in them.
     */
    fun isPlayable(board: Board): Boolean {
        if (board.dryCellCount > 0) return false
        val vowels = board.vowelCount
        return vowels >= minVowels(board.size) && vowels <= maxVowels(board.size)
    }

    private fun minVowels(size: BoardSize) = when (size) {
        BoardSize.CLASSIC -> 4
        BoardSize.BIG -> 6
    }

    private fun maxVowels(size: BoardSize) = size.cellCount / 2 + 1

    /**
     * Enough attempts that failing all of them is vanishingly unlikely, few
     * enough that a shake stays instant — one tumble costs a few microseconds.
     */
    private const val MAX_TUMBLES = 40
}

/**
 * Scoring, by letter count rather than die count.
 *
 * House rule: a three-letter word is worth its printed 1, and everything from
 * four letters up scores double the printed value. Short words stay worth
 * finding, but the reward for pushing a word out grows twice as fast.
 */
object Scoring {

    /**
     * Bumped whenever the table below changes. Best scores are filed under it,
     * so a score set on one table is never shown as beaten by a score set on a
     * different one — the two are not comparable.
     */
    const val VERSION = 3

    /** The length from which the doubling applies. */
    const val BONUS_FROM = 4

    /** The printed Boggle table, before the house rule. */
    private fun base(length: Int): Int = when (length) {
        0, 1, 2 -> 0
        3, 4 -> 1
        5 -> 2
        6 -> 3
        7 -> 5
        else -> 11
    }

    fun score(word: String): Int {
        val printed = base(word.length)
        return if (word.length >= BONUS_FROM) printed * 2 else printed
    }
}

data class FoundWord(val word: String, val path: List<Int>, val points: Int)
