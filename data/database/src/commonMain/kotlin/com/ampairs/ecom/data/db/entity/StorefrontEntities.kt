package com.ampairs.ecom.data.db.entity

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** Cached storefront branding/status. Contract §8 `storefront`. */
@Entity(
    tableName = "storefront",
    indices = [Index(value = ["slug"], unique = true, name = "storefront_slug_idx")]
)
data class StorefrontEntity(
    @PrimaryKey val uid: String,
    val slug: String,
    val name: String,
    val description: String? = null,
    val logo_url: String? = null,
    val banner_url: String? = null,
    val status: String,
    val access_mode: String? = null,
    val cached_at: Long,
)

/**
 * Cached storefront-directory listing for the common (multi-store) app so the picker survives
 * offline. Populated from the unfiltered `GET /store` directory page; offline search filters these
 * rows client-side. `position` preserves the server's ordering. This is an AppScope table (the
 * directory exists before any workspace is activated) and lives only in the storefront apps'
 * `StorefrontAppDatabase` — the main app never shows the directory.
 */
@Entity(tableName = "storefront_directory")
data class StorefrontDirectoryEntity(
    @PrimaryKey val slug: String,
    val uid: String,
    val name: String,
    val description: String? = null,
    val logo_url: String? = null,
    val banner_url: String? = null,
    val status: String,
    val access_mode: String? = null,
    val brand_color_argb: Long? = null,
    val position: Int,
    val cached_at: Long,
)

/** Taxonomy tile images from /catalog-meta. Contract §8 `taxonomy_image`. */
@Entity(
    tableName = "taxonomy_image",
    indices = [Index(value = ["storefront_id", "type", "name"], unique = true, name = "taxonomy_uniq_idx")]
)
data class TaxonomyImageEntity(
    @PrimaryKey val uid: String,
    val storefront_id: String,
    val type: String,            // CATEGORY | SUBCATEGORY | BRAND
    val name: String,
    val parent_name: String? = null,  // subcategory → parent category
    val image_url: String? = null,
    val product_count: Int = 0,
    val sort_order: Int = 0,
)
