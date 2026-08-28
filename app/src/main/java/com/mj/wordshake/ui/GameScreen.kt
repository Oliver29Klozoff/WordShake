@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package com.mj.wordshake.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mj.wordshake.game.FoundWord
import com.mj.wordshake.game.Settings
import com.mj.wordshake.game.Scoring
import com.mj.wordshake.game.Solver
import com.mj.wordshake.game.Verdict

@Composable
fun GameScreen(state: UiState, actions: GameActions) {
    WordShakeTheme(state.settings.theme) {
        // The surface itself runs full bleed so the chosen background reaches
        // behind the system bars; only the content is inset.
        Surface(color = Palette.Background, modifier = Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().systemBarsPadding()) {
                KeepScreenOn(state.phase == Phase.PLAYING)
                when (state.phase) {
                    Phase.LOADING -> LoadingPane()
                    Phase.RESULTS -> ResultsPane(state, actions)
                    else -> RoundPane(state, actions)
                }
                if (state.settingsOpen) {
                    SettingsScreen(
                        settings = state.settings,
                        onChange = actions::updateSettings,
                        onClose = actions::closeSettings,
                    )
                }
            }
        }
    }
}

/** Everything the screen can ask of the game, so previews need no ViewModel. */
interface GameActions {
    fun start()
    fun pause()
    fun resume()
    fun finish()
    fun shake()
    fun submit()
    fun clearPath()
    fun openSettings()
    fun closeSettings()
    fun updateSettings(settings: Settings)
    fun onTraceStart(cell: Int)
    fun onTraceMove(cell: Int)
    fun onTraceEnd()
    fun onTap(cell: Int)
}

@Composable
private fun LoadingPane() {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(color = Palette.PickedFace)
        Spacer(Modifier.height(20.dp))
        Text("Shaking out the dictionary...", color = Palette.TextMuted)
    }
}

// --- the round --------------------------------------------------------------

/**
 * One layout for both orientations: the board is square either way, so the
 * only question is whether the panel sits beside it or beneath it.
 */
@Composable
private fun RoundPane(state: UiState, actions: GameActions) {
    val board = state.board ?: return

    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp)) {
        val wide = maxWidth > maxHeight

        val boardPane: @Composable (Modifier) -> Unit = { mod ->
            Box(mod, contentAlignment = Alignment.Center) {
                BoardView(
                    board = board,
                    path = state.path,
                    enabled = state.phase == Phase.PLAYING,
                    swipeRadius = state.settings.swipeRadius,
                    onTraceStart = actions::onTraceStart,
                    onTraceMove = actions::onTraceMove,
                    onTraceEnd = actions::onTraceEnd,
                    onTap = actions::onTap,
                    modifier = Modifier.fillMaxSize(),
                )
                // The board stays hidden until the clock is running, so nobody
                // gets a free head start scanning it.
                if (state.phase != Phase.PLAYING) {
                    CoverPane(state, actions)
                }
            }
        }

        if (wide) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                boardPane(Modifier.fillMaxHeight().aspectRatio(1f))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    ScoreBar(state, actions)
                    WordBanner(state)
                    ControlRow(state, actions)
                    Spacer(Modifier.height(8.dp))
                    FoundPane(state, Modifier.weight(1f))
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                ScoreBar(state, actions)
                Spacer(Modifier.height(6.dp))
                boardPane(Modifier.fillMaxWidth().weight(1f))
                WordBanner(state)
                ControlRow(state, actions)
                Spacer(Modifier.height(6.dp))
                FoundPane(state, Modifier.weight(0.42f))
            }
        }
    }
}

