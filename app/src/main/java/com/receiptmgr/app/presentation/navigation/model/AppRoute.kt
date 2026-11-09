package com.receiptmgr.app.presentation.navigation.model

sealed interface AppRoute {
    val key: String

    data object Home : AppRoute {
        override val key: String = "route_home"
    }

    data object Login : AppRoute {
        override val key: String = "route_login"
    }

    data object Settings : AppRoute {
        override val key: String = "route_settings"
    }

    data object ApiLogs : AppRoute {
        override val key: String = "route_api_logs"
    }

    data class ApiLogDetail(val rawJson: String) : AppRoute {
        override val key: String = "route_api_log_detail_${rawJson.hashCode()}"
    }

    data object NotificationLogs : AppRoute {
        override val key: String = "route_notification_logs"
    }
}
