package com.ampairs.ecom.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
