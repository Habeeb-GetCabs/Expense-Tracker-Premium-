package com.example.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.CategoryBudgetDao
import com.example.data.local.PaymentReminderDao
import com.example.data.local.PendingTransactionDao
import com.example.data.local.TransactionDao
import com.example.data.model.CategoryBudget
import com.example.data.model.PaymentReminder
import com.example.data.model.PendingTransaction
import com.example.data.model.Transaction
import com.example.parser.SmsParser
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val pendingTransactionDao: PendingTransactionDao,
    private val categoryBudgetDao: CategoryBudgetDao,
    private val paymentReminderDao: PaymentReminderDao
) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allPendingTransactions: Flow<List<PendingTransaction>> = pendingTransactionDao.getAllPending()
    val allCategoryBudgets: Flow<List<CategoryBudget>> = categoryBudgetDao.getAllBudgets()
    val allPaymentReminders: Flow<List<PaymentReminder>> = paymentReminderDao.getAllReminders()

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun insertTransactions(transactions: List<Transaction>) {
        transactionDao.insertTransactions(transactions)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteAllTransactions() {
        transactionDao.deleteAllTransactions()
    }

    suspend fun insertPendingTransaction(pending: PendingTransaction): Long {
        return pendingTransactionDao.insertPending(pending)
    }

    suspend fun approvePendingTransaction(pendingId: Long, finalTransaction: Transaction) {
        transactionDao.insertTransaction(finalTransaction)
        pendingTransactionDao.deletePendingById(pendingId)
    }

    suspend fun rejectPendingTransaction(pendingId: Long) {
        pendingTransactionDao.deletePendingById(pendingId)
    }

    suspend fun deleteAllPending() {
        pendingTransactionDao.deleteAllPending()
    }

    suspend fun insertCategoryBudget(budget: CategoryBudget) {
        categoryBudgetDao.insertBudget(budget)
    }

    suspend fun deleteCategoryBudget(categoryName: String) {
        categoryBudgetDao.deleteBudgetByName(categoryName)
    }

    // Payment Reminders (EMI / Bills / Income)
    suspend fun insertPaymentReminder(reminder: PaymentReminder): Long {
        return paymentReminderDao.insertReminder(reminder)
    }

    suspend fun updatePaymentReminder(reminder: PaymentReminder) {
        paymentReminderDao.updateReminder(reminder)
    }

    suspend fun deletePaymentReminder(reminder: PaymentReminder) {
        paymentReminderDao.deleteReminder(reminder)
    }

    suspend fun clearAllTransactions() {
        transactionDao.deleteAllTransactions()
        pendingTransactionDao.deleteAllPending()
    }

    /**
     * Scans user's SMS inbox for bank/transaction SMS and converts them to Pending Transactions.
     */
    suspend fun scanInboxSms(context: Context): Int {
        var scannedCount = 0
        try {
            val cursor = context.contentResolver.query(
                Uri.parse("content://sms/inbox"),
                arrayOf("address", "body", "date"),
                null,
                null,
                "date DESC LIMIT 100"
            )

            cursor?.use {
                val addressIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val address = if (addressIdx != -1) it.getString(addressIdx) ?: "Unknown" else "Unknown"
                    val body = if (bodyIdx != -1) it.getString(bodyIdx) ?: "" else ""
                    val smsDate = if (dateIdx != -1) it.getLong(dateIdx) else System.currentTimeMillis()

                    val parsed = SmsParser.parse(address, body)
                    if (parsed != null) {
                        val pending = PendingTransaction(
                            rawSms = parsed.rawSms,
                            sender = parsed.sender,
                            extractedAmount = parsed.amount,
                            extractedType = parsed.type,
                            suggestedCategory = parsed.category,
                            extractedMerchant = parsed.merchant,
                            timestamp = smsDate
                        )
                        pendingTransactionDao.insertPending(pending)
                        scannedCount++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return scannedCount
    }

    /**
     * Populates default categories and sample reminders (with completely EMPTY transactions as requested)
     */
    suspend fun populateDefaultCategoriesAndSampleDataIfNeeded() {
        try {
            if (categoryBudgetDao.getBudgetCount() == 0) {
                populateDefaultCategoriesAndSampleData()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun populateDefaultCategoriesAndSampleData() {
        // Default categories & budgets
        val defaultBudgets = listOf(
            CategoryBudget("Grocery", 12000.0, "ShoppingCart", "#10B981"),
            CategoryBudget("Medical", 8000.0, "LocalHospital", "#EF4444"),
            CategoryBudget("Hospital", 15000.0, "Hospital", "#DC2626"),
            CategoryBudget("Dining", 6000.0, "Restaurant", "#F59E0B"),
            CategoryBudget("Shopping", 10000.0, "ShoppingBag", "#8B5CF6"),
            CategoryBudget("Transport", 5000.0, "DirectionsCar", "#3B82F6"),
            CategoryBudget("Utilities", 7000.0, "ElectricalServices", "#6366F1"),
            CategoryBudget("Salary", 0.0, "Payments", "#059669"),
            CategoryBudget("Entertainment", 4000.0, "Movie", "#EC4899"),
            CategoryBudget("Other", 5000.0, "Category", "#6B7280")
        )
        categoryBudgetDao.insertBudgets(defaultBudgets)

        // Sample Payment Reminder (e.g. Car Loan EMI)
        val defaultReminder = PaymentReminder(
            title = "Car Loan EMI",
            amount = 15000.0,
            type = "EXPENSE",
            category = "Transport",
            dueDayOfMonth = 10,
            notifyDaysBefore = 3,
            notes = "Auto debit from HDFC Bank Account"
        )
        paymentReminderDao.insertReminder(defaultReminder)

        // Note: Transactions and Pending items are intentionally kept completely empty!
    }
}
