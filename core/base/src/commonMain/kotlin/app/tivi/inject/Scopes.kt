// Copyright 2018, Google LLC, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.inject

import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Scope

@Scope
annotation class ApplicationScope

@Scope
annotation class ActivityScope

typealias ApplicationCoroutineScope = CoroutineScope
