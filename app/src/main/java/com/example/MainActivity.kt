package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.ExpenseViewModel
import com.example.ui.ExpenseViewModelFactory
import com.example.ui.navigation.Screen
import com.example.ui.screens.AddTransactionDialog
import com.example.ui.screens.CategoriesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PendingSmsScreen
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

@Composable
fun ExpenseAppMainScreen(viewModel: ExpenseViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val navController = rememberNavController()

    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val pendingTransactions by viewModel.pendingTransactions.collectAsStateWithLifecycle()
    val categoryBudgets by viewModel.categoryBudgets.collectAsStateWithLifecycle()
    val totalIncome by viewModel.totalIncome.collectAsStateWithLifecycle()
    val totalExpense by viewModel.totalExpense.collectAsStateWithLifecycle()
    val currentBalance by viewModel.currentBalance.collectAsStateWithLifecycle()
    val categorySpending by viewModel.categorySpending.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    // List of category names
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

    // Permission launcher for SMS permissions
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
                snackbarHostState.showSnackbar("SMS Permission was denied. You can still test parser manually or approve transactions!")
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Screen.bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route

                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
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
                        totalBudgetLimit = totalBudgetLimit,
                        onNavigateToPendingSms = { navController.navigate(Screen.PendingSms.route) },
                        onNavigateToTransactions = { navController.navigate(Screen.Transactions.route) },
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
                        onDeleteTransaction = { tx -> viewModel.deleteTransaction(tx) },
                        onResetSampleData = { viewModel.resetSampleData() }
                    )
                }

                composable(Screen.Transactions.route) {
                    TransactionsScreen(
                        transactions = transactions,
                        categories = categoryNames,
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
}
