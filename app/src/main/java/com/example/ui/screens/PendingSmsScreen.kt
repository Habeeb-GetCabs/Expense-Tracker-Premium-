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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PendingTransaction
import com.example.ui.components.CategoryIconBadge
import com.example.ui.components.TransactionTypeTag
import com.example.ui.components.formatCurrency
import com.example.ui.components.formatDate
import com.example.ui.components.getCategoryColor

@Composable
fun PendingSmsScreen(
    pendingTransactions: List<PendingTransaction>,
    categories: List<String>,
    onApprove: (pending: PendingTransaction, editedCategory: String?, editedAmount: Double?, editedMerchant: String?) -> Unit,
    onReject: (pending: PendingTransaction) -> Unit,
    onScanInbox: () -> Unit,
    onSimulateSms: (rawSms: String, sender: String) -> Boolean
) {
    var showSimulateDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header info & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SMS Approval Queue",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Confirm auto-parsed bank SMS before updating budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onScanInbox,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("scan_inbox_sms_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Scan Inbox", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = { showSimulateDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("test_parser_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Parser", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pendingTransactions.isEmpty()) {
                EmptyPendingPlaceholder(onScanInbox = onScanInbox, onTestSms = { showSimulateDialog = true })
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(pendingTransactions, key = { it.id }) { pending ->
                        PendingItemCard(
                            pending = pending,
                            categories = categories,
                            onApprove = { cat, amt, merch -> onApprove(pending, cat, amt, merch) },
                            onReject = { onReject(pending) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showSimulateDialog) {
        SimulateSmsDialog(
            onDismiss = { showSimulateDialog = false },
            onSimulate = { smsText, sender ->
                val success = onSimulateSms(smsText, sender)
                if (success) showSimulateDialog = false
                success
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingItemCard(
    pending: PendingTransaction,
    categories: List<String>,
    onApprove: (category: String?, amount: Double?, merchant: String?) -> Unit,
    onReject: () -> Unit
) {
    var expandedSms by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf(pending.suggestedCategory) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().testTag("pending_item_${pending.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    CategoryIconBadge(categoryName = selectedCategory)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = pending.extractedMerchant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "From: ${pending.sender} • ${formatDate(pending.timestamp)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = (if (pending.extractedType == "INCOME") "+ " else "- ") + formatCurrency(pending.extractedAmount),
                        fontWeight = FontWeight.Bold,
                        color = if (pending.extractedType == "INCOME") Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 17.sp
                    )
                    TransactionTypeTag(type = pending.extractedType)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category picker chip list for quick category correction
            Text(
                text = "Assigned Category:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categories.take(4).forEach { cat ->
                    val isSelected = cat == selectedCategory
                    val catColor = getCategoryColor(cat)
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = catColor.copy(alpha = 0.2f),
                            selectedLabelColor = catColor
                        )
                    )
                }
            }

            // Expandable raw SMS snippet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedSms = !expandedSms }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (expandedSms) "Hide Raw SMS" else "Show Raw Bank SMS",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expandedSms) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = expandedSms) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = pending.rawSms,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Approve / Reject Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).testTag("reject_pending_${pending.id}"),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reject")
                }

                Button(
                    onClick = { onApprove(selectedCategory, null, null) },
                    modifier = Modifier.weight(1f).testTag("approve_pending_${pending.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Approve")
                }
            }
        }
    }
}

@Composable
fun SimulateSmsDialog(
    onDismiss: () -> Unit,
    onSimulate: (rawSms: String, sender: String) -> Boolean
) {
    var smsText by remember { mutableStateOf("Rs 2,450.00 debited from A/C XX8921 at APOLLO PHARMA on 13-Sep-26. Ref: 892109.") }
    var sender by remember { mutableStateOf("AD-HDFCBK") }
    var parseError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().testTag("simulate_sms_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Test Automated SMS Parser",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Paste bank SMS text to test regex extraction logic.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("Sender Header") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = smsText,
                    onValueChange = {
                        smsText = it
                        parseError = false
                    },
                    label = { Text("SMS Message Body") },
                    minLines = 3,
                    maxLines = 5,
                    isError = parseError,
                    modifier = Modifier.fillMaxWidth().testTag("sms_text_input")
                )

                if (parseError) {
                    Text(
                        text = "Could not parse transaction details from this text. Ensure it contains keywords like 'debited', 'credited', 'Rs.', or 'INR'.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

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
                            val success = onSimulate(smsText, sender)
                            if (!success) {
                                parseError = true
                            }
                        }
                    ) {
                        Text("Parse & Add")
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyPendingPlaceholder(onScanInbox: () -> Unit, onTestSms: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.MarkEmailRead,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No Pending SMS Approvals",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = "When bank SMS messages arrive, they will automatically appear here for your confirmation.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onScanInbox) {
                    Text("Scan Inbox")
                }
                OutlinedButton(onClick = onTestSms) {
                    Text("Test SMS")
                }
            }
        }
    }
}
