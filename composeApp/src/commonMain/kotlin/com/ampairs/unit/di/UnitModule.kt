package com.ampairs.unit.di

import com.ampairs.unit.data.api.UnitApi
import com.ampairs.unit.data.api.UnitApiImpl
import com.ampairs.unit.data.db.UnitDatabase
import com.ampairs.unit.data.repository.UnitRepository
import com.ampairs.unit.domain.UnitStore
import com.ampairs.unit.ui.form.UnitFormViewModel
import com.ampairs.unit.ui.list.UnitListViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Unit Module - Common Dependency Injection
 *
 * CRITICAL: Workspace Isolation Pattern
 * - API: single (stateless, can be shared)
 * - Database: factory (fresh instance per workspace)
 * - DAOs: factory (tied to database instance)
 * - Repository: factory (tied to DAOs)
 * - Store: factory (tied to repository)
 * - ViewModels: viewModel/viewModelOf (navigation-scoped)
 *
 * All workspace-aware components use factory scope to ensure fresh instances
 * when switching between workspaces. This prevents stale data and ensures proper
 * database isolation.
 */
val unitModule = module {

    // ======================================
    // API LAYER - Single (Stateless)
    // ======================================
    singleOf(::UnitApiImpl) bind UnitApi::class

    // ======================================
    // DATABASE LAYER - Factory (Workspace-Aware)
    // ======================================
    // DAOs are created from the database instance
    // Use factory to get fresh DAOs with new database after workspace switch
    factory { get<UnitDatabase>().unitDao() }
    factory { get<UnitDatabase>().unitConversionDao() }

    // ======================================
    // REPOSITORY LAYER - Factory (Workspace-Aware)
    // ======================================
    // Repository depends on DAOs, so it must also be factory-scoped
    factory {
        UnitRepository(
            unitApi = get(),
            unitDao = get(),
            unitStore = get()
        )
    }

    // ======================================
    // DOMAIN LAYER - Factory (Workspace-Aware)
    // ======================================
    // Store depends on DAOs, so it must also be factory-scoped
    factory {
        UnitStore(
            unitApi = get(),
            unitDao = get()
        )
    }

    // ======================================
    // VIEW MODELS - Navigation-Scoped
    // ======================================
    // ViewModels use viewModel or viewModelOf which creates instances per navigation entry
    // This automatically handles recreation when navigating

    /**
     * List ViewModel - no parameters
     */
    viewModelOf(::UnitListViewModel)

    /**
     * Form ViewModel - with optional unitId parameter for edit mode
     */
    viewModel { (unitId: String?) ->
        UnitFormViewModel(
            unitRepository = get(),
            unitId = unitId
        )
    }
}
