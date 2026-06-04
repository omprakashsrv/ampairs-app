package com.ampairs.ecom.api.model

/**
 * Stock availability of a listed product. See contract §2.
 */
enum class StockStatus { IN_STOCK, LIMITED, OUT_OF_STOCK, UNKNOWN }

/**
 * Storefront publication lifecycle. See contract §1.
 */
enum class StorefrontStatus { PUBLISHED, DRAFT, UNPUBLISHED, UNKNOWN }

/**
 * Cart session status. See contract §4.5.
 */
enum class CartStatus { ACTIVE, CHECKED_OUT, ABANDONED, MERGED, EXPIRED, UNKNOWN }

/**
 * Order-level status surfaced in the tracking timeline. See contract §7.3.
 */
enum class OrderStatus { PENDING_MERCHANT_REVIEW, CONFIRMED, PROCESSING, DISPATCHED, DELIVERED, CANCELLED, PLACED, UNKNOWN }

/**
 * Line-item status. See contract §7.3.
 */
enum class LineItemStatus { PENDING, CONFIRMED, DISPATCHED, DELIVERED, CANCELLED, PARTIALLY_FULFILLED, UNKNOWN }

/**
 * Per-storefront access model — drives the login-first vs guest-first navigation branch.
 * Sourced from the storefront bootstrap payload when present, else a local config flag.
 */
enum class AccessMode { GUEST_FIRST, LOGIN_FIRST }

/**
 * Store-access request state for the LOGIN_FIRST gate (future backend API, see plan §11.1).
 */
enum class StoreAccessStatus { GRANTED, PENDING, REJECTED, NONE }

/** Taxonomy tile type cached locally. */
enum class TaxonomyType { CATEGORY, SUBCATEGORY, BRAND }
