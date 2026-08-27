@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mj.wordshake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.wordshake.game.BoardSize
import com.mj.wordshake.game.Settings
import com.mj.wordshake.game.ThemeChoice
import kotlin.math.roundToInt

/**
 * The settings sheet. Changes apply immediately rather than on a save button,
 * so the swipe slider and the theme can be judged against the board behind
 * them instead of from memory.
 */
@Composable
fun SettingsScreen(
    settings: Settings,
    onChange: (Settings) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Palette.Background)) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                Text(
                    "Settings",
                    color = Palette.TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 18.dp, bottom = 6.dp),
                )
            }

            item { Section("Game") }

            item {
                Choices(
                    label = "Board",
                    options = BoardSize.entries.map { it to it.label },
                    selected = settings.boardSize,
                    onSelect = { onChange(settings.copy(boardSize = it)) },
                    note = "Changing this shakes a new board.",
                )
            }

            item {
                Choices(
                    label = "Round length",
                    options = Settings.ROUND_CHOICES.map { it to "${it / 60} min" },
                    selected = settings.roundSeconds,
                    onSelect = { onChange(settings.copy(roundSeconds = it)) },
                )
            }

            item {
                Choices(
                    label = "Shortest word",
                    options = Settings.LENGTH_CHOICES.map {
                        it to (it?.let { n -> "$n" } ?: "Auto")
                    },
                    selected = settings.minWordLength,
                    onSelect = { onChange(settings.copy(minWordLength = it)) },
                    note = if (settings.minWordLength == null) {
                        "Auto follows the board: 3 letters on 4 x 4, 4 on 5 x 5."
                    } else {
                        "Currently ${settings.effectiveMinLength} letters."
                    },
                )
            }

            item {
                Toggle(
                    label = "Show words remaining",
                    note = "The score bar counts against the board total while you play.",
                    checked = settings.showWordsRemaining,
                    onChange = { onChange(settings.copy(showWordsRemaining = it)) },
                )
            }

            item { Section("Swiping") }

            item {
                SwipeSlider(
                    value = settings.swipeRadius,
                    onChange = { onChange(settings.copy(swipeRadius = it)) },
                )
            }

            item { Section("Feedback") }

            item {
                Toggle(
                    label = "Vibrate",
                    note = "A tick when a word is accepted, a double buzz when it is not.",
                    checked = settings.haptics,
                    onChange = { onChange(settings.copy(haptics = it)) },
                )
            }

            item {
                Toggle(
                    label = "Sound",
                    note = "Short tones on the same events.",
                    checked = settings.sound,
                    onChange = { onChange(settings.copy(sound = it)) },
                )
            }

            item { Section("Appearance") }

            item {
                Choices(
                    label = "Theme",
                    options = listOf(
                        ThemeChoice.SYSTEM to "System",
                        ThemeChoice.LIGHT to "Light",
                        ThemeChoice.DARK to "Dark",
                    ),
                    selected = settings.theme,
                    onSelect = { onChange(settings.copy(theme = it)) },
                )
            }

            item {
                Button(
                    onClick = onClose,
                    colors = primaryButtonColors(),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                ) { Text("Done", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Text(
            title.uppercase(),
            color = Palette.Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        HorizontalDivider(Modifier.padding(top = 6.dp), color = Palette.SurfaceHigh)
    }
}

@Composable
private fun <T> Choices(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    note: String? = null,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(label, color = Palette.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((value, text) in options) {
                val on = value == selected
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) Palette.PickedFace else Palette.Surface)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text,
                        color = if (on) Palette.PickedText else Palette.TextMuted,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                    )
                }
            }
        }
        if (note != null) {
            Spacer(Modifier.height(6.dp))
            Text(note, color = Palette.TextMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Toggle(
    label: String,
    note: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = Palette.TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(note, color = Palette.TextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Palette.PickedText,
                checkedTrackColor = Palette.PickedFace,
                uncheckedThumbColor = Palette.TextMuted,
                uncheckedTrackColor = Palette.Surface,
                uncheckedBorderColor = Palette.SurfaceHigh,
            ),
        )
    }
}

/**
 * The swipe radius, phrased as reach rather than as the fraction of a cell it
 * actually is — the number means nothing to a player, but "how close you have
 * to get" does.
 */
@Composable
private fun SwipeSlider(value: Float, onChange: (Float) -> Unit) {
    val span = Settings.MAX_SWIPE_RADIUS - Settings.MIN_SWIPE_RADIUS
    val percent = ((value - Settings.MIN_SWIPE_RADIUS) / span * 100).roundToInt()

    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Die reach",
                color = Palette.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Text("$percent%", color = Palette.PickedFace, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = Settings.MIN_SWIPE_RADIUS..Settings.MAX_SWIPE_RADIUS,
            colors = SliderDefaults.colors(
                thumbColor = Palette.PickedFace,
                activeTrackColor = Palette.PickedFace,
                inactiveTrackColor = Palette.SurfaceHigh,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Precise", color = Palette.TextMuted, fontSize = 12.sp)
            Text("Forgiving", color = Palette.TextMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "How near the middle of a die your finger has to pass for it to " +
                "join the word. Bigger is easier to hit, but a diagonal swipe " +
                "gets likelier to catch the die it slides past.",
            color = Palette.TextMuted,
            fontSize = 12.sp,
        )
    }
}
