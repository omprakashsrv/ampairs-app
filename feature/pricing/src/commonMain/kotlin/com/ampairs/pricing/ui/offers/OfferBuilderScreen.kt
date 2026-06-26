package com.ampairs.pricing.ui.offers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import com.ampairs.common.locale.LocalAppLocale
import com.ampairs.common.locale.formatMoney
import com.ampairs.pricing.domain.model.OfferConditionType
import com.ampairs.pricing.domain.model.OfferRewardType
import com.ampairs.pricing.domain.model.OfferStatus
import com.ampairs.pricing.domain.model.SalesChannel
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import org.jetbrains.compose.resources.stringResource
import ampairsapp.feature.pricing.generated.resources.Res
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_back
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_basics_brand_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_basics_category_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_basics_name_hint
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_basics_name_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_channel_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_channel_retail
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_channel_wholesale
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_continue
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_cart_min
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_cart_min_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_cart_min_sub
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_coupon
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_coupon_code_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_coupon_limit_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_coupon_sub
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_first_order
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_first_order_sub
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_none
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_none_sub
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_quantity_min
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_quantity_min_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_cond_quantity_min_sub
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_error_name_required
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_geo_zone_all
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_geo_zone_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_limits_exclusive_sub
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_limits_exclusive_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_limits_per_customer_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_limits_stackable_sub
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_limits_stackable_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_limits_total_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_optional_id_hint
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_preview_sentence
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_priority_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_review_audience
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_review_cart_subtotal
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_review_channel
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_review_condition
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_review_customer_pays
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_review_discount
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_review_name
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_review_reward
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_review_sample_cart
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_review_sync_note
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_bogo
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_bogo_buy_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_bogo_summary
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_bogo_get_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_cap_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_flat
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_flat_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_free_gift
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_free_shipping
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_hint
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_percent
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_percent_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_reward_tiered
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_schedule
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_step0_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_step1_hint
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_step1_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_step2_hint
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_step2_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_step3_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_step4_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_step5_title
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_step_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_status_draft
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_status_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_status_scheduled
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_target_group_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_target_type_label
import ampairsapp.feature.pricing.generated.resources.pricing_offerb_title

@Composable
fun OfferBuilderScreen(
    offerId: String?,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OfferBuilderViewModel = assistedMetroViewModel<OfferBuilderViewModel, OfferBuilderViewModel.Factory>(
        key = offerId ?: "new",
    ) { create(offerId) },
) {
    val state by viewModel.uiState.collectAsState()
    val isLast = state.step == OFFER_STEP_COUNT - 1

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(Res.string.pricing_offerb_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                StepIndicator(step = state.step)
            }
        }
        HorizontalDivider()

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(
                    text = stringResource(Res.string.pricing_offerb_step_label, state.step + 1, OFFER_STEP_COUNT),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item { Text(stepTitle(state.step), style = MaterialTheme.typography.headlineSmall) }
            item { PreviewCard(state) }
            item {
                when (state.step) {
                    0 -> StepBasics(state, viewModel)
                    1 -> StepTargets(state, viewModel)
                    2 -> StepTrigger(state, viewModel)
                    3 -> StepReward(state, viewModel)
                    4 -> StepLimits(state, viewModel)
                    else -> StepReview(state)
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }

        HorizontalDivider()
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(onClick = viewModel::back, enabled = state.step > 0) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(Res.string.pricing_offerb_back))
            }
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f).clickable(enabled = !state.isSaving) {
                    if (isLast) viewModel.save(onSaved) else viewModel.next()
                },
            ) {
                Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isLast) stringResource(Res.string.pricing_offerb_schedule) else stringResource(Res.string.pricing_offerb_continue),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun stepTitle(step: Int): String = when (step) {
    0 -> stringResource(Res.string.pricing_offerb_step0_title)
    1 -> stringResource(Res.string.pricing_offerb_step1_title)
    2 -> stringResource(Res.string.pricing_offerb_step2_title)
    3 -> stringResource(Res.string.pricing_offerb_step3_title)
    4 -> stringResource(Res.string.pricing_offerb_step4_title)
    else -> stringResource(Res.string.pricing_offerb_step5_title)
}

