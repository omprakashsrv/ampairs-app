package com.ampairs.common

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object UnauthenticatedHandler {
    private val _onUnauthenticated = MutableSharedFlow<Unit>(replay = 1)
    val onUnauthenticated = _onUnauthenticated.asSharedFlow()

    fun onUnauthenticated() {
        _onUnauthenticated.tryEmit(Unit)
    }
}
