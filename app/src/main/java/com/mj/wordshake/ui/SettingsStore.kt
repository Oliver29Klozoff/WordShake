package com.mj.wordshake.ui

import android.content.Context
import android.content.SharedPreferences
import com.mj.wordshake.game.BoardSize
import com.mj.wordshake.game.Scoring
import com.mj.wordshake.game.Settings
import com.mj.wordshake.game.ThemeChoice

/**
 * Reads and writes [Settings]. Every getter falls back to the default rather
 * than throwing, so a preferences file left over from an older build — or one
 * naming a board size that no longer exists — degrades instead of crashing.
 */
class SettingsStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("wordshake", Context.MODE_PRIVATE)

    fun load(): Settings {
        val fallback = Settings()
        return Settings(
            boardSize = enumOrNull<BoardSize>(prefs.getString(BOARD_SIZE, null))
                ?: fallback.boardSize,
            roundSeconds = prefs.getInt(ROUND_SECONDS, fallback.roundSeconds),
            minWordLength = prefs.getInt(MIN_LENGTH, 0).takeIf { it > 0 },
            showWordsRemaining = prefs.getBoolean(SHOW_REMAINING, fallback.showWordsRemaining),
            swipeRadius = prefs.getFloat(SWIPE_RADIUS, fallback.swipeRadius)
                .coerceIn(Settings.MIN_SWIPE_RADIUS, Settings.MAX_SWIPE_RADIUS),
            haptics = prefs.getBoolean(HAPTICS, fallback.haptics),
            sound = prefs.getBoolean(SOUND, fallback.sound),
            theme = enumOrNull<ThemeChoice>(prefs.getString(THEME, null)) ?: fallback.theme,
        )
    }

    fun save(settings: Settings) {
        prefs.edit().apply {
            putString(BOARD_SIZE, settings.boardSize.name)
            putInt(ROUND_SECONDS, settings.roundSeconds)
            putInt(MIN_LENGTH, settings.minWordLength ?: 0)
            putBoolean(SHOW_REMAINING, settings.showWordsRemaining)
            putFloat(SWIPE_RADIUS, settings.swipeRadius)
            putBoolean(HAPTICS, settings.haptics)
            putBoolean(SOUND, settings.sound)
            putString(THEME, settings.theme.name)
        }.apply()
    }

    /**
     * Best scores are kept per board size, round length and minimum length,
     * since all three change how many points are on the table — and per
     * [Scoring.VERSION], so that changing the points table retires the old
     * bests rather than leaving them to be beaten by scores the new table
     * inflated. Nothing is deleted; the old keys are simply no longer read.
     */
    fun bestScore(settings: Settings): Int = prefs.getInt(bestKey(settings), 0)

    fun recordScore(settings: Settings, score: Int): Int {
        val key = bestKey(settings)
        if (score > prefs.getInt(key, 0)) prefs.edit().putInt(key, score).apply()
        return prefs.getInt(key, 0)
    }

    private fun bestKey(settings: Settings) = "best" +
        "_s${Scoring.VERSION}" +
        "_${settings.boardSize.name}" +
        "_${settings.roundSeconds}" +
        "_${settings.effectiveMinLength}"

    private inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() }

    private companion object {
        const val BOARD_SIZE = "board_size"
        const val ROUND_SECONDS = "round_seconds"
        const val MIN_LENGTH = "min_length"
        const val SHOW_REMAINING = "show_remaining"
        const val SWIPE_RADIUS = "swipe_radius"
        const val HAPTICS = "haptics"
        const val SOUND = "sound"
        const val THEME = "theme"
    }
}
