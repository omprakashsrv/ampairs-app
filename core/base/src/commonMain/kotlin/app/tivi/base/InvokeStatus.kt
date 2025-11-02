// Copyright 2019, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.base

sealed class InvokeStatus
object InvokeStarted : InvokeStatus()
object InvokeSuccess : InvokeStatus()
data class InvokeError(val throwable: Throwable) : InvokeStatus()
