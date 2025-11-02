package com.ampairs.common.navigation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*

/**
 * Desktop-specific implementation using keyboard input
 * Handles Escape key and Alt+Left arrow for back navigation
 */
@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBackPressed: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(enabled) {
        if (enabled) {
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {
                // Ignore focus request failures
            }
        }
    }

    if (enabled) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    when {
                        // Handle Escape key
                        event.type == KeyEventType.KeyDown && event.key == Key.Escape -> {
                            onBackPressed()
                            true
                        }
                        // Handle Alt+Left arrow (browser-style back navigation)
                        event.type == KeyEventType.KeyDown &&
                                event.key == Key.DirectionLeft &&
                                event.isAltPressed -> {
                            onBackPressed()
                            true
                        }
                        // Handle Backspace (common desktop pattern)
                        event.type == KeyEventType.KeyDown &&
                                event.key == Key.Backspace &&
                                !event.isCtrlPressed -> {
                            // Handle backspace for back navigation
                            // Note: This will work when the Box itself has focus
                            onBackPressed()
                            true
                        }

                        else -> false
                    }
                }
        )
    }
}