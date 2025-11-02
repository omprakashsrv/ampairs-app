// Copyright 2023, Christopher Banes and the Tivi project contributors
// SPDX-License-Identifier: Apache-2.0

package app.tivi

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch

fun main() = application {
    Window(
        title = "Ampairs",
        onCloseRequest = ::exitApplication,
    ) {
        val scope = rememberCoroutineScope()
        Button(onClick = {
            scope.launch {

            }
        }) {
            Text("Send Code")
        }
    }
}