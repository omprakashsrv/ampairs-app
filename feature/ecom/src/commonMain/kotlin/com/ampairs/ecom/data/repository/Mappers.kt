package com.ampairs.ecom.data.repository

import com.ampairs.ecom.api.model.AddressResponse
import com.ampairs.ecom.api.model.CartItemResponse
import com.ampairs.ecom.api.model.CartResponse
import com.ampairs.ecom.api.model.EcomOrderLineItem
import com.ampairs.ecom.api.model.EcomOrderResponse
import com.ampairs.ecom.api.model.ListedProduct
import com.ampairs.ecom.api.model.ProductSyncItem
import com.ampairs.ecom.api.model.Storefront
import com.ampairs.ecom.data.db.entity.CartEntity
import com.ampairs.ecom.data.db.entity.CartItemEntity
import com.ampairs.ecom.data.db.entity.CustomerAddressEntity
import com.ampairs.ecom.data.db.entity.EcomOrderEntity
import com.ampairs.ecom.data.db.entity.EcomOrderLineItemEntity
import com.ampairs.ecom.data.db.entity.ListedProductEntity
import com.ampairs.ecom.data.db.entity.StorefrontEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal val ecomJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

internal fun encodeImageUrls(urls: List<String>): String =
    ecomJson.encodeToString(ListSerializer(String.serializer()), urls)

internal fun decodeImageUrls(json: String): List<String> =
    runCatching { ecomJson.decodeFromString(ListSerializer(String.serializer()), json) }.getOrDefault(emptyList())

// ── Storefront ──

fun Storefront.toEntity(cachedAt: Long): StorefrontEntity = StorefrontEntity(
    uid = uid,
    slug = slug,
    name = name,
    description = description,
    logo_url = logoUrl,
    banner_url = bannerUrl,
    status = status,
    access_mode = accessMode,
    cached_at = cachedAt,
)

// ── Listed product ──

fun ListedProduct.toEntity(storefrontId: String): ListedProductEntity = ListedProductEntity(
    uid = uid,
    storefront_id = storefrontId,
    management_product_id = managementProductId,
    name = name,
    brand = brand,
    category = category,
    subcategory = subcategory,
    unit = unit,
    price = price,
    mrp = mrp,
    stock_status = stockStatus,
    stock_quantity = stockQuantity,
    image_urls = encodeImageUrls(imageUrls),
    description = description,
    is_visible = 1,
    updated_at = null,
)

fun ProductSyncItem.toEntity(storefrontId: String): ListedProductEntity = ListedProductEntity(
    uid = uid,
    storefront_id = storefrontId,
    management_product_id = managementProductId,
    name = name,
    brand = brand,
    category = category,
    subcategory = subcategory,
    unit = unit,
    price = price,
    mrp = mrp,
    stock_status = stockStatus,
    stock_quantity = stockQuantity,
    image_urls = encodeImageUrls(imageUrls),
    description = description,
    is_visible = if (isVisible) 1 else 0,
    updated_at = updatedAt,
)

fun ListedProductEntity.toModel(): ListedProduct = ListedProduct(
    uid = uid,
    name = name,
    brand = brand,
    category = category,
    subcategory = subcategory,
    unit = unit,
    price = price,
    mrp = mrp,
    stockStatus = stock_status,
    stockQuantity = stock_quantity,
    imageUrls = decodeImageUrls(image_urls),
    description = description,
    managementProductId = management_product_id,
)

// ── Cart ──

fun CartResponse.toCartEntity(storefrontId: String): CartEntity = CartEntity(
    uid = uid,
    storefront_id = storefrontId,
    session_token = sessionToken,
    status = status,
    expires_at = expiresAt,
)

fun CartItemResponse.toEntity(cartId: String): CartItemEntity = CartItemEntity(
    uid = uid,
    cart_id = cartId,
    listed_product_id = listedProductId,
    management_product_id = managementProductId,
    product_name = productName,
    brand = brand,
    unit = unit,
    unit_price = unitPrice,
    mrp_at_add = mrpAtAdd,
    quantity = quantity,
    primary_image_url = primaryImageUrl,
)

// ── Address ──

fun AddressResponse.toEntity(synced: Int = 1): CustomerAddressEntity = CustomerAddressEntity(
    uid = uid,
    label = label,
    address_line1 = addressLine1,
    address_line2 = addressLine2,
    city = city,
    state = state,
    pin_code = pinCode,
    country = country,
    phone = phone,
    is_default = if (isDefault) 1 else 0,
    active = 1,
    synced = synced,
)

fun CustomerAddressEntity.toModel(): AddressResponse = AddressResponse(
    uid = uid,
    label = label,
    addressLine1 = address_line1,
    addressLine2 = address_line2,
    city = city,
    state = state,
    pinCode = pin_code,
    country = country,
    phone = phone,
    isDefault = is_default == 1,
)

// ── Order ──

fun EcomOrderResponse.toEntity(): EcomOrderEntity = EcomOrderEntity(
    uid = uid,
    ecom_order_ref = ecomOrderRef,
    storefront_id = storefrontId,
    status = status,
    subtotal = subtotal,
    total_amount = totalAmount,
    notes = notes,
    placed_at = placedAt,
    confirmed_at = confirmedAt,
    delivery_address = deliveryAddress?.let { ecomJson.encodeToString(com.ampairs.ecom.api.model.DeliveryAddress.serializer(), it) },
)

fun EcomOrderLineItem.toEntity(orderUid: String): EcomOrderLineItemEntity = EcomOrderLineItemEntity(
    uid = uid,
    order_uid = orderUid,
    listed_product_id = listedProductId,
    product_name = productName,
    unit_price = unitPrice,
    quantity_ordered = quantityOrdered,
    quantity_confirmed = quantityConfirmed,
    line_total = lineTotal,
    status = status,
)
