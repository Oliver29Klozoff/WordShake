package com.mj.wordshake

import com.mj.wordshake.game.BoardSize
import com.mj.wordshake.game.Dice
import com.mj.wordshake.game.Solver
import com.mj.wordshake.game.WordDictionary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import kotlin.random.Random
import kotlin.system.measureTimeMillis

/**
 * Guards the shipped asset. [WordDictionary] binary-searches, so a word list
 * that is out of byte order fails silently and at random — worth checking on
 * every build rather than trusting the tool that generated it.
 */
class DictionaryAssetTest {

    companion object {
        private lateinit var dict: WordDictionary

        @BeforeClass @JvmStatic fun loadAsset() {
            val candidates = listOf(
                File("src/main/assets/words.txt"),
                File("app/src/main/assets/words.txt"),
            )
            val file = candidates.firstOrNull { it.exists() }
                ?: error("words.txt not found; looked in ${candidates.map { it.absolutePath }}")
            dict = WordDictionary.load(file.inputStream())
        }
    }

    @Test fun `asset is in strict byte order`() {
        var previous = dict.wordAt(0)
        for (i in 1 until dict.size) {
            val current = dict.wordAt(i)
            assertTrue(
                "out of order at $i: '$previous' then '$current'",
                previous < current,
            )
            previous = current
        }
    }

    @Test fun `asset holds only playable lower-case words`() {
        for (i in 0 until dict.size) {
            val w = dict.wordAt(i)
            assertTrue("bad entry '$w'", w.length in 3..16 && w.all { it in 'a'..'z' })
        }
    }

    @Test fun `asset is the expected size`() {
        assertTrue("only ${dict.size} words", dict.size > 150_000)
    }

    @Test fun `common words are present`() {
        for (w in listOf("cat", "quiz", "quiet", "jazz", "vex", "zebra", "boggle", "rate", "syzygy")) {
            assertTrue("missing '$w'", dict.contains(w))
        }
    }

    @Test fun `non-words are absent`() {
        for (w in listOf("qqq", "zzzz", "asdfgh", "aa", "xyzzyx")) {
            assertFalse("unexpectedly present: '$w'", dict.contains(w))
        }
    }

    @Test fun `every entry can be found by lookup`() {
        // Sampled rather than exhaustive: 170k binary searches over the whole
        // list is slow, and a stride catches an index built from bad offsets.
        var i = 0
        while (i < dict.size) {
            val w = dict.wordAt(i)
            assertTrue("could not find '$w' at $i", dict.contains(w))
            i += 97
        }
    }

    @Test fun `solving a classic board is fast and productive`() {
        val board = Dice.shake(BoardSize.CLASSIC, Random(2026))
        lateinit var found: Map<String, List<Int>>
        val ms = measureTimeMillis { found = Solver.solve(board, dict) }
        println("classic board solved in ${ms}ms, ${found.size} words:\n$board")
        assertTrue("solver took ${ms}ms", ms < 4000)
        assertTrue("only ${found.size} words found", found.size > 20)
    }

    @Test fun `solving a big board is fast and productive`() {
        val board = Dice.shake(BoardSize.BIG, Random(2026))
        lateinit var found: Map<String, List<Int>>
        val ms = measureTimeMillis { found = Solver.solve(board, dict) }
        println("big board solved in ${ms}ms, ${found.size} words:\n$board")
        assertTrue("solver took ${ms}ms", ms < 8000)
        assertTrue("only ${found.size} words found", found.size > 50)
    }

    @Test fun `every shaken board yields something to find`() {
        // The real die distributions should never strand a player.
        repeat(40) { seed ->
            val board = Dice.shake(BoardSize.CLASSIC, Random(seed.toLong()))
            val found = Solver.solve(board, dict)
            assertTrue("seed $seed produced a dead board:\n$board", found.size >= 10)
        }
    }

    @Test fun `solved paths always spell their word legally`() {
        val board = Dice.shake(BoardSize.CLASSIC, Random(99))
        for ((word, path) in Solver.solve(board, dict)) {
            assertTrue("$word has an illegal path", board.isLegalPath(path))
            assertEquals(word, board.wordFor(path))
        }
    }
}
