package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.CategoryBudget
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.formatCurrency
import com.example.ui.components.getCategoryColor

@Composable
fun CategoriesScreen(
    categoryBudgets: List<CategoryBudget>,
    categorySpending: Map<String, Double>,
    onAddOrUpdateBudget: (name: String, limit: Double) -> Unit,
    onDeleteBudget: (name: String) -> Unit
) {
    var editingCategory by remember { mutableStateOf<CategoryBudget?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Category Budgeting",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Set monthly spending limits and monitor budget alerts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categoryBudgets, key = { it.categoryName }) { budget ->
                    val spent = categorySpending[budget.categoryName] ?: 0.0
                    CategoryBudgetCard(
                        budget = budget,
                        spentAmount = spent,
                        onEdit = { editingCategory = budget },
                        onDelete = { onDeleteBudget(budget.categoryName) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("fab_add_category")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Category")
        }
    }

    if (editingCategory != null) {
        EditBudgetDialog(
            categoryName = editingCategory!!.categoryName,
            currentLimit = editingCategory!!.monthlyLimit,
            onDismiss = { editingCategory = null },
            onSave = { newLimit ->
                onAddOrUpdateBudget(editingCategory!!.categoryName, newLimit)
                editingCategory = null
            }
        )
    }

    if (showAddDialog) {
        AddCategoryWithLimitDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, limit ->
                onAddOrUpdateBudget(name, limit)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun CategoryBudgetCard(
    budget: CategoryBudget,
    spentAmount: Double,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val limit = budget.monthlyLimit
    val progress = if (limit > 0) (spentAmount / limit).coerceIn(0.0, 1.0).toFloat() else 0f
    val isOverLimit = spentAmount > limit && limit > 0
    val isNearLimit = progress >= 0.85f && !isOverLimit
    val catColor = getCategoryColor(budget.categoryName)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("category_card_${budget.categoryName}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryIconBadge(categoryName = budget.categoryName)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = budget.categoryName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (limit > 0) "Limit: ${formatCurrency(limit)}" else "No limit set",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatCurrency(spentAmount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (isOverLimit) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                        )
                        if (limit > 0) {
                            Text(
                                text = "${(progress * 100).toInt()}% spent",
                                fontSize = 11.sp,
                                color = if (isOverLimit) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Budget", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (limit > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = when {
                        isOverLimit -> Color(0xFFDC2626)
                        isNearLimit -> Color(0xFFF59E0B)
                        else -> catColor
                    },
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }

            if (isOverLimit || isNearLimit) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isOverLimit) Color(0xFFDC2626) else Color(0xFFD97706),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isOverLimit) "Exceeded by ${formatCurrency(spentAmount - limit)}" else "Near spending limit (85%+)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isOverLimit) Color(0xFFDC2626) else Color(0xFFD97706)
                    )
                }
            }
        }
    }
}

@Composable
fun EditBudgetDialog(
    categoryName: String,
    currentLimit: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var limitText by remember { mutableStateOf(if (currentLimit > 0) currentLimit.toInt().toString() else "") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Edit Budget for $categoryName",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly Budget Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("edit_budget_input")
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val limit = limitText.toDoubleOrNull() ?: 0.0
                            onSave(limit)
                        }
                    ) {
                        Text("Save Limit")
                    }
                }
            }
        }
    }
}

@Composable
fun AddCategoryWithLimitDialog(
    onDismiss: () -> Unit,
    onSave: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var limitText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add Custom Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Hospital, Medical, Grocery") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_category_name_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = limitText,
                    onValueChange = { limitText = it },
                    label = { Text("Monthly Budget Limit (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(name.trim(), limitText.toDoubleOrNull() ?: 0.0)
                            }
                        },
                        enabled = name.isNotBlank()
                    ) {
                        Text("Add Category")
                    }
                }
            }
        }
    }
}
