package com.ampairs.tally.model.master


enum class VoucherAction(private val action: String) {
    Create("Create"),
    Alter("Alter"),
    Cancel("Cancel"),
    Delete("Delete")
}
