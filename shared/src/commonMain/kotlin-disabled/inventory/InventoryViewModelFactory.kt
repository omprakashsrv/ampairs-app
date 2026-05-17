package com.ampairs.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

class InventoryViewModelFactory(private val creators: Map<KClass<out ViewModel>, (CreationExtras) -> ViewModel>) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        val creator =
            creators[modelClass] ?: creators.entries.firstOrNull { modelClass == it.key }?.value
            ?: throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
        return try {
            @Suppress("UNCHECKED_CAST")
            creator(extras) as T
        } catch (e: Exception) {
            throw RuntimeException("Failed to create ViewModel for ${modelClass.simpleName}", e)
        }
    }

}