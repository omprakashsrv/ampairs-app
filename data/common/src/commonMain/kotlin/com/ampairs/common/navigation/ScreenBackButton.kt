package com.ampairs.common.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * Whether a screen should render its OWN back button in its top bar.
 *
 * The shared global app layout renders a single back button in its header on layouts that
 * actually show that header (desktop: navigation rail + top header, and the workspace-selection
 * flow). On those layouts a screen must NOT draw a second back button, or two appear stacked.
 *
 * On mobile the global header is not shown for in-app screens, so each screen owns its back
 * affordance and this is `true`.
 *
 * Provided once by `GlobalAppLayoutNav3`; defaults to `true` so a screen shown outside that
 * layout (previews, auth flow) still gets a back button.
 */
val LocalShowScreenBackButton = compositionLocalOf { true }

/**
 * Back button for a screen-level top bar that hides itself when the shared global header
 * already provides one (see [LocalShowScreenBackButton]). Use this in place of a raw
 * `IconButton { Icon(ArrowBack) }` inside a screen's `navigationIcon` / custom top bar.
 */
@Composable
fun ScreenBackButton(
    onClick: () -> Unit,
    contentDescription: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    if (LocalShowScreenBackButton.current) {
        IconButton(onClick = onClick, modifier = modifier) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = contentDescription,
                tint = tint,
            )
        }
    }
}
