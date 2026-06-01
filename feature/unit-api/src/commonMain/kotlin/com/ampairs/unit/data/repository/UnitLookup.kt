package com.ampairs.unit.data.repository

import com.ampairs.unit.domain.model.Unit

interface UnitLookup {
    suspend fun getActiveUnits(): List<Unit>
    suspend fun getUnitById(id: String): Unit?
}
