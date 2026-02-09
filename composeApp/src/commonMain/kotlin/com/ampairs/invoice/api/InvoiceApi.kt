package com.ampairs.invoice.api

import com.ampairs.invoice.api.model.InvoiceApiModel
import com.ampairs.common.model.Response

interface InvoiceApi {

    suspend fun updateInvoice(invoice: InvoiceApiModel): Response<InvoiceApiModel>
    suspend fun getInvoices(lastUpdated: Long): Response<List<InvoiceApiModel>>
}