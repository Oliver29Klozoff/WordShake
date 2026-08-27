package com.mj.wordshake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import com.mj.wordshake.game.Settings
import com.mj.wordshake.ui.GameActions
import com.mj.wordshake.ui.GameScreen
import com.mj.wordshake.ui.GameViewModel
import com.mj.wordshake.ui.Phase

class MainActivity : ComponentActivity() {

    private val model: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by model.state.collectAsState()
            val actions = remember { bind(model) }

            // Back closes settings if they are open; during a round it stops
            // the clock rather than dropping the player out mid-word.
            BackHandler(enabled = state.settingsOpen || state.phase == Phase.PLAYING) {
                if (state.settingsOpen) model.closeSettings() else model.pause()
            }

            GameScreen(state, actions)
        }
    }

    /** Leaving the app pauses the round rather than letting the clock run down. */
    override fun onPause() {
        super.onPause()
        model.pause()
    }
}

private fun bind(model: GameViewModel) = object : GameActions {
    override fun start() = model.start()
    override fun pause() = model.pause()
    override fun resume() = model.resume()
    override fun finish() = model.finish()
    override fun shake() = model.shake()
    override fun submit() = model.submit()
    override fun clearPath() = model.clearPath()
    override fun openSettings() = model.openSettings()
    override fun closeSettings() = model.closeSettings()
    override fun updateSettings(settings: Settings) = model.updateSettings(settings)
    override fun onTraceStart(cell: Int) = model.onTraceStart(cell)
    override fun onTraceMove(cell: Int) = model.onTraceMove(cell)
    override fun onTraceEnd() = model.onTraceEnd()
    override fun onTap(cell: Int) = model.onTap(cell)
}
