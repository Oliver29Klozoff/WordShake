package com.mj.wordshake.ui

import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable

/** The one filled-button style, shared so the accent stays in one place. */
@Composable
fun primaryButtonColors() = ButtonDefaults.buttonColors(
    containerColor = Palette.PickedFace,
    contentColor = Palette.PickedText,
    disabledContainerColor = Palette.SurfaceHigh,
    disabledContentColor = Palette.TextMuted,
)
