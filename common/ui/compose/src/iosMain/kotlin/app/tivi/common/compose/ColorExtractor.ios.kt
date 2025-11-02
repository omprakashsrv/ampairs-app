// Copyright 2024, Christopher Banes
// SPDX-License-Identifier: Apache-2.0

package app.tivi.common.compose

import coil3.request.ImageRequest

internal actual fun ImageRequest.Builder.prepareForColorExtractor(): ImageRequest.Builder = this
