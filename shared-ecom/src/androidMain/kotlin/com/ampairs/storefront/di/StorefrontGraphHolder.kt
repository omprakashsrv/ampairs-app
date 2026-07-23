package com.ampairs.storefront.di

/**
 * Holds the app-wide [StorefrontAppGraph] created in the Application. Only the platform entry points
 * (Application / Activity) touch this; screens receive dependencies through Metro-injected
 * ViewModels, never via the holder.
 */
object StorefrontGraphHolder {
    lateinit var graph: StorefrontAppGraph
}
