package com.ampairs.product.db.entity

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * FTS4 full-text index over [ProductEntity] for fast multi-field product search at scale.
 * Room auto-generates the `product_fts` virtual table AND the content-sync triggers that keep it in
 * step with `productEntity` on insert/update/delete — feature code never writes to this table
 * directly, so the existing offline-sync write path is untouched.
 *
 * Indexed fields: name, code (SKU/product code), description. Every column must name a real column
 * on [ProductEntity] (FTS contentEntity contract). The content table's INTEGER PK `seq_id` is the
 * rowid the FTS index maps to.
 *
 * Tradeoffs (FTS4): prefix matching only (no mid-word substring) and no built-in relevance rank
 * (results are ordered by name). See feature/customer's CustomerFts for the same pattern.
 */
@Fts4(contentEntity = ProductEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "product_fts")
data class ProductFts(
    val name: String,
    val code: String,
    val description: String?,
)
