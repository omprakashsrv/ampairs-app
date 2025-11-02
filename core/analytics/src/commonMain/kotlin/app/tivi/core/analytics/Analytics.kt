// Copyright 2021, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.core.analytics

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName(swiftName = "TiviAnalytics")
interface Analytics {
  fun trackScreenView(
    name: String,
    arguments: Map<String, *>? = null,
  )

  fun setEnabled(enabled: Boolean)
}
