package com.ampairs.business.domain

import com.ampairs.business.data.repository.BusinessRepository
import com.ampairs.common.di.WorkspaceScope
import com.ampairs.common.locale.AppLocale
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Exposes the active workspace's [AppLocale] (timezone / date format / time format / currency) as a
 * reactive stream derived from the business profile. Lives in the workspace graph, so it is
 * recreated per workspace — no stale settings after a workspace switch.
 *
 * Surfaced to the UI as `LocalAppLocale` near the navigation root; formatters read it at display time.
 */
@Inject
@SingleIn(WorkspaceScope::class)
class BusinessLocaleProvider(
    businessRepository: BusinessRepository,
) {
    val locale: Flow<AppLocale> = businessRepository.observeBusiness().map { business ->
        if (business == null) {
            AppLocale.Default
        } else {
            AppLocale(
                timeZoneId = business.timezone.ifBlank { AppLocale.Default.timeZoneId },
                dateFormat = business.dateFormat.ifBlank { AppLocale.Default.dateFormat },
                timeFormat = business.timeFormat.ifBlank { AppLocale.Default.timeFormat },
                currencyCode = business.currency.ifBlank { AppLocale.Default.currencyCode },
            )
        }
    }
}
