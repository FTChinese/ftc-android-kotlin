package com.ft.ftchinese.model.conversion

enum class AppEventType(val symbol: String) {
    FirstOpen("first_open"), // The first_open event should always be sent for attributing installs
    SessionStart("session_start"),
    InAppPurchase("in_app_purchase"),
    ViewItemList("view_item_list"),
    ViewItem("view_item"),
    ViewSearchResults("view_search_results"),
    AddToCart("add_to_cart"),
    ECommercePurchase("ecommerce_purchase"),
    Custom("custom");

    override fun toString(): String {
        return symbol
    }
}
