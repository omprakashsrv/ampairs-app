package com.ampairs.file.api

enum class FileEntityType(val backendValue: String) {
    PRODUCT("PRODUCT"),
    CUSTOMER("CUSTOMER"),
    BRAND("BRAND"),
    CATEGORY("CATEGORY"),
    SUB_CATEGORY("SUB_CATEGORY"),
    GROUP("GROUP"),

    /** A static HTML print-template file (one per static template, entityUid = template id). */
    PRINT_TEMPLATE("PRINT_TEMPLATE"),
}