/** Pre-round and paused states: covers the dice and offers the way forward. */
@Composable
private fun CoverPane(state: UiState, actions: GameActions) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Palette.Background, RoundedCornerShape(18.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp).verticalScroll(rememberScrollState()),
        ) {
            when (state.phase) {
                Phase.PAUSED -> {
                    Text("Paused", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Palette.TextPrimary)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = actions::resume, colors = primaryButtonColors()) { Text("Resume") }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = actions::finish) {
                        Text("End round", color = Palette.TextMuted)
                    }
                }

                else -> {
                    Text(
                        "WordShake",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Palette.TextPrimary,
                    )
                    Text(
                        "v${appVersion()}",
                        color = Palette.TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Trace touching letters to make words. " +
                            "Each die can be used once per word.",
                        color = Palette.TextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "${state.settings.boardSize.label}  ·  " +
                            "${state.settings.roundSeconds / 60} min  ·  " +
                            "${state.minWordLength}+ letters",
                        color = Palette.Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(18.dp))
                    Button(onClick = actions::start, colors = primaryButtonColors()) {
                        Text("Start round", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = actions::shake) {
                            Text("Shake again", color = Palette.TextMuted)
                        }
                        TextButton(onClick = actions::openSettings) {
                            Text("Settings", color = Palette.TextMuted)
                        }
                    }
                    if (state.best > 0) {
                        Text(
                            "Best on this setting: ${state.best}",
                            color = Palette.TextMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(state: UiState, actions: GameActions) {
    val urgent = state.secondsLeft <= 30 && state.phase == Phase.PLAYING
    val clockColour = if (urgent) Palette.Invalid else Palette.TextPrimary
    val blink by animateFloatAsState(
        targetValue = if (urgent && state.secondsLeft % 2 == 0) 0.55f else 1f,
        animationSpec = tween(400),
        label = "blink",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .background(Palette.Surface, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stat("SCORE", state.score.toString())
        Stat(
            label = "WORDS",
            value = if (state.settings.showWordsRemaining) {
                // An ellipsis rather than a wrong total while the board is
                // still being graded in the background.
                val total = if (state.solving) "..." else state.solution.size.toString()
                "${state.found.size}/$total"
            } else {
                state.found.size.toString()
            },
        )
        Text(
            text = "%d:%02d".format(state.secondsLeft / 60, state.secondsLeft % 60),
            style = ClockStyle,
            color = clockColour,
            modifier = Modifier.alpha(blink),
        )
        IconButton(onClick = actions::openSettings) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Palette.TextMuted,
            )
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(label, color = Palette.TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Palette.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/** The word under construction, or the verdict on the one just submitted. */
@Composable
private fun WordBanner(state: UiState) {
    val flash = state.flash
    val (text, colour) = when {
        flash != null -> when (flash.verdict) {
            Verdict.ACCEPTED -> "${flash.word}   +${flash.points}" to Palette.Valid
            Verdict.REPEAT -> "${flash.word} · already found" to Palette.Duplicate
            Verdict.TOO_SHORT -> "${flash.word} · needs ${state.minWordLength}" to Palette.Duplicate
            Verdict.UNKNOWN -> "${flash.word} · not in the list" to Palette.Invalid
        }

        state.path.isNotEmpty() ->
            state.currentWord to if (state.canSubmit) Palette.PickedFace else Palette.TextMuted

        else -> "" to Palette.TextMuted
    }

    Box(
        Modifier.fillMaxWidth().height(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = colour,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ControlRow(state: UiState, actions: GameActions) {
    val playing = state.phase == Phase.PLAYING
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = actions::clearPath,
            enabled = playing && state.path.isNotEmpty(),
            modifier = Modifier.weight(1f),
        ) { Text("Clear") }

        Button(
            onClick = actions::submit,
            enabled = playing && state.canSubmit,
            colors = primaryButtonColors(),
            modifier = Modifier.weight(1f),
        ) { Text("Submit") }

        OutlinedButton(
            onClick = actions::pause,
            enabled = playing,
            modifier = Modifier.weight(1f),
        ) { Text("Pause") }
    }
}

@Composable
private fun FoundPane(state: UiState, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .background(Palette.Surface, RoundedCornerShape(14.dp))
            .padding(10.dp),
    ) {
        if (state.found.isEmpty()) {
            Text(
                "Words you find land here.",
                color = Palette.TextMuted,
                fontSize = 13.sp,
                modifier = Modifier.align(Alignment.TopStart),
            )
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (found in state.found.asReversed()) {
                        WordChip(found.word, found.points)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordChip(word: String, points: Int?, dim: Boolean = false) {
    Row(
        Modifier
            .padding(vertical = 3.dp)
            .background(
                if (dim) Palette.Background else Palette.SurfaceHigh,
                RoundedCornerShape(8.dp),
            )
            .border(
                1.dp,
                if (dim) Palette.SurfaceHigh else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            word,
            color = if (dim) Palette.TextMuted else Palette.TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        if (points != null) {
            Spacer(Modifier.width(5.dp))
            Text("$points", color = Palette.PickedFace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// --- results ----------------------------------------------------------------

@Composable
private fun ResultsPane(state: UiState, actions: GameActions) {
    val perfect = Solver.perfectScore(state.solution.keys)
    val missed = state.missed.sortedWith(compareByDescending<String> { it.length }.thenBy { it })

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("ROUND OVER", color = Palette.TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    state.score.toString(),
                    color = Palette.PickedFace,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (state.score >= state.best && state.score > 0) {
                    Text("New best", color = Palette.Valid, fontWeight = FontWeight.Bold)
                } else {
                    Text("Best ${state.best}", color = Palette.TextMuted, fontSize = 13.sp)
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Palette.Surface, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Stat("FOUND", "${state.found.size}")
                Stat("ON BOARD", if (state.solving) "..." else "${state.solution.size}")
                Stat("PERFECT", if (state.solving) "..." else "$perfect")
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = actions::shake,
                    colors = primaryButtonColors(),
                    modifier = Modifier.weight(1f),
                ) { Text("New board", fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = actions::openSettings) { Text("Settings") }
            }
        }

        if (state.found.isNotEmpty()) {
            item {
                SectionHeader("Your words", state.found.size)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (f in state.found.sortedWith(compareByDescending<FoundWord> { it.points }.thenBy { it.word })) {
                        WordChip(f.word, f.points)
                    }
                }
            }
        }

        if (state.solving) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = Palette.PickedFace,
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Working out what was there...", color = Palette.TextMuted, fontSize = 13.sp)
                }
            }
        } else if (missed.isNotEmpty()) {
            item { SectionHeader("Missed", missed.size) }
            for ((length, words) in missed.groupBy { it.length }.toSortedMap(reverseOrder())) {
                item {
                    Column {
                        Text(
                            "$length letters · ${Scoring.score(words.first())} each",
                            color = Palette.TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (w in words) WordChip(w, null, dim = true)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Palette.TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("$count", color = Palette.TextMuted, fontSize = 14.sp)
        }
        HorizontalDivider(Modifier.padding(top = 6.dp, bottom = 8.dp), color = Palette.SurfaceHigh)
    }
}
@Composable
private fun KeepScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(active) {
        view.keepScreenOn = active
        onDispose { view.keepScreenOn = false }
    }
}
