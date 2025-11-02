// Copyright 2021, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.common.compose

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState

inline fun CombinedLoadStates.appendErrorOrNull(): UiMessage? {
  return (append as? LoadState.Error)?.let { UiMessage(it.error) }
}

inline fun CombinedLoadStates.prependErrorOrNull(): UiMessage? {
  return (prepend as? LoadState.Error)?.let { UiMessage(it.error) }
}

inline fun CombinedLoadStates.refreshErrorOrNull(): UiMessage? {
  return (refresh as? LoadState.Error)?.let { UiMessage(it.error) }
}
