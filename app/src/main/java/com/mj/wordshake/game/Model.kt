package com.mj.wordshake.game

import kotlin.random.Random

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

    /** Roll every die, then tumble them into the tray. */
    fun shake(size: BoardSize, rng: Random = Random.Default): Board {
        val rolled = forSize(size).mapTo(ArrayList()) { it[rng.nextInt(it.size)] }
        rolled.shuffle(rng)
        return Board(size, rolled)
    }
}

/** Standard Boggle scoring, by letter count rather than die count. */
object Scoring {
    fun score(word: String): Int = when (word.length) {
        0, 1, 2 -> 0
        3, 4 -> 1
        5 -> 2
        6 -> 3
        7 -> 5
        else -> 11
    }
}

data class FoundWord(val word: String, val path: List<Int>, val points: Int)
