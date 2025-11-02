package com.ampairs.tally.model.master

enum class VoucherName(private val voucherType: String) {
    Sales("Sales"),
    Receipt("Receipt"),
    Purchase("Purchase"),
    CreditNote("CreditNote"),
    DebitNote("DebitNote"),
    PhysicalStock("Physical Stock")
}
