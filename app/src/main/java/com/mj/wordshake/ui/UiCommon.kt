package com.mj.wordshake.ui

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** The one filled-button style, shared so the accent stays in one place. */
@Composable
fun primaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Palette.PickedFace,
    contentColor = Palette.PickedText,
    disabledContainerColor = Palette.SurfaceHigh,
    disabledContentColor = Palette.TextMuted,
)

/**
 * The installed version, read from the package rather than BuildConfig so it
 * reports what is actually on the device — which is the number worth showing
 * next to an update button.
 */
@Composable
fun appVersion(): String {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull().orEmpty()
    }
}
