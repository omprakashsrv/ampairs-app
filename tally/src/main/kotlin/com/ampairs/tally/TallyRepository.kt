package com.ampairs.tally

import com.ampairs.tally.model.ReportType
import com.ampairs.tally.model.TallyXML
import com.ampairs.tally.model.Type
import com.ampairs.tally.model.toTallyXML

class TallyRepository(val tallyApi: TallyApi) {

    suspend fun getUnits(): TallyXML {
        return post(Type.UNIT.toTallyXML())
    }

    suspend fun getStockGroups(): TallyXML {
        return post(Type.STOCK_GROUP.toTallyXML())
    }

    suspend fun getStockCategories(): TallyXML {
        return post(Type.STOCK_CATEGORY.toTallyXML())
    }


    suspend fun getInventoryStock(): TallyXML {
        return post(ReportType.STOCK_SUMMARY.toTallyXML())
    }

    suspend fun getStockItems(): TallyXML {
        return post(Type.STOCK_ITEM.toTallyXML())
    }

    suspend fun getLedgers(): TallyXML {
        return post(Type.LEDGER.toTallyXML())
    }

    suspend fun getGroups(): TallyXML {
        return post(Type.GROUP.toTallyXML())
    }


    suspend fun post(tallyXML: TallyXML): TallyXML {
        return tallyApi.post(tallyXML)
    }

}