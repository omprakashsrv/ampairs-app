package com.ampairs.product.data

import com.ampairs.common.di.WorkspaceScope
import com.ampairs.form.render.DynamicOptionProvider
import com.ampairs.form.render.FieldOption
import com.ampairs.product.db.dao.BrandDao
import com.ampairs.product.db.dao.CategoryDao
import com.ampairs.product.db.dao.GroupDao
import com.ampairs.product.db.dao.SubCategoryDao
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Dynamic option sources for product choice fields (spec 011, US5). The schema-driven renderer
 * resolves a field's `dynamicSourceKey` (matching the backend ProductStandardFieldProvider keys)
 * to these providers and populates the dropdown from live, already-synced workspace data.
 * Values are catalog UIDs — that is what the product columns (groupId/categoryId/...) store.
 */
@Inject
@ContributesIntoSet(WorkspaceScope::class)
class ProductGroupOptionProvider(
    private val groupDao: GroupDao,
) : DynamicOptionProvider {
    override val sourceKey: String = "product_groups"
    override fun options(): Flow<List<FieldOption>> =
        groupDao.observeGroups().map { rows -> rows.map { FieldOption(it.id, it.name) } }
}

@Inject
@ContributesIntoSet(WorkspaceScope::class)
class ProductCategoryOptionProvider(
    private val categoryDao: CategoryDao,
) : DynamicOptionProvider {
    override val sourceKey: String = "product_categories"
    override fun options(): Flow<List<FieldOption>> =
        categoryDao.observeCategories().map { rows -> rows.map { FieldOption(it.id, it.name) } }
}

@Inject
@ContributesIntoSet(WorkspaceScope::class)
class ProductSubCategoryOptionProvider(
    private val subCategoryDao: SubCategoryDao,
) : DynamicOptionProvider {
    override val sourceKey: String = "product_sub_categories"
    override fun options(): Flow<List<FieldOption>> =
        subCategoryDao.observeSubCategories().map { rows -> rows.map { FieldOption(it.id, it.name) } }
}

@Inject
@ContributesIntoSet(WorkspaceScope::class)
class ProductBrandOptionProvider(
    private val brandDao: BrandDao,
) : DynamicOptionProvider {
    override val sourceKey: String = "product_brands"
    override fun options(): Flow<List<FieldOption>> =
        brandDao.observeBrands().map { rows -> rows.map { FieldOption(it.id, it.name) } }
}
