package com.ampairs.tally.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ampairs.customer.db.CustomerRepository
import com.ampairs.inventory.db.InventoryRepository
import com.ampairs.product.api.model.InventoryApiModel
import com.ampairs.product.api.model.ProductApiModel
import com.ampairs.product.api.model.TaxCodeApiModel
import com.ampairs.product.api.model.TaxInfoApiModel
import com.ampairs.product.db.TaxRepository
import com.ampairs.product.domain.TaxSpec
import com.ampairs.product.domain.TaxType
import com.ampairs.product.ui.group.GroupType
import com.ampairs.repository.ProductRepository
import com.ampairs.tally.TallyRepository
import com.ampairs.tally.dto.asProductApiModel
import com.ampairs.tally.dto.asStockCategoryModel
import com.ampairs.tally.dto.asStockGroupModel
import com.ampairs.tally.dto.asUnitApiModel
import com.ampairs.tally.dto.toCustomers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TallyViewModel(
    private val tallyRepository: TallyRepository,
    val customerRepository: CustomerRepository,
    val productRepository: ProductRepository,
    val inventoryRepository: InventoryRepository,
    val taxRepository: TaxRepository,
) :
    ViewModel() {

    fun syncLedgers(onSyncComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tallyXML = tallyRepository.getLedgers()
                tallyXML.body?.data?.collection?.ledgers?.filter { it.isBillWiseOn?.lowercase() == "yes" }
                    ?.toCustomers()
                    ?.let {
                        val updateCustomers = customerRepository.updateCustomers(it)
                        onSyncComplete(updateCustomers.data?.size ?: 0)
                        updateCustomers
                    }
            } catch (e: Exception) {
                onSyncComplete(0)
                println(e)
            }
        }
    }

    fun syncUnits(onSyncComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tallyXMLUnits = tallyRepository.getUnits()
                val units = tallyXMLUnits.body?.data?.collection?.units?.asUnitApiModel()
                units?.let {
                    val updateUnits = productRepository.updateUnits(it)
                    onSyncComplete(updateUnits.data?.size ?: 0)
                    updateUnits
                }
            } catch (e: Exception) {
                onSyncComplete(0)
            }
            val units = productRepository.getUnits().data
            if (units != null) {
                productRepository.saveUnits(units)
            }
        }
    }

    fun syncStockGroups(onSyncComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tallyXMLGroups = tallyRepository.getStockGroups()
                val groups = tallyXMLGroups.body?.data?.collection?.stockGroups?.asStockGroupModel()
                groups?.let {
                    val updateGroups = productRepository.updateGroups(groups)
                    onSyncComplete(updateGroups.data?.size ?: 0)
                    updateGroups
                }
            } catch (e: Exception) {
                onSyncComplete(0)
                println(e)
            }
            syncAllGroups()
        }
    }

    fun syncStockCategories(onSyncComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tallyXMLCategories = tallyRepository.getStockCategories()
                val categories =
                    tallyXMLCategories.body?.data?.collection?.stockCategories?.asStockCategoryModel()
                categories?.let {
                    val updateCategories =
                        productRepository.updateCategories(categories)
                    onSyncComplete(updateCategories.data?.size ?: 0)
                    updateCategories
                }
            } catch (e: Exception) {
                onSyncComplete(0)
                println(e)
            }
            syncAllGroups()
        }
    }

    fun syncInventoryStock(onSyncComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allProducts = productRepository.getProducts()
                val units = productRepository.getAllUnits()

                val stockSummary = tallyRepository.getInventoryStock()
                val inventories = mutableListOf<InventoryApiModel>()
                stockSummary.dspaccname?.forEachIndexed { index, stockItem ->
                    val name = stockItem.name
                    val productLists = allProducts.filter { product -> product.name == name }
                    if (productLists.size == 1) {
                        val product = productLists.get(0)
                        val closingStock = stockSummary.stockInfo?.get(index)?.closingStock
                        val stringList = closingStock?.qty?.split(" ")
                        if (stringList?.isNotEmpty() == true) {
                            val stock =
                                runCatching { stringList.get(0).toDouble() }.getOrElse { 0.0 }
                            val unit = if (stringList.size > 1) stringList.get(1) else ""
                            val productUnit = units.find { it.name == unit }
                            inventories.add(
                                InventoryApiModel(
                                    productId = product.id,
                                    stock = stock,
                                    baseUnitId = productUnit?.id,
                                    description = name,
                                    softDeleted = false
                                )
                            )
                        }

                    } else {
                        print("No or more than one product found : " + productLists.size)
                    }
                }
                inventories.chunked(200).forEach {
                    val updateInventories = inventoryRepository.updateInventories(it)
                    onSyncComplete(updateInventories.data?.size ?: 0)
                }

            } catch (e: Exception) {
                onSyncComplete(0)
                println(e)
            }
        }
    }

    private suspend fun syncAllGroups() {
        val allGroups = productRepository.getAllGroups()
        allGroups.data?.let { productRepository.saveAllGroups(it) }
    }

    fun syncStockItems(onSyncComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tallyXMLStockItems = tallyRepository.getStockItems()
                val taxCodeSet = mutableSetOf<TaxCodeApiModel>()
                tallyXMLStockItems.body?.data?.collection?.stockItems?.forEach { stockItem ->
                    stockItem.gstDetailList?.forEach {
                        val taxInfos = mutableListOf<TaxInfoApiModel>()
                        val rateDetailsList = it.stateWiseDetailsList?.get(0)?.rateDetailsList
                        val cgst =
                            rateDetailsList?.find { it.gstRateDutyHead == "Central Tax" }?.gstRate?.toDouble()
                                ?: 0.0
                        if (cgst > 0) {
                            taxInfos.add(
                                TaxInfoApiModel(
                                    id = "",
                                    name = "CGST [" + cgst + "%]",
                                    formattedName = "CGST [" + cgst + "%]",
                                    taxSpec = TaxSpec.INTRA,
                                    refId = "CGST_" + cgst,
                                    percentage = cgst,
                                    active = true,
                                    softDeleted = false
                                )
                            )
                        }
                        val sgst =
                            rateDetailsList?.find { it.gstRateDutyHead == "State Tax" }?.gstRate?.toDouble()
                                ?: 0.0
                        if (sgst > 0) {
                            taxInfos.add(
                                TaxInfoApiModel(
                                    id = "",
                                    name = "SGST [" + cgst + "%]",
                                    formattedName = "SGST [" + cgst + "%]",
                                    taxSpec = TaxSpec.INTRA,
                                    refId = "SGST_" + sgst,
                                    percentage = sgst,
                                    active = true,
                                    softDeleted = false
                                )
                            )
                        }
                        val igst =
                            rateDetailsList?.find { it.gstRateDutyHead == "Integrated Tax" }?.gstRate?.toDouble()
                                ?: 0.0
                        if (igst > 0) {
                            taxInfos.add(
                                TaxInfoApiModel(
                                    id = "",
                                    name = "IGST [" + igst + "%]",
                                    formattedName = "IGST [" + igst + "%]",
                                    taxSpec = TaxSpec.INTER,
                                    refId = "IGST_" + igst,
                                    percentage = igst,
                                    active = true,
                                    softDeleted = false
                                )
                            )
                        }
                        val cess =
                            rateDetailsList?.find { it.gstRateDutyHead == "Cess" }?.gstRate?.toDouble()
                                ?: 0.0
                        if (cess > 0) {
                            taxInfos.add(
                                TaxInfoApiModel(
                                    id = "",
                                    name = "CESS [" + cess + "%]",
                                    formattedName = "CESS [" + cess + "%]",
                                    taxSpec = TaxSpec.INTER,
                                    refId = "CESS_" + cess + "_INTER",
                                    percentage = cess,
                                    active = true,
                                    softDeleted = false
                                )
                            )
                            taxInfos.add(
                                TaxInfoApiModel(
                                    id = "",
                                    name = "CESS [" + cess + "%]",
                                    formattedName = "CESS [" + cess + "%]",
                                    taxSpec = TaxSpec.INTRA,
                                    refId = "CESS_" + cess + "_INTRA",
                                    percentage = cess,
                                    active = true,
                                    softDeleted = false
                                )
                            )
                        }

                        var description = it.hsnMasterName ?: ""
                        if (description.contains("Undefined")) {
                            description = ""
                        }
                        val taxCode = TaxCodeApiModel(
                            id = "",
                            refId = it.hsnCode,
                            type = TaxType.HSN,
                            code = it.hsnCode ?: "",
                            effectiveFrom = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(
                                Date(
                                    SimpleDateFormat(
                                        "yyyyMMdd",
                                        Locale.ENGLISH
                                    ).parse(it.applicableFrom).time
                                )
                            ),
                            active = true,
                            softDeleted = false,
                            description = description,
                            taxInfos = taxInfos
                        )
                        taxCodeSet.add(taxCode)
                    }
                }

                val taxInfoSet = hashSetOf<TaxInfoApiModel>()
                taxCodeSet.forEach {
                    taxInfoSet.addAll(it.taxInfos)
                }
                if (taxInfoSet.size > 0) {
                    taxRepository.updateTaxInfos(taxInfoSet.toList())
                }
                if (taxCodeSet.size > 0) {
                    taxRepository.updateTaxCodes(taxCodeSet.filter { it.taxInfos.isNotEmpty() }
                        .toList())
                }

                val groups = productRepository.getGroups(GroupType.GROUP)
                val categories = productRepository.getGroups(GroupType.CATEGORY)
                val units = productRepository.getAllUnits()

                val products = arrayListOf<ProductApiModel>()
                tallyXMLStockItems.body?.data?.collection?.stockItems?.forEach { stockItem ->
                    val product = stockItem.asProductApiModel()
                    product.groupId = groups.find { it1 ->
                        it1.name == stockItem.parent
                    }?.id
                    product.categoryId = categories.find { it1 ->
                        it1.name == stockItem.category
                    }?.id
                    product.baseUnitId = units.find { it1 ->
                        it1.name == stockItem.baseUnits
                    }?.id
                    product.subCategoryId = null
                    product.brandId = null
                    products.add(product)
                }
                products.chunked(200).forEach {
                    val updateProducts = productRepository.updateProducts(it)
                    onSyncComplete(updateProducts.data?.size ?: 0)
                }
            } catch (e: Exception) {
                onSyncComplete(0)
                println(e)
            }
        }

    }


}