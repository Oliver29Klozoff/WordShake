package com.mj.wordshake.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.mj.wordshake.game.ThemeChoice

/**
 * One colour scheme. The properties are capitalised so call sites still read
 * `Palette.DieFace`, exactly as they did before a second theme existed.
 */
@Immutable
data class Skin(
    val Background: Color,
    val Surface: Color,
    val SurfaceHigh: Color,
    val Tray: Color,
    val TrayEdge: Color,
    val DieHigh: Color,
    val DieFace: Color,
    val DieLow: Color,
    val DieText: Color,
    val PickedHigh: Color,
    val PickedFace: Color,
    val PickedLow: Color,
    val PickedText: Color,
    val Valid: Color,
    val Invalid: Color,
    val Duplicate: Color,
    val TextPrimary: Color,
    val TextMuted: Color,
    val Accent: Color,
    val isDark: Boolean,
)

/** Bone dice on a slate tray, lit by a teal accent. */
val DarkSkin = Skin(
    Background = Color(0xFF16191D),
    Surface = Color(0xFF22272E),
    SurfaceHigh = Color(0xFF2E353E),
    Tray = Color(0xFF1B2026),
    TrayEdge = Color(0xFF0E1114),
    DieHigh = Color(0xFFFFFFFF),
    DieFace = Color(0xFFF2F4F5),
    DieLow = Color(0xFFD6DBDE),
    DieText = Color(0xFF1A1D21),
    PickedHigh = Color(0xFF6FD9CB),
    PickedFace = Color(0xFF35B6A6),
    PickedLow = Color(0xFF1E8F81),
    PickedText = Color(0xFF06231F),
    // Greener than the teal accent on purpose: a verdict must not be mistaken
    // for the selection colour.
    Valid = Color(0xFF4FC17A),
    Invalid = Color(0xFFE5645B),
    Duplicate = Color(0xFF7C8894),
    TextPrimary = Color(0xFFECEFF2),
    TextMuted = Color(0xFF8A949E),
    Accent = Color(0xFF35B6A6),
    isDark = true,
)

/** The same dice on a pale slate tray in daylight. */
val LightSkin = Skin(
    Background = Color(0xFFF4F6F7),
    Surface = Color(0xFFE6EAEC),
    SurfaceHigh = Color(0xFFD5DBDF),
    Tray = Color(0xFFBFC8CE),
    TrayEdge = Color(0xFF9EA9B1),
    DieHigh = Color(0xFFFFFFFF),
    DieFace = Color(0xFFFAFBFB),
    DieLow = Color(0xFFE0E5E8),
    DieText = Color(0xFF16191D),
    PickedHigh = Color(0xFF7FE0D2),
    // Deeper than the dark theme's teal so it holds its own against paper.
    PickedFace = Color(0xFF29A091),
    PickedLow = Color(0xFF1C7C70),
    PickedText = Color(0xFF04231F),
    Valid = Color(0xFF2E9E63),
    Invalid = Color(0xFFC0453B),
    Duplicate = Color(0xFF75818C),
    TextPrimary = Color(0xFF171A1D),
    TextMuted = Color(0xFF5D6870),
    Accent = Color(0xFF1F8C7E),
    isDark = false,
)

val LocalSkin = staticCompositionLocalOf { DarkSkin }

/** The active skin. Readable only from a composable, which is every call site. */
val Palette: Skin
    @Composable @ReadOnlyComposable get() = LocalSkin.current

private fun Skin.toColorScheme() = if (isDark) {
    darkColorScheme(
        primary = PickedFace,
        onPrimary = PickedText,
        secondary = Accent,
        background = Background,
        onBackground = TextPrimary,
        surface = Surface,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceHigh,
        onSurfaceVariant = TextMuted,
        outline = TextMuted,
        error = Invalid,
    )
} else {
    lightColorScheme(
        primary = PickedFace,
        onPrimary = PickedText,
        secondary = Accent,
        background = Background,
        onBackground = TextPrimary,
        surface = Surface,
        onSurface = TextPrimary,
        surfaceVariant = SurfaceHigh,
        onSurfaceVariant = TextMuted,
        outline = TextMuted,
        error = Invalid,
    )
}

private val Type = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Tabular figures keep the clock from jittering as the digits change. */
val ClockStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
)

@Composable
fun WordShakeTheme(choice: ThemeChoice = ThemeChoice.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (choice) {
        ThemeChoice.DARK -> true
        ThemeChoice.LIGHT -> false
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
    }
    val skin = if (dark) DarkSkin else LightSkin

    // System bar icons are drawn by the system, so they have to be told which
    // way round the page went or they disappear into it.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalSkin provides skin) {
        MaterialTheme(colorScheme = skin.toColorScheme(), typography = Type, content = content)
    }
}