@Composable
private fun StepIndicator(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(OFFER_STEP_COUNT) { index ->
            val color = if (index <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
            Surface(color = color, shape = CircleShape, modifier = Modifier.size(if (index == step) 10.dp else 8.dp)) {}
        }
    }
}

// ── live preview sentence: "<condition> on <channel> → get <reward>" ────────────────

@Composable
private fun PreviewCard(state: OfferBuilderUiState) {
    val locale = LocalAppLocale.current
    val condition = conditionLabel(state.conditionType)
    val channel = channelLabel(state.channel)
    val reward = rewardSummary(state, formatMoney(state.rewardFlat.toMajorOrNull(), locale), formatMoney(state.cartMin.toMajorOrNull(), locale))

    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(15.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(Res.string.pricing_offerb_preview_sentence, condition, channel, reward),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun channelLabel(channel: SalesChannel): String = when (channel) {
    SalesChannel.WHOLESALE -> stringResource(Res.string.pricing_offerb_channel_wholesale)
    SalesChannel.RETAIL -> stringResource(Res.string.pricing_offerb_channel_retail)
}

@Composable
private fun conditionLabel(type: OfferConditionType): String = when (type) {
    OfferConditionType.NONE -> stringResource(Res.string.pricing_offerb_cond_none)
    OfferConditionType.CART_MIN -> stringResource(Res.string.pricing_offerb_cond_cart_min)
    OfferConditionType.QUANTITY_MIN -> stringResource(Res.string.pricing_offerb_cond_quantity_min)
    OfferConditionType.FIRST_ORDER -> stringResource(Res.string.pricing_offerb_cond_first_order)
    OfferConditionType.COUPON -> stringResource(Res.string.pricing_offerb_cond_coupon)
}

@Composable
private fun rewardLabel(type: OfferRewardType): String = when (type) {
    OfferRewardType.PERCENT -> stringResource(Res.string.pricing_offerb_reward_percent)
    OfferRewardType.FLAT -> stringResource(Res.string.pricing_offerb_reward_flat)
    OfferRewardType.BOGO -> stringResource(Res.string.pricing_offerb_reward_bogo)
    OfferRewardType.FREE_SHIPPING -> stringResource(Res.string.pricing_offerb_reward_free_shipping)
    OfferRewardType.FREE_GIFT -> stringResource(Res.string.pricing_offerb_reward_free_gift)
    OfferRewardType.TIERED -> stringResource(Res.string.pricing_offerb_reward_tiered)
}

/** Plain-language reward summary for the live sentence + review card. */
@Composable
private fun rewardSummary(state: OfferBuilderUiState, flatMoney: String, cartMoney: String): String = when (state.rewardType) {
    OfferRewardType.PERCENT -> state.rewardPercent.ifBlank { "0" } + "% " + stringResource(Res.string.pricing_offerb_reward_percent).lowercase()
    OfferRewardType.FLAT -> flatMoney.ifBlank { rewardLabel(OfferRewardType.FLAT) }
    OfferRewardType.BOGO -> stringResource(
        Res.string.pricing_offerb_reward_bogo_summary,
        state.bogoBuyQty.ifBlank { "0" },
        state.bogoGetQty.ifBlank { "0" },
    )
    else -> rewardLabel(state.rewardType)
}

// ── STEP 0 basics ───────────────────────────────────────────────────────────────────

@Composable
private fun StepBasics(state: OfferBuilderUiState, viewModel: OfferBuilderViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::setName,
            label = { Text(stringResource(Res.string.pricing_offerb_basics_name_label)) },
            placeholder = { Text(stringResource(Res.string.pricing_offerb_basics_name_hint)) },
            isError = state.error == "name_required",
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.error == "name_required") {
            Text(
                stringResource(Res.string.pricing_offerb_error_name_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Text(stringResource(Res.string.pricing_offerb_channel_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceCard(
                icon = Icons.Filled.Storefront,
                title = stringResource(Res.string.pricing_offerb_channel_wholesale),
                selected = state.channel == SalesChannel.WHOLESALE,
                onClick = { viewModel.setChannel(SalesChannel.WHOLESALE) },
                modifier = Modifier.weight(1f),
            )
            ChoiceCard(
                icon = Icons.Filled.ShoppingBag,
                title = stringResource(Res.string.pricing_offerb_channel_retail),
                selected = state.channel == SalesChannel.RETAIL,
                onClick = { viewModel.setChannel(SalesChannel.RETAIL) },
                modifier = Modifier.weight(1f),
            )
        }

        Text(stringResource(Res.string.pricing_offerb_status_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ChoiceCard(
                icon = Icons.Filled.AutoAwesome,
                title = stringResource(Res.string.pricing_offerb_status_scheduled),
                selected = state.status == OfferStatus.SCHEDULED,
                onClick = { viewModel.setStatus(OfferStatus.SCHEDULED) },
                modifier = Modifier.weight(1f),
            )
            ChoiceCard(
                icon = Icons.Filled.Category,
                title = stringResource(Res.string.pricing_offerb_status_draft),
                selected = state.status == OfferStatus.DRAFT,
                onClick = { viewModel.setStatus(OfferStatus.DRAFT) },
                modifier = Modifier.weight(1f),
            )
        }

        OutlinedTextField(
            value = state.priority,
            onValueChange = viewModel::setPriority,
            label = { Text(stringResource(Res.string.pricing_offerb_priority_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ChoiceCard(icon: ImageVector, title: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null, tint = content)
            Text(title, style = MaterialTheme.typography.titleSmall, color = content)
        }
    }
}

// ── STEP 1 targets ────────────────────────────────────────────────────────────────────

@Composable
private fun StepTargets(state: OfferBuilderUiState, viewModel: OfferBuilderViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(stringResource(Res.string.pricing_offerb_step1_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        IconTextField(Icons.Filled.Groups, stringResource(Res.string.pricing_offerb_target_group_label), state.customerGroupId, viewModel::setCustomerGroupId)
        IconTextField(Icons.Filled.Category, stringResource(Res.string.pricing_offerb_target_type_label), state.customerType, viewModel::setCustomerType)
        IconTextField(Icons.Filled.BrandingWatermark, stringResource(Res.string.pricing_offerb_basics_brand_label), state.brandId, viewModel::setBrandId)
        IconTextField(Icons.Filled.Category, stringResource(Res.string.pricing_offerb_basics_category_label), state.categoryId, viewModel::setCategoryId)

        Text(stringResource(Res.string.pricing_offerb_geo_zone_label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        ZoneRow(stringResource(Res.string.pricing_offerb_geo_zone_all), state.geoZoneId.isBlank()) { viewModel.setGeoZoneId("") }
        state.geoZones.forEach { zone ->
            ZoneRow(zone.name, state.geoZoneId == zone.uid) { viewModel.setGeoZoneId(zone.uid) }
        }
    }
}

@Composable
private fun IconTextField(icon: ImageVector, label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(stringResource(Res.string.pricing_offerb_optional_id_hint)) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ZoneRow(name: String, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            Icon(Icons.Filled.Map, contentDescription = null, tint = content)
            Text(name, style = MaterialTheme.typography.bodyLarge, color = content, modifier = Modifier.weight(1f))
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            }
        }
    }
}

// ── STEP 2 trigger ────────────────────────────────────────────────────────────────────

@Composable
private fun StepTrigger(state: OfferBuilderUiState, viewModel: OfferBuilderViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(stringResource(Res.string.pricing_offerb_step2_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        ConditionOption(Icons.Filled.AutoAwesome, stringResource(Res.string.pricing_offerb_cond_none), stringResource(Res.string.pricing_offerb_cond_none_sub), state.conditionType == OfferConditionType.NONE) { viewModel.setConditionType(OfferConditionType.NONE) }
        ConditionOption(Icons.Filled.ShoppingCart, stringResource(Res.string.pricing_offerb_cond_cart_min), stringResource(Res.string.pricing_offerb_cond_cart_min_sub), state.conditionType == OfferConditionType.CART_MIN) { viewModel.setConditionType(OfferConditionType.CART_MIN) }
        ConditionOption(Icons.Filled.Numbers, stringResource(Res.string.pricing_offerb_cond_quantity_min), stringResource(Res.string.pricing_offerb_cond_quantity_min_sub), state.conditionType == OfferConditionType.QUANTITY_MIN) { viewModel.setConditionType(OfferConditionType.QUANTITY_MIN) }
        ConditionOption(Icons.Filled.PersonAdd, stringResource(Res.string.pricing_offerb_cond_first_order), stringResource(Res.string.pricing_offerb_cond_first_order_sub), state.conditionType == OfferConditionType.FIRST_ORDER) { viewModel.setConditionType(OfferConditionType.FIRST_ORDER) }
        ConditionOption(Icons.Filled.ConfirmationNumber, stringResource(Res.string.pricing_offerb_cond_coupon), stringResource(Res.string.pricing_offerb_cond_coupon_sub), state.conditionType == OfferConditionType.COUPON) { viewModel.setConditionType(OfferConditionType.COUPON) }

        when (state.conditionType) {
            OfferConditionType.CART_MIN -> OutlinedTextField(
                value = state.cartMin,
                onValueChange = viewModel::setCartMin,
                label = { Text(stringResource(Res.string.pricing_offerb_cond_cart_min_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OfferConditionType.QUANTITY_MIN -> OutlinedTextField(
                value = state.quantityMin,
                onValueChange = viewModel::setQuantityMin,
                label = { Text(stringResource(Res.string.pricing_offerb_cond_quantity_min_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OfferConditionType.COUPON -> Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = state.couponCode,
                    onValueChange = viewModel::setCouponCode,
                    label = { Text(stringResource(Res.string.pricing_offerb_cond_coupon_code_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.couponLimit,
                    onValueChange = viewModel::setCouponLimit,
                    label = { Text(stringResource(Res.string.pricing_offerb_cond_coupon_limit_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun ConditionOption(icon: ImageVector, label: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(13.dp)) {
            Icon(icon, contentDescription = null, tint = content)
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleSmall, color = content)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = content)
            }
            if (selected) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            }
        }
    }
}

// ── STEP 3 reward ─────────────────────────────────────────────────────────────────────

@Composable
private fun StepReward(state: OfferBuilderUiState, viewModel: OfferBuilderViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(stringResource(Res.string.pricing_offerb_reward_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Grid of reward types — two rows of three.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RewardCard(Icons.Filled.Percent, rewardLabel(OfferRewardType.PERCENT), state.rewardType == OfferRewardType.PERCENT, Modifier.weight(1f)) { viewModel.setRewardType(OfferRewardType.PERCENT) }
            RewardCard(Icons.Filled.Savings, rewardLabel(OfferRewardType.FLAT), state.rewardType == OfferRewardType.FLAT, Modifier.weight(1f)) { viewModel.setRewardType(OfferRewardType.FLAT) }
            RewardCard(Icons.Filled.ShoppingBag, rewardLabel(OfferRewardType.BOGO), state.rewardType == OfferRewardType.BOGO, Modifier.weight(1f)) { viewModel.setRewardType(OfferRewardType.BOGO) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RewardCard(Icons.Filled.LocalShipping, rewardLabel(OfferRewardType.FREE_SHIPPING), state.rewardType == OfferRewardType.FREE_SHIPPING, Modifier.weight(1f)) { viewModel.setRewardType(OfferRewardType.FREE_SHIPPING) }
            RewardCard(Icons.Filled.CardGiftcard, rewardLabel(OfferRewardType.FREE_GIFT), state.rewardType == OfferRewardType.FREE_GIFT, Modifier.weight(1f)) { viewModel.setRewardType(OfferRewardType.FREE_GIFT) }
            RewardCard(Icons.Filled.Stairs, rewardLabel(OfferRewardType.TIERED), state.rewardType == OfferRewardType.TIERED, Modifier.weight(1f)) { viewModel.setRewardType(OfferRewardType.TIERED) }
        }

        when (state.rewardType) {
            OfferRewardType.PERCENT -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.rewardPercent,
                    onValueChange = viewModel::setRewardPercent,
                    label = { Text(stringResource(Res.string.pricing_offerb_reward_percent_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.rewardCap,
                    onValueChange = viewModel::setRewardCap,
                    label = { Text(stringResource(Res.string.pricing_offerb_reward_cap_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OfferRewardType.FLAT -> OutlinedTextField(
                value = state.rewardFlat,
                onValueChange = viewModel::setRewardFlat,
                label = { Text(stringResource(Res.string.pricing_offerb_reward_flat_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OfferRewardType.BOGO -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = state.bogoBuyQty,
                    onValueChange = viewModel::setBogoBuyQty,
                    label = { Text(stringResource(Res.string.pricing_offerb_reward_bogo_buy_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = state.bogoGetQty,
                    onValueChange = viewModel::setBogoGetQty,
                    label = { Text(stringResource(Res.string.pricing_offerb_reward_bogo_get_label)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun RewardCard(icon: ImageVector, label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val container = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = container, shape = MaterialTheme.shapes.medium, modifier = modifier.clickable(onClick = onClick)) {
        Column(Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

// ── STEP 4 limits ─────────────────────────────────────────────────────────────────────

@Composable
private fun StepLimits(state: OfferBuilderUiState, viewModel: OfferBuilderViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ToggleRow(
            icon = Icons.Filled.Layers,
            title = stringResource(Res.string.pricing_offerb_limits_stackable_title),
            subtitle = stringResource(Res.string.pricing_offerb_limits_stackable_sub),
            checked = state.stackable,
            onToggle = viewModel::toggleStackable,
        )
        ToggleRow(
            icon = Icons.Filled.Block,
            title = stringResource(Res.string.pricing_offerb_limits_exclusive_title),
            subtitle = stringResource(Res.string.pricing_offerb_limits_exclusive_sub),
            checked = state.exclusive,
            onToggle = viewModel::toggleExclusive,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = state.perCustomerLimit,
                onValueChange = viewModel::setPerCustomerLimit,
                label = { Text(stringResource(Res.string.pricing_offerb_limits_per_customer_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.totalLimit,
                onValueChange = viewModel::setTotalLimit,
                label = { Text(stringResource(Res.string.pricing_offerb_limits_total_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onToggle: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}

// ── STEP 5 review + sample-cart preview ────────────────────────────────────────────────

@Composable
private fun StepReview(state: OfferBuilderUiState) {
    val locale = LocalAppLocale.current
    val condition = conditionLabel(state.conditionType)
    val channel = channelLabel(state.channel)
    val reward = rewardSummary(state, formatMoney(state.rewardFlat.toMajorOrNull(), locale), formatMoney(state.cartMin.toMajorOrNull(), locale))
    val audience = state.customerGroupId.ifBlank { state.customerType }.ifBlank { "—" }

    // Sample-cart preview: a fixed demo subtotal so the user sees the reward applied.
    val subtotal = 1000.0
    val discount = when (state.rewardType) {
        OfferRewardType.PERCENT -> {
            val pct = state.rewardPercent.toDoubleOrNull() ?: 0.0
            val raw = subtotal * pct / 100.0
            val cap = state.rewardCap.toMajorOrNull()
            if (cap != null && raw > cap) cap else raw
        }
        OfferRewardType.FLAT -> state.rewardFlat.toMajorOrNull() ?: 0.0
        else -> 0.0
    }
    val finalPay = (subtotal - discount).coerceAtLeast(0.0)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column {
                ReviewRow(stringResource(Res.string.pricing_offerb_review_name), state.name, divider = true)
                ReviewRow(stringResource(Res.string.pricing_offerb_review_channel), channel, divider = true)
                ReviewRow(stringResource(Res.string.pricing_offerb_review_audience), audience, divider = true)
                ReviewRow(stringResource(Res.string.pricing_offerb_review_condition), condition, divider = true)
                ReviewRow(stringResource(Res.string.pricing_offerb_review_reward), reward, divider = false)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(Res.string.pricing_offerb_review_sample_cart), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                SampleRow(stringResource(Res.string.pricing_offerb_review_cart_subtotal), formatMoney(subtotal, locale), MaterialTheme.colorScheme.onSurface)
                SampleRow(stringResource(Res.string.pricing_offerb_review_discount), "- " + formatMoney(discount, locale), MaterialTheme.colorScheme.secondary)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(Res.string.pricing_offerb_review_customer_pays), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Text(formatMoney(finalPay, locale), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(Res.string.pricing_offerb_review_sync_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(13.dp),
            )
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String, divider: Boolean) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SampleRow(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor)
    }
}

/** Major-unit money text → Double (null when blank/invalid). */
private fun String.toMajorOrNull(): Double? = trim().toDoubleOrNull()
