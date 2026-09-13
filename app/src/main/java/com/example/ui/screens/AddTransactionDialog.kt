package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.components.getCategoryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onSave: (amount: Double, type: String, category: String, merchantOrNote: String) -> Unit,
    onAddCustomCategory: (String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("EXPENSE") }
    var selectedCategory by remember { mutableStateOf(if (categories.contains("Grocery")) "Grocery" else categories.firstOrNull() ?: "Other") }
    var merchantOrNote by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCustomCategoryDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().testTag("add_transaction_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Transaction",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("close_dialog_button")) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Income / Expense Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "EXPENSE") Color(0xFFEF4444) else Color.Transparent)
                            .clickable { type = "EXPENSE" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expense",
                            fontWeight = FontWeight.Bold,
                            color = if (type == "EXPENSE") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (type == "INCOME") Color(0xFF10B981) else Color.Transparent)
                            .clickable { type = "INCOME" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Income",
                            fontWeight = FontWeight.Bold,
                            color = if (type == "INCOME") Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Amount (₹)") },
                    placeholder = { Text("e.g. 1250.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("amount_input")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { showCustomCategoryDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add custom category", modifier = Modifier.padding(end = 4.dp))
                        Text("Custom")
                    }
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        val isSelected = cat == selectedCategory
                        val catColor = getCategoryColor(cat)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(catColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.2f),
                                selectedLabelColor = catColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Merchant / Note Input
                OutlinedTextField(
                    value = merchantOrNote,
                    onValueChange = { merchantOrNote = it },
                    label = { Text("Merchant or Note") },
                    placeholder = { Text("e.g. Grocery store, Swiggy, Salary") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("merchant_input")
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            errorMessage = "Please enter a valid positive amount"
                        } else {
                            onSave(amount, type, selectedCategory, merchantOrNote)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_transaction_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (type == "EXPENSE") Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "Save Transaction",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    if (showCustomCategoryDialog) {
        AddCustomCategoryDialog(
            onDismiss = { showCustomCategoryDialog = false },
            onAdd = { newCat ->
                onAddCustomCategory(newCat)
                selectedCategory = newCat
                showCustomCategoryDialog = false
            }
        )
    }
}

@Composable
fun AddCustomCategoryDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Add Custom Category",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Medical, Hospital, Pet Care") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_category_input")
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
                            if (categoryName.isNotBlank()) {
                                onAdd(categoryName.trim())
                            }
                        },
                        enabled = categoryName.isNotBlank()
                    ) {
                        Text("Add")
                    }
                }
            }
        }
    }
}
