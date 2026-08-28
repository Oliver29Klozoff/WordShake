package com.mj.wordshake.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mj.wordshake.update.Release
import com.mj.wordshake.update.UpdateChecker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Where the update flow has got to. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: Release) : UpdateState
    data class Downloading(val release: Release, val progress: Float) : UpdateState
    data object NeedsPermission : UpdateState
    data class Failed(val reason: String) : UpdateState
}

/**
 * Kept apart from [GameViewModel] so a download survives rotation and the
 * settings sheet closing, without the game state having to know anything about
 * releases.
 */
class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val checker = UpdateChecker(app)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    val currentVersion: String = checker.currentVersion()

    private var job: Job? = null

    init {
        // Sweep an APK left behind by an install that never completed.
        checker.cleanupStaleApk()
    }

    fun check() {
        if (_state.value is UpdateState.Checking || _state.value is UpdateState.Downloading) return
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = UpdateState.Checking
            val release = checker.latest()
            _state.value = when {
                release == null ->
                    UpdateState.Failed("Could not reach GitHub. Check your connection.")

                UpdateChecker.isNewer(release.tag, currentVersion) ->
                    UpdateState.Available(release)

                else -> UpdateState.UpToDate
            }
        }
    }

    fun download(release: Release) {
        if (!checker.canInstall()) {
            _state.value = UpdateState.NeedsPermission
            return
        }
        job?.cancel()
        job = viewModelScope.launch {
            _state.value = UpdateState.Downloading(release, 0f)
            val file = checker.download(release) { fraction ->
                _state.update { current ->
                    if (current is UpdateState.Downloading) current.copy(progress = fraction)
                    else current
                }
            }
            _state.value = when {
                file == null -> UpdateState.Failed("Download failed. Try again.")
                // The installer takes over from here; leaving the release on
                // screen means a cancelled install can be retried without
                // checking again.
                checker.install(file) -> UpdateState.Available(release)
                else -> UpdateState.Failed("Could not open the installer.")
            }
        }
    }

    fun grantInstallPermission() {
        checker.openInstallPermissionSettings()
        _state.value = UpdateState.Idle
    }

    fun dismiss() {
        job?.cancel()
        _state.value = UpdateState.Idle
    }
}
