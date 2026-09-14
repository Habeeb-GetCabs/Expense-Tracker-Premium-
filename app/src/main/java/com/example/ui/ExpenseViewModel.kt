package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryBudget
import com.example.data.model.PaymentReminder
import com.example.data.model.PendingTransaction
import com.example.data.model.Transaction
import com.example.parser.SmsParser
import com.example.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingTransactions: StateFlow<List<PendingTransaction>> = repository.allPendingTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val categoryBudgets: StateFlow<List<CategoryBudget>> = repository.allCategoryBudgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val paymentReminders: StateFlow<List<PaymentReminder>> = repository.allPaymentReminders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val totalIncome: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == "INCOME" && isCurrentMonth(it.date) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalExpense: StateFlow<Double> = transactions.map { list ->
        list.filter { it.type == "EXPENSE" && isCurrentMonth(it.date) }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentBalance: StateFlow<Double> = transactions.map { list ->
        val income = list.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = list.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        income - expense
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val categorySpending: StateFlow<Map<String, Double>> = transactions.map { list ->
        list.filter { it.type == "EXPENSE" && isCurrentMonth(it.date) }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun addTransaction(
        amount: Double,
        type: String,
        category: String,
        merchantOrNote: String,
        date: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = amount,
                type = type,
                category = category,
                merchantOrNote = merchantOrNote.ifBlank { "Manual Entry" },
                date = date,
                isAutoParsed = false
            )
            repository.insertTransaction(transaction)
        }
    }

    fun editTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun approvePending(pending: PendingTransaction, editedCategory: String? = null, editedAmount: Double? = null, editedMerchant: String? = null) {
        viewModelScope.launch {
            val finalTransaction = Transaction(
                amount = editedAmount ?: pending.extractedAmount,
                type = pending.extractedType,
                category = editedCategory ?: pending.suggestedCategory,
                date = pending.timestamp,
                merchantOrNote = editedMerchant ?: pending.extractedMerchant,
                isAutoParsed = true,
                smsSender = pending.sender
            )
            repository.approvePendingTransaction(pending.id, finalTransaction)
        }
    }

    fun rejectPending(pending: PendingTransaction) {
        viewModelScope.launch {
            repository.rejectPendingTransaction(pending.id)
        }
    }

    fun addCategoryBudget(name: String, limit: Double, iconName: String = "Category", colorHex: String = "#3B82F6") {
        viewModelScope.launch {
            repository.insertCategoryBudget(
                CategoryBudget(
                    categoryName = name.trim(),
                    monthlyLimit = limit,
                    iconName = iconName,
                    colorHex = colorHex
                )
            )
        }
    }

    fun deleteCategoryBudget(categoryName: String) {
        viewModelScope.launch {
            repository.deleteCategoryBudget(categoryName)
        }
    }

    // Payment Reminders & EMI Management
    fun addPaymentReminder(
        title: String,
        amount: Double,
        type: String = "EXPENSE",
        category: String = "Utilities",
        dueDayOfMonth: Int,
        notifyDaysBefore: Int = 3,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val reminder = PaymentReminder(
                title = title.trim(),
                amount = amount,
                type = type,
                category = category,
                dueDayOfMonth = dueDayOfMonth.coerceIn(1, 31),
                notifyDaysBefore = notifyDaysBefore.coerceIn(1, 7),
                notes = notes
            )
            repository.insertPaymentReminder(reminder)
        }
    }

    fun updatePaymentReminder(reminder: PaymentReminder) {
        viewModelScope.launch {
            repository.updatePaymentReminder(reminder)
        }
    }

    fun deletePaymentReminder(reminder: PaymentReminder) {
        viewModelScope.launch {
            repository.deletePaymentReminder(reminder)
        }
    }

    fun markReminderAsPaid(reminder: PaymentReminder) {
        viewModelScope.launch {
            val transaction = Transaction(
                amount = reminder.amount,
                type = reminder.type,
                category = reminder.category,
                merchantOrNote = "${reminder.title} (Bill Paid)",
                date = System.currentTimeMillis(),
                isAutoParsed = false
            )
            repository.insertTransaction(transaction)
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
        }
    }

    fun scanInboxSms(context: Context, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.scanInboxSms(context)
            onResult(count)
        }
    }

    fun simulateSmsParsing(rawSms: String, sender: String): Boolean {
        val parsed = SmsParser.parse(sender, rawSms)
        if (parsed != null) {
            viewModelScope.launch {
                val pending = PendingTransaction(
                    rawSms = parsed.rawSms,
                    sender = parsed.sender,
                    extractedAmount = parsed.amount,
                    extractedType = parsed.type,
                    suggestedCategory = parsed.category,
                    extractedMerchant = parsed.merchant,
                    timestamp = System.currentTimeMillis()
                )
                repository.insertPendingTransaction(pending)
            }
            return true
        }
        return false
    }

    private fun isCurrentMonth(timeMs: Long): Boolean {
        val calNow = Calendar.getInstance()
        val calItem = Calendar.getInstance().apply { timeInMillis = timeMs }
        return calNow.get(Calendar.YEAR) == calItem.get(Calendar.YEAR) &&
                calNow.get(Calendar.MONTH) == calItem.get(Calendar.MONTH)
    }
}

class ExpenseViewModelFactory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExpenseViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
