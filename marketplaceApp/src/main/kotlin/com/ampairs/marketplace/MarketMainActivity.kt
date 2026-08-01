package com.ampairs.marketplace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.compose.setSingletonImageLoaderFactory
import com.ampairs.storefront.ui.StorefrontRoot
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init

/**
 * Single activity for the common multi-store app. Passes no pinned slug to [StorefrontRoot], which
 * puts it in **directory mode**: after login the user picks a storefront, and that store's isolated
 * graph is activated on selection (brand color taken from the store's own metadata).
 */
class MarketMainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        actionBar?.hide()

        FileKit.init(this)

        val appGraph = (application as MarketApplication).appGraph
        setContent {
            setSingletonImageLoaderFactory { _ -> appGraph.imageLoader }
            // No workspaceSlug → directory mode (storefront picker). Seed color starts neutral and
            // switches to the selected store's brand color.
            StorefrontRoot(graph = appGraph)
        }
    }
}
