package com.mj.wordshake.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** Ivory dice in a lamplit tray on a deep navy table. */
object Palette {
    val Background = Color(0xFF0D1522)
    val Surface = Color(0xFF16233A)
    val SurfaceHigh = Color(0xFF1E2F4B)
    val Tray = Color(0xFF132034)
    val TrayEdge = Color(0xFF0A1120)

    val DieHigh = Color(0xFFFFFBF2)
    val DieFace = Color(0xFFF1E7D4)
    val DieLow = Color(0xFFDCCEB4)
    val DieText = Color(0xFF1A2438)

    val PickedHigh = Color(0xFFF6C87A)
    val PickedFace = Color(0xFFE8A33D)
    val PickedLow = Color(0xFFC8811C)
    val PickedText = Color(0xFF2A1A05)

    val Valid = Color(0xFF57C08A)
    val Invalid = Color(0xFFE05A54)
    val Duplicate = Color(0xFF7E93B0)

    val TextPrimary = Color(0xFFEDF2F8)
    val TextMuted = Color(0xFF8CA1BD)
    val Accent = Color(0xFF6FB4E8)
}

private val Scheme = darkColorScheme(
    primary = Palette.PickedFace,
    onPrimary = Palette.PickedText,
    secondary = Palette.Accent,
    background = Palette.Background,
    onBackground = Palette.TextPrimary,
    surface = Palette.Surface,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.SurfaceHigh,
    onSurfaceVariant = Palette.TextMuted,
    error = Palette.Invalid,
)

/** Tabular figures keep the clock from jittering as the digits change. */
private val Type = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

val ClockStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
)

@Composable
fun WordShakeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = Type, content = content)
}
