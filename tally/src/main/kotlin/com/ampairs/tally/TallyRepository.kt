package com.ampairs.tally

import com.ampairs.tally.model.ReportType
import com.ampairs.tally.model.TallyXML
import com.ampairs.tally.model.Type
import com.ampairs.tally.model.buildVoucherImport
import com.ampairs.tally.model.toTallyXML
import com.ampairs.tally.model.voucher.Voucher

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

    suspend fun getStockBalances(): TallyXML {
        val xml = Type.STOCK_BALANCE.toTallyXML()
        val collection = xml.body?.desc?.tdl?.tdlMessage?.collection!!
        collection.nativeMethod = listOf("NAME", "GUID", "BASEUNITS", "ALTERID")
        collection.compute = listOf(
            "CLOSINGBALANCE : \$CLOSINGBALANCE",
            "CLOSINGVALUE : \$CLOSINGVALUE",
        )
        return post(xml)
    }

    suspend fun getLedgers(): TallyXML {
        return post(Type.LEDGER.toTallyXML())
    }

    suspend fun getGroups(): TallyXML {
        return post(Type.GROUP.toTallyXML())
    }

    /** All vouchers (sales, purchase, receipts, payments, credit/debit notes) for invoice + payment sync. */
    suspend fun getVouchers(): TallyXML {
        return post(Type.VOUCHER.toTallyXML())
    }


    /**
     * Pushes vouchers *into* Tally via an IMPORTDATA request (used for order → invoice sales
     * vouchers). Returns the parsed [com.ampairs.tally.model.ImportResult] (created/altered counts,
     * LASTMID/LASTVCHID, any LINEERROR); null only if Tally returned an empty body. Tally's reply is a
     * bare `<RESPONSE>` root, so this goes through [TallyApi.postImport], not the `<ENVELOPE>`-typed
     * [post]. [companyName] targets a specific company; blank → Tally's active company.
     */
    suspend fun importVouchers(
        vouchers: List<Voucher>,
        companyName: String? = null,
    ): com.ampairs.tally.model.ImportResult? {
        return tallyApi.postImport(buildVoucherImport(vouchers, companyName))
    }

    suspend fun post(tallyXML: TallyXML): TallyXML {
        return tallyApi.post(tallyXML)
    }

}