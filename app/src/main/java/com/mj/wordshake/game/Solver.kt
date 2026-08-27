package com.mj.wordshake.game

/**
 * Exhaustive board search. Used to grade the round: the player's score only
 * means something next to the number of words that were actually there.
 */
object Solver {

    /**
     * Every word findable on [board], mapped to one path that spells it.
     *
     * A depth-first walk from every cell, pruned the moment the letters so far
     * are not a prefix of any word. Without that prune a 5x5 board has billions
     * of paths; with it the walk visits a few hundred thousand nodes and
     * finishes in well under a second.
     */
    fun solve(board: Board, dict: WordDictionary): Map<String, List<Int>> {
        val cells = board.size.cellCount
        val minLength = board.size.minWordLength
        val faces = Array(cells) { board.faces[it].lowercase() }
        val visited = BooleanArray(cells)
        val path = IntArray(cells)
        val word = StringBuilder(cells * 2)
        val found = HashMap<String, List<Int>>()

        fun walk(cell: Int, depth: Int) {
            val face = faces[cell]
            word.append(face)
            val probe = dict.probe(word)
            if (probe != WordDictionary.NONE) {
                visited[cell] = true
                path[depth] = cell

                if (probe and WordDictionary.EXACT != 0 && word.length >= minLength) {
                    val text = word.toString().uppercase()
                    if (text !in found) found[text] = path.copyOf(depth + 1).asList()
                }
                for (next in board.neighbours[cell]) {
                    if (!visited[next]) walk(next, depth + 1)
                }
                visited[cell] = false
            }
            word.setLength(word.length - face.length)
        }

        for (cell in 0 until cells) walk(cell, 0)
        return found
    }

    /** The score a perfect player would post on this board. */
    fun perfectScore(words: Collection<String>): Int = words.sumOf { Scoring.score(it) }
}
