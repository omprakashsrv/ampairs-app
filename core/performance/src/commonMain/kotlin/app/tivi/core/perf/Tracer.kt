// Copyright 2021, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.core.perf

fun interface Tracer {
  fun trace(
    name: String,
    block: () -> Unit,
  )
}
