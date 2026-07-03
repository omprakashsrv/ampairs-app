package com.ampairs.app.ambika

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.compose.setSingletonImageLoaderFactory
import com.ampairs.storefront.ui.StorefrontRoot
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

class AmbikaMainActivity : ComponentActivity() {

    companion object {
        /** The Ambika build is pinned to this storefront/workspace slug. */
        private const val WORKSPACE_SLUG = "ambika-enterprise"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()

        FileKit.init(this)

        val appGraph = (application as AmbikaApplication).appGraph
        setContent {
            setSingletonImageLoaderFactory { _ -> appGraph.imageLoader }
            StorefrontRoot(graph = appGraph, workspaceSlug = WORKSPACE_SLUG)
        }
    }
}
