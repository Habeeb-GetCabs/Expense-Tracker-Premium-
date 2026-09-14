package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.model.Transaction
import com.example.ui.ExpenseViewModel
import com.example.ui.ExpenseViewModelFactory
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddTransactionDialog
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditTransactionDialog
import com.example.ui.screens.PendingSmsScreen
import com.example.ui.screens.RemindersScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ExpenseViewModel by viewModels {
        ExpenseViewModelFactory((application as ExpenseApplication).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                ExpenseAppMainScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseAppMainScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val pendingTransactions by viewModel.pendingTransactions.collectAsStateWithLifecycle()
    val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
    val paymentReminders by viewModel.paymentReminders.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val currentBalance by viewModel.currentBalance.collectAsStateWithLifecycle()
    val categorySpending by viewModel.categorySpending.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }

    val categoryNames = remember(categoryBudgets) {
        val names = categoryBudgets.map { it.categoryName }.toMutableList()
        if (names.isEmpty()) {
            listOf("Grocery", "Medical", "Hospital", "Dining", "Shopping", "Transport", "Utilities", "Salary", "Rent", "Entertainment", "Other")
        } else {
            names
        }
    }

    val totalBudgetLimit = remember(categoryBudgets) {
        categoryBudgets.sumOf { it.monthlyLimit }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val receiveGranted = permissions[Manifest.permission.RECEIVE_SMS] ?: false
        val readGranted = permissions[Manifest.permission.READ_SMS] ?: false

        if (receiveGranted || readGranted) {
            viewModel.scanInboxSms(context) { count ->
                scope.launch {
                    snackbarHostState.showSnackbar("Scanned inbox: Parsed $count new transaction SMS!")
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("SMS Permission denied. You can test parser manually or approve transactions!")
            }
        }
    }

    fun checkAndRequestSmsPermissions(onAlreadyGranted: () -> Unit) {
        val hasReceive = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val hasRead = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED

        if (hasReceive && hasRead) {
            onAlreadyGranted()
        } else {
            smsPermissionLauncher.launch(
                arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
            )
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .width(320.dp)
                    .testTag("navigation_drawer")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        // Drawer Header Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF1E1B4B),
                                            Color(0xFF312E81),
                                            Color(0xFF4338CA)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AccountBalanceWallet,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Expense Tracker",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = "v2.0 • Pro Edition",
                                            color = Color(0xFFC7D2FE),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "Automated Bank SMS Parsing & Smart Budget Management",
                                    color = Color(0xFFE0E7FF),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "NAVIGATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        // Nav Items
                        Screen.bottomNavItems.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationDrawerItem(
                                icon = {
                                    if (screen == Screen.PendingSms && pendingTransactions.isNotEmpty()) {
                                        BadgedBox(
                                            badge = {
                                                Badge { Text(pendingTransactions.size.toString()) }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                                contentDescription = screen.title
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title
                                        )
                                    }
                                },
                                label = { Text(screen.title, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                                selected = selected,
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = false
                                        }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .testTag("drawer_item_${screen.route}")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "QUICK ACTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )

                        NavigationDrawerItem(
                            icon = { Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null) },
                            label = { Text("Scan Bank SMS Inbox") },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                checkAndRequestSmsPermissions {
                                    viewModel.scanInboxSms(context) { count ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Inbox scan complete: Parsed $count transactions!")
                                        }
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }

                    // Developer Credit Card Footer
                    Column {
                        Spacer(modifier = Modifier.height(24.dp))
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("developer_credit_card")
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Code,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = "Developed by Basheer",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Copyright,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "2026 Basheer. All Rights Reserved.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Built with Kotlin & Jetpack Compose",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = when (currentRoute) {
                                    Screen.Dashboard.route -> "Expense Tracker"
                                    Screen.Transactions.route -> "Ledger"
                                    Screen.PendingSms.route -> "SMS Approvals"
                                    Screen.Reminders.route -> "Payment Reminders"
                                    Screen.Categories.route -> "Budgets"
                                    Screen.Statistics.route -> "Analytics"
                                    else -> "Expense Tracker"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("hamburger_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Open Hamburger Menu"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    Screen.bottomNavItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route

                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            },
                            icon = {
                                if (screen == Screen.PendingSms && pendingTransactions.isNotEmpty()) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(pendingTransactions.size.toString())
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                            contentDescription = screen.title
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Dashboard.route
                ) {
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            currentBalance = currentBalance,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            pendingCount = pendingTransactions.size,
                            recentTransactions = transactions,
                            paymentReminders = paymentReminders,
                            totalBudgetLimit = totalBudgetLimit,
                            onNavigateToPendingSms = { navController.navigate(Screen.PendingSms.route) },
                            onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
                            onNavigateToReminders = { navController.navigate(Screen.Reminders.route) },
                            onOpenAddDialog = { showAddDialog = true },
                            onScanInboxSms = {
                                checkAndRequestSmsPermissions {
                                    viewModel.scanInboxSms(context) { count ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Inbox scan complete: Parsed $count transactions!")
                                        }
                                    }
                                }
                            },
                            onSimulateSms = { navController.navigate(Screen.PendingSms.route) },
                            onEditTransaction = { tx -> editingTransaction = tx },
                            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                            onMarkReminderPaid = { reminder ->
                                viewModel.markReminderAsPaid(reminder)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Recorded payment for ${reminder.title} in ledger!")
                                }
                            }
                        )
                    }

                    composable(Screen.Transactions.route) {
                        TransactionsScreen(
                            transactions = transactions,
                            categories = categoryNames,
                            onEditTransaction = { tx -> editingTransaction = tx },
                            onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                            onOpenAddDialog = { showAddDialog = true }
                        )
                    }

                    composable(Screen.PendingSms.route) {
                        PendingSmsScreen(
                            pendingTransactions = pendingTransactions,
                            categories = categoryNames,
                            onApprove = { pending, cat, amt, merch ->
                                viewModel.approvePending(pending, cat, amt, merch)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Transaction approved to ledger!")
                                }
                            },
                            onReject = { pending ->
                                viewModel.rejectPending(pending)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Pending transaction dismissed.")
                                }
                            },
                            onScanInbox = {
                                checkAndRequestSmsPermissions {
                                    viewModel.scanInboxSms(context) { count ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Inbox scan complete: Parsed $count transactions!")
                                        }
                                    }
                                }
                            },
                            onSimulateSms = { smsText, sender ->
                                val success = viewModel.simulateSmsParsing(smsText, sender)
                                if (success) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Parsed SMS added to Pending Queue!")
                                    }
                                }
                                success
                            }
                        )
                    }

                    composable(Screen.Reminders.route) {
                        RemindersScreen(
                            reminders = paymentReminders,
                            categories = categoryNames,
                            onAddReminder = { title, amt, type, cat, dueDay, notifyDays, notes ->
                                viewModel.addPaymentReminder(title, amt, type, cat, dueDay, notifyDays, notes)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Payment reminder added for $title!")
                                }
                            },
                            onDeleteReminder = { reminder ->
                                viewModel.deletePaymentReminder(reminder)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Reminder removed.")
                                }
                            },
                            onMarkAsPaid = { reminder ->
                                viewModel.markReminderAsPaid(reminder)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Payment of ${reminder.amount} recorded for ${reminder.title}!")
                                }
                            }
                        )
                    }

                    composable(Screen.Categories.route) {
                        CategoriesScreen(
                            categoryBudgets = categoryBudgets,
                            categorySpending = categorySpending,
                            onAddOrUpdateBudget = { name, limit ->
                                viewModel.addCategoryBudget(name, limit)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Saved budget for $name")
                                }
                            },
                            onDeleteBudget = { name -> viewModel.deleteCategoryBudget(name) }
                        )
                    }

                    composable(Screen.Statistics.route) {
                        StatisticsScreen(
                            categorySpending = categorySpending,
                            totalIncome = totalIncome,
                            totalExpense = totalExpense,
                            categoryBudgets = categoryBudgets
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTransactionDialog(
            categories = categoryNames,
            onDismiss = { showAddDialog = false },
            onSave = { amount, type, category, merchantOrNote ->
                viewModel.addTransaction(amount, type, category, merchantOrNote)
                showAddDialog = false
                scope.launch {
                    snackbarHostState.showSnackbar("Transaction saved!")
                }
            },
            onAddCustomCategory = { newCat ->
                viewModel.addCategoryBudget(newCat, 5000.0)
            }
        )
    }

    editingTransaction?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            categories = categoryNames,
            onDismiss = { editingTransaction = null },
            onSave = { updatedTx ->
                viewModel.editTransaction(updatedTx)
                editingTransaction = null
                scope.launch {
                    snackbarHostState.showSnackbar("Transaction updated successfully!")
                }
            },
            onDelete = { txToDelete ->
                viewModel.deleteTransaction(txToDelete)
                editingTransaction = null
                scope.launch {
                    snackbarHostState.showSnackbar("Transaction deleted!")
                }
            }
        )
    }
}
