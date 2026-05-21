package com.ampairs.di

object AppGraphHolder {
    @Volatile
    var graph: AppGraph? = null
}
