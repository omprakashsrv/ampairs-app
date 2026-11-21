package com.ampairs.storefront.di

/**
 * Holds the app-wide [StorefrontGraph] created at the platform entry point (Android Application /
 * iOS MainViewController). Only the platform entry points touch this; screens receive dependencies
 * through Metro-injected ViewModels, never via the holder.
 */
object StorefrontGraphHolder {
    lateinit var graph: StorefrontGraph
}
