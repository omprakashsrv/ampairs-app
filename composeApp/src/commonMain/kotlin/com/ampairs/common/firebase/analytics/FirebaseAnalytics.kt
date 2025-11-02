package com.ampairs.common.firebase.analytics

/**
 * Common interface for Firebase Analytics across platforms
 */
expect class FirebaseAnalytics {
    /**
     * Log an event with the given name and parameters
     * @param eventName The name of the event to log
     * @param params Optional map of parameters to include with the event
     */
    fun logEvent(eventName: String, params: Map<String, Any>? = null)

    /**
     * Set a user property to a given value
     * @param name The name of the user property to set
     * @param value The value of the user property
     */
    fun setUserProperty(name: String, value: String?)

    /**
     * Set the user ID for analytics
     * @param userId The user identifier
     */
    fun setUserId(userId: String?)

    /**
     * Set the current screen name
     * @param screenName The name of the current screen
     * @param screenClass The class name of the screen (optional)
     */
    fun setCurrentScreen(screenName: String, screenClass: String? = null)

    /**
     * Enable or disable analytics collection
     * @param enabled Whether analytics collection should be enabled
     */
    fun setAnalyticsCollectionEnabled(enabled: Boolean)
}

/**
 * Common Analytics Event Names
 */
object AnalyticsEvents { 
    const val LOGIN = "login"
    const val LOGOUT = "logout"
    const val SIGN_UP = "sign_up"
    const val SCREEN_VIEW = "screen_view"
    const val SELECT_CONTENT = "select_content"
    const val SEARCH = "search"
    const val SHARE = "share"
    const val ADD_TO_CART = "add_to_cart"
    const val BEGIN_CHECKOUT = "begin_checkout"
    const val PURCHASE = "purchase"
    const val REFUND = "refund"
}

/**
 * Common Analytics Parameter Names
 */
object AnalyticsParams {
    const val METHOD = "method"
    const val CONTENT_TYPE = "content_type"
    const val ITEM_ID = "item_id"
    const val SEARCH_TERM = "search_term"
    const val VALUE = "value"
    const val CURRENCY = "currency"
    const val TRANSACTION_ID = "transaction_id"
    const val SCREEN_NAME = "screen_name"
    const val SCREEN_CLASS = "screen_class"
}
