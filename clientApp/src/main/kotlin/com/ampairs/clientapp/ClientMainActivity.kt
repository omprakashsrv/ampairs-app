package com.ampairs.clientapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.compose.setSingletonImageLoaderFactory
import com.ampairs.storefront.ui.StorefrontRoot
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

/**
 * Single activity for every white-label client. The pinned storefront/workspace slug is injected at
 * build time from clients/<id>/config.properties → [BuildConfig.WORKSPACE_SLUG].
 */
class ClientMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()

        FileKit.init(this)

        val appGraph = (application as ClientApplication).appGraph
        setContent {
            setSingletonImageLoaderFactory { _ -> appGraph.imageLoader }
            StorefrontRoot(graph = appGraph, workspaceSlug = BuildConfig.WORKSPACE_SLUG)
        }
    }
}
