package com.mj.wordshake.game

/** Which colour scheme to paint. */
enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/**
 * Everything the player can change. Kept free of Android types so the rules
 * that depend on it stay unit-testable; persistence lives in SettingsStore.
 */
data class Settings(
    val boardSize: BoardSize = BoardSize.CLASSIC,
    val roundSeconds: Int = 180,
    /** Null means follow the board: three letters on 4x4, four on 5x5. */
    val minWordLength: Int? = null,
    val showWordsRemaining: Boolean = false,
    val swipeRadius: Float = Grid.DEFAULT_HIT_RADIUS,
    val haptics: Boolean = true,
    val sound: Boolean = false,
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
) {
    /** The shortest word that scores on the current board. */
    val effectiveMinLength: Int get() = minWordLength ?: boardSize.minWordLength

    companion object {
        val ROUND_CHOICES = listOf(60, 120, 180, 300)
        val LENGTH_CHOICES = listOf(null, 3, 4, 5)

        /**
         * Bounds for the swipe radius, as a fraction of a cell. The ceiling
         * matters: a diagonal drag passes 0.707 of a cell from the centre of
         * the die it goes by, so a radius approaching that would start
         * grabbing neighbours again — the bug this slider is tuning around.
         */
        const val MIN_SWIPE_RADIUS = 0.28f
        const val MAX_SWIPE_RADIUS = 0.50f
    }
}
