// Copyright 2024, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.core.notifications

import kotlin.time.ExperimentalTime
import kotlin.time.Instant


@OptIn(ExperimentalTime::class)
data class Notification(
    val id: String,
    val title: String,
    val message: String,
    val channel: NotificationChannel,
    val date: Instant,
    val deeplinkUrl: String? = null,
)

enum class NotificationChannel(val id: String) {
    DEVELOPER("dev"),
    EPISODES_AIRING("episodes_airing"),
    ;

    companion object {
        fun fromId(id: String): NotificationChannel {
            return NotificationChannel.entries.first { it.id == id }
        }
    }
}
