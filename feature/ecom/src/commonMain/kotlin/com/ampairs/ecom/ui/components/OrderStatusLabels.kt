package com.ampairs.ecom.ui.components

import androidx.compose.runtime.Composable
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_status_cancelled
import ampairsapp.feature.ecom.generated.resources.ecom_status_confirmed
import ampairsapp.feature.ecom.generated.resources.ecom_status_delivered
import ampairsapp.feature.ecom.generated.resources.ecom_status_dispatched
import ampairsapp.feature.ecom.generated.resources.ecom_status_pending_review
import ampairsapp.feature.ecom.generated.resources.ecom_status_placed
import ampairsapp.feature.ecom.generated.resources.ecom_status_processing
import com.ampairs.ecom.api.model.OrderStatus
import org.jetbrains.compose.resources.stringResource

/** Customer-facing label for an order status (contract §7.3). */
@Composable
fun orderStatusLabel(status: String): String = when (status) {
    OrderStatus.PENDING_MERCHANT_REVIEW.name -> stringResource(Res.string.ecom_status_pending_review)
    OrderStatus.CONFIRMED.name -> stringResource(Res.string.ecom_status_confirmed)
    OrderStatus.PROCESSING.name -> stringResource(Res.string.ecom_status_processing)
    OrderStatus.DISPATCHED.name -> stringResource(Res.string.ecom_status_dispatched)
    OrderStatus.DELIVERED.name -> stringResource(Res.string.ecom_status_delivered)
    OrderStatus.CANCELLED.name -> stringResource(Res.string.ecom_status_cancelled)
    else -> stringResource(Res.string.ecom_status_placed)
}

/** Ordered tracking timeline stages (terminal CANCELLED handled separately). */
val trackingStages: List<OrderStatus> = listOf(
    OrderStatus.PLACED,
    OrderStatus.CONFIRMED,
    OrderStatus.PROCESSING,
    OrderStatus.DISPATCHED,
    OrderStatus.DELIVERED,
)

/** Index of [status] within [trackingStages]; PENDING_MERCHANT_REVIEW maps to the PLACED stage. */
fun trackingProgress(status: String): Int = when (status) {
    OrderStatus.PENDING_MERCHANT_REVIEW.name, OrderStatus.PLACED.name -> 0
    OrderStatus.CONFIRMED.name -> 1
    OrderStatus.PROCESSING.name -> 2
    OrderStatus.DISPATCHED.name -> 3
    OrderStatus.DELIVERED.name -> 4
    else -> 0
}
