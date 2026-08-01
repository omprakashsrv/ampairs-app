package com.ampairs.ecom.ui.directory

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ampairsapp.feature.ecom.generated.resources.Res
import ampairsapp.feature.ecom.generated.resources.ecom_directory_clear_search
import ampairsapp.feature.ecom.generated.resources.ecom_directory_empty_body
import ampairsapp.feature.ecom.generated.resources.ecom_directory_empty_title
import ampairsapp.feature.ecom.generated.resources.ecom_directory_offline_banner
import ampairsapp.feature.ecom.generated.resources.ecom_directory_error_body
import ampairsapp.feature.ecom.generated.resources.ecom_directory_error_title
import ampairsapp.feature.ecom.generated.resources.ecom_directory_search
import ampairsapp.feature.ecom.generated.resources.ecom_directory_single_subtitle
import ampairsapp.feature.ecom.generated.resources.ecom_directory_single_title
import ampairsapp.feature.ecom.generated.resources.ecom_directory_start_ordering
import ampairsapp.feature.ecom.generated.resources.ecom_directory_subtitle
import ampairsapp.feature.ecom.generated.resources.ecom_directory_title
import ampairsapp.feature.ecom.generated.resources.ecom_directory_try_again
import coil3.compose.AsyncImage
import com.ampairs.ecom.api.model.Storefront
import dev.zacsweers.metrox.viewmodel.metroViewModel
import org.jetbrains.compose.resources.stringResource

private val BannerHeight = 116.dp
private val LogoBadgeSize = 60.dp
private val LogoBadgeOverlap = 30.dp
private val HeroBannerHeight = 180.dp
private val HeroLogoSize = 76.dp
private val HeroLogoOverlap = 38.dp

@Composable
fun StorefrontDirectoryScreen(
    onStorefrontSelected: (slug: String, brandColorArgb: Long?) -> Unit,
    viewModel: StorefrontDirectoryViewModel = metroViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isInitialLoad = state.isLoading && state.storefronts.isEmpty() && state.error == null
    val isSingleStore = !isInitialLoad && state.storefronts.size == 1

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(
                text = if (isSingleStore) stringResource(Res.string.ecom_directory_single_title)
                else stringResource(Res.string.ecom_directory_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = if (isSingleStore) stringResource(Res.string.ecom_directory_single_subtitle)
                else stringResource(Res.string.ecom_directory_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }

        DirectorySearchBar(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            isLoading = state.isLoading,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        if (state.isOffline && state.storefronts.isNotEmpty()) {
            OfflineBanner(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                isInitialLoad -> SkeletonContent()
                state.error != null && state.storefronts.isEmpty() -> ErrorContent(onRetry = viewModel::refresh)
                state.storefronts.isEmpty() -> EmptySearchContent(onClearSearch = { viewModel.onQueryChange("") })
                isSingleStore -> SingleStoreHeroContent(
                    store = state.storefronts.first(),
                    onSelect = { onStorefrontSelected(it.slug, it.brandColorArgb) },
                )
                else -> StoreListContent(
                    stores = state.storefronts,
                    onSelect = { onStorefrontSelected(it.slug, it.brandColorArgb) },
                )
            }
        }
    }
}

// ─── Search bar ───────────────────────────────────────────────────────────────

@Composable
private fun DirectorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurface,
            ),
            decorationBox = { innerTextField ->
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .height(52.dp)
                        .fillMaxWidth(),
                ) {
                    Row(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    stringResource(Res.string.ecom_directory_search),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            innerTextField()
                        }
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.size(32.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            },
        )
        // Thin indeterminate bar under search when any fetch is in flight
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        } else {
            Spacer(Modifier.height(7.dp))
        }
    }
}

// ─── Offline banner ─────────────────────────────────────────────────────────────

@Composable
private fun OfflineBanner(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Text(
                stringResource(Res.string.ecom_directory_offline_banner),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

// ─── Store list ───────────────────────────────────────────────────────────────

@Composable
private fun StoreListContent(
    stores: List<Storefront>,
    onSelect: (Storefront) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(stores, key = { it.slug }) { store ->
            StorefrontCard(store = store, onClick = { onSelect(store) })
        }
    }
}

// ─── Store card ───────────────────────────────────────────────────────────────

@Composable
private fun StorefrontCard(
    store: Storefront,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val brandColor = remember(store.brandColorArgb, primaryColor) {
        store.brandColorArgb?.let { Color(it.toInt()) } ?: primaryColor
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 4.dp),
    ) {
        // IntrinsicSize.Min lets the edge strip (fillMaxHeight) fill the full card height
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(brandColor),
            )
            // Content area — logo badge absolutely positioned over banner/content boundary
            Box(Modifier.fillMaxWidth()) {
                Column {
                    StorefrontBanner(store = store, brandColor = brandColor, height = BannerHeight)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 11.dp, end = 14.dp, bottom = 14.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // Reserve layout space for the bottom part of the overlapping logo
                        Spacer(Modifier.width(LogoBadgeSize).height(LogoBadgeOverlap))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f).padding(top = 10.dp)) {
                            Text(
                                store.name.ifBlank { store.slug },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val desc = store.description
                            if (!desc.isNullOrBlank()) {
                                Text(
                                    desc,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        Icon(
                            Icons.Filled.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.CenterVertically),
                        )
                    }
                }
                // Logo badge — laps the banner/content seam by LogoBadgeOverlap
                LogoBadge(
                    store = store,
                    brandColor = brandColor,
                    ringColor = surfaceColor,
                    size = LogoBadgeSize,
                    ringWidth = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 11.dp)
                        .offset(y = BannerHeight - LogoBadgeOverlap),
                )
            }
        }
    }
}

