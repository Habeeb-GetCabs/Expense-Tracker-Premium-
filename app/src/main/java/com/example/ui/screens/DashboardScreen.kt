package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Transaction
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.TransactionTypeTag
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate

@Composable
fun DashboardScreen(
    currentBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    pendingCount: Int,
    recentTransactions: List<Transaction>,
    totalBudgetLimit: Double,
    onNavigateToPendingSms: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onScanInboxSms: () -> Unit,
    onSimulateSms: () -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onResetSampleData: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Hero Balance Card
            item {
                HeroBalanceCard(
                    balance = currentBalance,
                    income = totalIncome,
                    expense = totalExpense
                )
            }

            // Pending SMS Approval Alert Banner
            if (pendingCount > 0) {
                item {
                    PendingSmsBanner(
                        pendingCount = pendingCount,
                        onReviewClicked = onNavigateToPendingSms
                    )
                }
            }

            // Overall Monthly Budget Progress & Alert
            item {
                MonthlyBudgetCard(
                    totalExpense = totalExpense,
                    totalBudgetLimit = totalBudgetLimit
                )
            }

            // Quick Actions Bar
            item {
                QuickActionsRow(
                    onOpenAdd = onOpenAddDialog,
                    onScanSms = onScanInboxSms,
                    onTestSms = onSimulateSms,
                    onResetData = onResetSampleData
                )
            }

            // Recent Transactions Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "View All",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigateToTransactions() }
                            .padding(8.dp)
                    )
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    EmptyTransactionsPlaceholder(onOpenAddDialog)
                }
            } else {
                items(recentTransactions.take(8)) { transaction ->
                    TransactionItemRow(
                        transaction = transaction,
                        onDelete = { onDeleteTransaction(transaction) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onOpenAddDialog,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("fab_add_transaction")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
        }
    }
}

@Composable
fun HeroBalanceCard(
    balance: Double,
    income: Double,
    expense: Double
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_balance_card")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E293B),
                            Color(0xFF334155)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Current Balance",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = formatCurrency(balance),
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Income Pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Income",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatCurrency(income),
                                color = Color(0xFF34D399),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Expense Pill
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEF4444).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Expense",
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                            Text(
                                text = formatCurrency(expense),
                                color = Color(0xFFF87171),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PendingSmsBanner(
    pendingCount: Int,
    onReviewClicked: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF3C7)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReviewClicked() }
            .testTag("pending_sms_banner")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$pendingCount Auto-Parsed SMS Pending",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF78350F),
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Review and approve transactions before adding to budget",
                        color = Color(0xFF92400E),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onReviewClicked,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Review", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun MonthlyBudgetCard(
    totalExpense: Double,
    totalBudgetLimit: Double
) {
    val progress = if (totalBudgetLimit > 0) (totalExpense / totalBudgetLimit).coerceIn(0.0, 1.0).toFloat() else 0f
    val isOverBudget = totalExpense > totalBudgetLimit && totalBudgetLimit > 0
    val isNearBudget = progress >= 0.85f && !isOverBudget

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOverBudget -> Color(0xFFFEF2F2)
                isNearBudget -> Color(0xFFFFFBEB)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        ),
        modifier = Modifier.fillMaxWidth().testTag("monthly_budget_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Budget Limit",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${formatCurrency(totalExpense)} / ${formatCurrency(totalBudgetLimit)}",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isOverBudget) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = when {
                    isOverBudget -> Color(0xFFDC2626)
                    isNearBudget -> Color(0xFFF59E0B)
                    else -> Color(0xFF10B981)
                },
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            AnimatedVisibility(visible = isOverBudget || isNearBudget) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isOverBudget) Color(0xFFDC2626) else Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverBudget) "Alert: You have exceeded your monthly limit!" else "Warning: You are near your monthly budget limit (85%+)",
                        color = if (isOverBudget) Color(0xFFDC2626) else Color(0xFFD97706),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onOpenAdd: () -> Unit,
    onScanSms: () -> Unit,
    onTestSms: () -> Unit,
    onResetData: () -> Unit
) {
    Column {
        Text(
            text = "Quick Tools",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onScanSms,
                modifier = Modifier.weight(1f).testTag("scan_sms_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan Inbox", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onTestSms,
                modifier = Modifier.weight(1f).testTag("test_sms_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Test SMS", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TransactionItemRow(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().testTag("transaction_item_${transaction.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                CategoryIconBadge(categoryName = transaction.category)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = transaction.merchantOrNote,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        if (transaction.isAutoParsed) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "SMS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${transaction.category} • ${formatDate(transaction.date)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (transaction.type == "INCOME") "+ " else "- ") + formatCurrency(transaction.amount),
                        fontWeight = FontWeight.Bold,
                        color = if (transaction.type == "INCOME") Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 15.sp
                    )
                    TransactionTypeTag(type = transaction.type)
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyTransactionsPlaceholder(onOpenAdd: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Transactions Recorded",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "Add manual income/expense or scan SMS messages automatically.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onOpenAdd) {
                Text("Add First Transaction")
            }
        }
    }
}
