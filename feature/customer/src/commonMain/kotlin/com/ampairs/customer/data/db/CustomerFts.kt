package com.ampairs.customer.data.db

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * FTS4 full-text index over [CustomerEntity] for fast multi-field customer search at scale
 * (20k–100k+ rows per workspace). Room auto-generates the `customer_fts` virtual table AND the
 * content-sync triggers that keep it in step with `customers` on insert/update/delete — feature
 * code never writes to this table directly, so the existing offline-sync write path is untouched.
 *
 * Every column here must name a real column on [CustomerEntity] (the FTS contentEntity contract).
 * Phone is indexed as the raw `phone` column: the DAO's `nullifyInvalidPhones` enforces a 10-digit
 * value, so it is already a single space-free token. The query side strips spaces so a search for
 * "98765 43210" still matches the stored "9876543210". GSTIN is a single token → prefix-matchable.
 *
 * Tradeoffs (FTS4, see spec 001): prefix matching only (no mid-word substring) and no built-in
 * relevance rank (results are ordered by name). A future move to Room `@Fts5` (androidx.room3)
 * can add bm25 ranking / trigram substring without touching the repository/UI.
 */
@Fts4(contentEntity = CustomerEntity::class, tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "customer_fts")
data class CustomerFts(
    val name: String,
    val email: String?,
    val phone: String?,
    val gstNumber: String?,
    val address: String?,
    val street: String?,
    val city: String?,
    val state: String?,
)
