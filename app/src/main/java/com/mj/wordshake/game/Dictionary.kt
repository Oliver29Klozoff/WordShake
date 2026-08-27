package com.mj.wordshake.game

import java.io.InputStream

/**
 * The word list, held as one flat character buffer plus an index of word
 * offsets. Storing 170k words as individual [String] objects would cost
 * roughly 10 MB of heap and 170k allocations at startup; this layout costs a
 * little over 3 MB and allocates two arrays.
 *
 * Lookups are binary searches, so the backing file must be sorted by byte
 * value. Queries take a [CharSequence] rather than a [String] so the solver can
 * probe with a mutable [StringBuilder] without allocating.
 *
 * Keys must be lower-case; use [normalise] for anything that came from a board.
 */
class WordDictionary private constructor(
    private val buf: CharArray,
    private val starts: IntArray,
) {

    /** Number of words held. */
    val size: Int get() = starts.size - 1

    fun wordAt(index: Int): String = String(buf, starts[index], starts[index + 1] - starts[index])

    fun contains(key: CharSequence): Boolean = probe(key) and EXACT != 0

    /** True when at least one word begins with [prefix] — the solver's pruning test. */
    fun hasPrefix(prefix: CharSequence): Boolean = probe(prefix) and PREFIX != 0

    /**
     * Single-search membership test returning [EXACT] and/or [PREFIX] flags.
     *
     * The solver asks both questions at every node of its walk, and both
     * answers fall out of one binary search: if the first word at or after
     * [key] starts with [key] then [key] is a live prefix, and it is itself a
     * word exactly when that candidate is no longer than the key.
     */
    fun probe(key: CharSequence): Int {
        if (key.isEmpty()) return PREFIX
        val i = lowerBound(key)
        if (i >= size || !startsWithAt(i, key)) return NONE
        return if (starts[i + 1] - starts[i] == key.length) EXACT or PREFIX else PREFIX
    }

    /** Index of the first word >= [key], in `0..size`. */
    private fun lowerBound(key: CharSequence): Int {
        var lo = 0
        var hi = size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (compareAt(mid, key) < 0) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun compareAt(index: Int, key: CharSequence): Int {
        val start = starts[index]
        val len = starts[index + 1] - start
        val common = minOf(len, key.length)
        for (i in 0 until common) {
            val d = buf[start + i].code - key[i].code
            if (d != 0) return d
        }
        return len - key.length
    }

    private fun startsWithAt(index: Int, prefix: CharSequence): Boolean {
        val start = starts[index]
        if (starts[index + 1] - start < prefix.length) return false
        for (i in prefix.indices) {
            if (buf[start + i] != prefix[i]) return false
        }
        return true
    }

    companion object {

        const val NONE = 0
        const val PREFIX = 1
        const val EXACT = 2

        /** Board faces are upper-case and may contain "Qu"; the list is lower-case. */
        fun normalise(word: String): String = word.lowercase()

        /**
         * Reads a newline-separated, byte-sorted list. Blank lines are skipped
         * and CRLF is tolerated, so the asset can be edited on Windows without
         * silently corrupting the index.
         */
        fun load(input: InputStream): WordDictionary {
            val bytes = input.use { it.readBytes() }

            var count = 0
            var chars = 0
            var run = 0
            for (b in bytes) {
                if (b == NL || b == CR) {
                    if (run > 0) { count++; chars += run; run = 0 }
                } else run++
            }
            if (run > 0) { count++; chars += run }

            val buf = CharArray(chars)
            val starts = IntArray(count + 1)
            var written = 0
            var wordStart = 0
            var wordIndex = 0
            for (b in bytes) {
                if (b == NL || b == CR) {
                    if (written > wordStart) {
                        wordIndex++
                        starts[wordIndex] = written
                        wordStart = written
                    }
                } else {
                    buf[written++] = b.toInt().toChar().lowercaseChar()
                }
            }
            if (written > wordStart) {
                wordIndex++
                starts[wordIndex] = written
            }
            return WordDictionary(buf, starts)
        }

        /** Test and preview helper: builds a dictionary from an in-memory list. */
        fun of(words: Collection<String>): WordDictionary {
            val sorted = words.map { it.lowercase() }.distinct().sorted()
            return load(sorted.joinToString("\n").toByteArray(Charsets.US_ASCII).inputStream())
        }

        private const val NL: Byte = '\n'.code.toByte()
        private const val CR: Byte = '\r'.code.toByte()
    }
}