// ─── Banner ───────────────────────────────────────────────────────────────────

@Composable
private fun StorefrontBanner(
    store: Storefront,
    brandColor: Color,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val darkVariant = remember(brandColor) {
        Color(
            red = (brandColor.red * 0.82f).coerceIn(0f, 1f),
            green = (brandColor.green * 0.82f).coerceIn(0f, 1f),
            blue = (brandColor.blue * 0.82f).coerceIn(0f, 1f),
            alpha = brandColor.alpha,
        )
    }
    val lightVariant = remember(brandColor) {
        Color(
            red = (brandColor.red * 1.20f).coerceIn(0f, 1f),
            green = (brandColor.green * 1.20f).coerceIn(0f, 1f),
            blue = (brandColor.blue * 1.20f).coerceIn(0f, 1f),
            alpha = brandColor.alpha,
        )
    }
    val initial = store.name.firstOrNull()?.uppercaseChar()?.toString() ?: ""

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to brandColor,
                            0.55f to darkVariant,
                            1f to lightVariant,
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                )
            },
    ) {
        // Large monogram watermark anchored to bottom-right, partially clipped by card shape
        if (store.bannerUrl.isNullOrBlank()) {
            Text(
                text = initial,
                fontSize = 150.sp,
                lineHeight = 150.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.13f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 30.dp),
            )
        }
        if (!store.bannerUrl.isNullOrBlank()) {
            AsyncImage(
                model = store.bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

// ─── Logo badge ───────────────────────────────────────────────────────────────

@Composable
private fun LogoBadge(
    store: Storefront,
    brandColor: Color,
    ringColor: Color,
    size: Dp,
    ringWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(ringColor)
            .padding(ringWidth)
            .clip(CircleShape)
            .background(brandColor),
        contentAlignment = Alignment.Center,
    ) {
        if (!store.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = store.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = store.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─── Single-store hero ────────────────────────────────────────────────────────

@Composable
private fun SingleStoreHeroContent(
    store: Storefront,
    onSelect: (Storefront) -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val brandColor = remember(store.brandColorArgb, primaryColor) {
        store.brandColorArgb?.let { Color(it.toInt()) } ?: primaryColor
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Card(
            onClick = { onSelect(store) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        ) {
            Box {
                Column {
                    StorefrontBanner(
                        store = store,
                        brandColor = brandColor,
                        height = HeroBannerHeight,
                    )
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    ) {
                        Spacer(Modifier.height(HeroLogoOverlap))
                        Text(
                            store.name.ifBlank { store.slug },
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        val desc = store.description
                        if (!desc.isNullOrBlank()) {
                            Text(
                                desc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        Button(
                            onClick = { onSelect(store) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp)
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = brandColor),
                        ) {
                            Text(
                                stringResource(Res.string.ecom_directory_start_ordering),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
                LogoBadge(
                    store = store,
                    brandColor = brandColor,
                    ringColor = surfaceColor,
                    size = HeroLogoSize,
                    ringWidth = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 20.dp)
                        .offset(y = HeroBannerHeight - HeroLogoOverlap),
                )
            }
        }
    }
}

// ─── Skeleton shimmer ─────────────────────────────────────────────────────────

@Composable
private fun SkeletonContent() {
    val shimmerBrush = rememberShimmerBrush()
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        userScrollEnabled = false,
    ) {
        items(3) { SkeletonCard(shimmerBrush) }
    }
}

@Composable
private fun SkeletonCard(shimmerBrush: Brush) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(BannerHeight)
                    .background(shimmerBrush),
            )
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    Modifier
                        .size(LogoBadgeSize)
                        .clip(CircleShape)
                        .background(shimmerBrush),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(top = 4.dp)) {
                    Box(
                        Modifier
                            .fillMaxWidth(0.55f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(shimmerBrush),
                    )
                    Spacer(Modifier.height(9.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.85f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(shimmerBrush),
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier
                            .fillMaxWidth(0.65f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(shimmerBrush),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberShimmerBrush(): Brush {
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val highlightColor = MaterialTheme.colorScheme.surface
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1400, easing = LinearEasing)),
        label = "shimmerX",
    )
    val x = progress * 1200f - 400f
    return Brush.horizontalGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        startX = x,
        endX = x + 500f,
    )
}

// ─── Error ────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            stringResource(Res.string.ecom_directory_error_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            stringResource(Res.string.ecom_directory_error_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onRetry,
            modifier = Modifier
                .padding(top = 24.dp)
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.ecom_directory_try_again))
        }
    }
}

// ─── Empty search ─────────────────────────────────────────────────────────────

@Composable
private fun EmptySearchContent(onClearSearch: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(92.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            stringResource(Res.string.ecom_directory_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            stringResource(Res.string.ecom_directory_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        OutlinedButton(
            onClick = onClearSearch,
            modifier = Modifier
                .padding(top = 20.dp)
                .height(44.dp),
            shape = RoundedCornerShape(22.dp),
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(Res.string.ecom_directory_clear_search))
        }
    }
}
