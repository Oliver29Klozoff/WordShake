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

/** Ivory dice in a lamplit tray on a deep navy table. */
val DarkSkin = Skin(
    Background = Color(0xFF0D1522),
    Surface = Color(0xFF16233A),
    SurfaceHigh = Color(0xFF1E2F4B),
    Tray = Color(0xFF132034),
    TrayEdge = Color(0xFF0A1120),
    DieHigh = Color(0xFFFFFBF2),
    DieFace = Color(0xFFF1E7D4),
    DieLow = Color(0xFFDCCEB4),
    DieText = Color(0xFF1A2438),
    PickedHigh = Color(0xFFF6C87A),
    PickedFace = Color(0xFFE8A33D),
    PickedLow = Color(0xFFC8811C),
    PickedText = Color(0xFF2A1A05),
    Valid = Color(0xFF57C08A),
    Invalid = Color(0xFFE05A54),
    Duplicate = Color(0xFF7E93B0),
    TextPrimary = Color(0xFFEDF2F8),
    TextMuted = Color(0xFF8CA1BD),
    Accent = Color(0xFF6FB4E8),
    isDark = true,
)

/** The same dice on an oak tray in daylight. */
val LightSkin = Skin(
    Background = Color(0xFFF4EFE6),
    Surface = Color(0xFFE9E1D3),
    SurfaceHigh = Color(0xFFD9CEB9),
    Tray = Color(0xFFB2966D),
    TrayEdge = Color(0xFF8E7550),
    DieHigh = Color(0xFFFFFFFF),
    DieFace = Color(0xFFFBF6EC),
    DieLow = Color(0xFFE3D9C6),
    DieText = Color(0xFF2A2419),
    PickedHigh = Color(0xFFFFD895),
    PickedFace = Color(0xFFE09A2B),
    PickedLow = Color(0xFFB87814),
    PickedText = Color(0xFF33200A),
    Valid = Color(0xFF2A7F55),
    Invalid = Color(0xFFB53A30),
    Duplicate = Color(0xFF7C7263),
    TextPrimary = Color(0xFF241F17),
    TextMuted = Color(0xFF6B6255),
    Accent = Color(0xFF2C6A9E),
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
