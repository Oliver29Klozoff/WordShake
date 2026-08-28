package com.mj.wordshake.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlin.math.roundToInt

/**
 * The version row and the update flow.
 *
 * Its own ViewModel, so a download is not cancelled by closing the sheet or
 * turning the phone.
 */
@Composable
fun UpdatePane(model: UpdateViewModel = viewModel()) {
    val state by model.state.collectAsState()

    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "WordShake ${model.currentVersion}",
                    color = Palette.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(2.dp))
                Text(statusLine(state), color = Palette.TextMuted, fontSize = 12.sp)
            }
            Spacer(Modifier.width(12.dp))

            when (state) {
                is UpdateState.Checking, is UpdateState.Downloading ->
                    CircularProgressIndicator(
                        color = Palette.PickedFace,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp),
                    )

                else -> OutlinedButton(onClick = model::check) { Text("Check") }
            }
        }

        when (val current = state) {
            is UpdateState.Downloading -> {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { current.progress },
                    color = Palette.PickedFace,
                    trackColor = Palette.SurfaceHigh,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is UpdateState.Available -> {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Palette.Surface, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        current.release.title,
                        color = Palette.TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (current.release.notes.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            current.release.notes,
                            color = Palette.TextMuted,
                            fontSize = 12.sp,
                            // Release notes can run long; keep the sheet usable.
                            modifier = Modifier
                                .heightIn(max = 120.dp)
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { model.download(current.release) },
                            colors = primaryButtonColors(),
                        ) { Text("Install", fontWeight = FontWeight.Bold) }
                        TextButton(onClick = model::dismiss) {
                            Text("Not now", color = Palette.TextMuted)
                        }
                    }
                }
            }

            is UpdateState.NeedsPermission -> {
                Spacer(Modifier.height(12.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Palette.Surface, RoundedCornerShape(12.dp))
                        .padding(14.dp),
                ) {
                    Text(
                        "Android needs your permission before WordShake can install " +
                            "its own updates. Turn on \"Allow from this source\", then " +
                            "come back and press Install again.",
                        color = Palette.TextMuted,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = model::grantInstallPermission,
                        colors = primaryButtonColors(),
                    ) { Text("Open settings") }
                }
            }

            else -> Unit
        }
    }
}

private fun statusLine(state: UpdateState): String = when (state) {
    is UpdateState.Idle -> "Tap check to look for a new version."
    is UpdateState.Checking -> "Checking..."
    is UpdateState.UpToDate -> "You are on the latest version."
    is UpdateState.Available -> "${state.release.tag} is available."
    is UpdateState.Downloading -> "Downloading ${(state.progress * 100).roundToInt()}%"
    is UpdateState.NeedsPermission -> "Permission needed to install."
    is UpdateState.Failed -> state.reason
}

