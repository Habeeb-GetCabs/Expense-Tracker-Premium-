package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        selectedIcon = Icons.Filled.AccountBalanceWallet,
        unselectedIcon = Icons.Outlined.AccountBalanceWallet
    )

    data object Transactions : Screen(
        route = "transactions",
        title = "Ledger",
        selectedIcon = Icons.Filled.ReceiptLong,
        unselectedIcon = Icons.Outlined.ReceiptLong
    )

    data object PendingSms : Screen(
        route = "pending_sms",
        title = "SMS Approvals",
        selectedIcon = Icons.Filled.MarkEmailUnread,
        unselectedIcon = Icons.Outlined.MarkEmailUnread
    )

    data object Reminders : Screen(
        route = "reminders",
        title = "Reminders",
        selectedIcon = Icons.Filled.Alarm,
        unselectedIcon = Icons.Outlined.Alarm
    )

    data object Categories : Screen(
        route = "categories",
        title = "Budgets",
        selectedIcon = Icons.Filled.Category,
        unselectedIcon = Icons.Outlined.Category
    )

    data object Statistics : Screen(
        route = "statistics",
        title = "Analytics",
        selectedIcon = Icons.Filled.BarChart,
        unselectedIcon = Icons.Outlined.BarChart
    )

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Dashboard, Transactions, PendingSms, Reminders, Categories, Statistics)
    }
}
