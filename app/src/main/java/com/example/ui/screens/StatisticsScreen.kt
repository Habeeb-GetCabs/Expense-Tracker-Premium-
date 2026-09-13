package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryBudget
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.getCategoryColor

@Composable
fun StatisticsScreen(
    categorySpending: Map<String, Double>,
    totalIncome: Double,
    totalExpense: Double,
    categoryBudgets: List<CategoryBudget>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            Column {
                Text(
                    text = "Analytics & Budget Insights",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Visual breakdown of expenses and monthly spending alerts",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Donut Chart Card
        item {
            CategoryDonutChartCard(categorySpending = categorySpending, totalExpense = totalExpense)
        }

        // Income vs Expense Ratio Card
        item {
            IncomeVSExpenseRatioCard(income = totalIncome, expense = totalExpense)
        }

        // Category Spending Health & Alert List
        item {
            Text(
                text = "Budget Limits & Alerts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        val categoriesWithBudgets = categoryBudgets.filter { it.monthlyLimit > 0 }
        if (categoriesWithBudgets.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "No category limits set yet. Go to the Budgets tab to set target limits.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(categoriesWithBudgets) { budget ->
                val spent = categorySpending[budget.categoryName] ?: 0.0
                BudgetHealthRow(budget = budget, spent = spent)
            }
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun CategoryDonutChartCard(
    categorySpending: Map<String, Double>,
    totalExpense: Double
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth().testTag("donut_chart_card")
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Monthly Category Spending",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (totalExpense <= 0 || categorySpending.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No expenses recorded this month",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            } else {
                var animStarted by remember { mutableStateOf(false) }
                val animProgress by animateFloatAsState(
                    targetValue = if (animStarted) 1f else 0f,
                    animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                    label = "chartAnim"
                )

                LaunchedEffect(Unit) {
                    animStarted = true
                }

                Box(
                    modifier = Modifier.size(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(200.dp)) {
                        val strokeWidth = 36.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                        val arcSize = Size(diameter, diameter)

                        var startAngle = -90f
                        categorySpending.forEach { (cat, amount) ->
                            val sweepAngle = ((amount / totalExpense) * 360f).toFloat() * animProgress
                            val color = getCategoryColor(cat)

                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Spent",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalExpense),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Legend Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categorySpending.entries.sortedByDescending { it.value }.forEach { (cat, amt) ->
                        val percent = if (totalExpense > 0) ((amt / totalExpense) * 100).toInt() else 0
                        val color = getCategoryColor(cat)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cat,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "$percent%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = formatCurrency(amt),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeVSExpenseRatioCard(income: Double, expense: Double) {
    val total = (income + expense).coerceAtLeast(1.0)
    val incomeRatio = (income / total).toFloat()
    val expenseRatio = (expense / total).toFloat()

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Income vs Expense Comparison",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                if (incomeRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(incomeRatio)
                            .fillMaxSize()
                            .background(Color(0xFF10B981))
                    )
                }
                if (expenseRatio > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(expenseRatio)
                            .fillMaxSize()
                            .background(Color(0xFFEF4444))
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF10B981)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Income: ${formatCurrency(income)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Expense: ${formatCurrency(expense)}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun BudgetHealthRow(
    budget: CategoryBudget,
    spent: Double
) {
    val limit = budget.monthlyLimit
    val isOver = spent > limit
    val isNear = (spent / limit) >= 0.85f && !isOver

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isOver -> Color(0xFFFEF2F2)
                isNear -> Color(0xFFFFFBEB)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryIconBadge(categoryName = budget.categoryName)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = budget.categoryName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Spent ${formatCurrency(spent)} of ${formatCurrency(limit)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isOver || isNear) {
                Surface(
                    color = if (isOver) Color(0xFFDC2626) else Color(0xFFD97706),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isOver) "EXCEEDED" else "85%+ NEAR",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Text(
                    text = "SAFE",
                    color = Color(0xFF10B981),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
