package com.ampairs.common.model

// A model class to re-present UI state
sealed class UiState<out T> {

    object Empty : UiState<Nothing>()
    data class Loading<out T>(val data: T?) : UiState<T & Any>()
    data class Success<out T>(val data: T?) : UiState<T & Any>()
    data class Error(val msg: String?) : UiState<Nothing>()
}