package com.example.repository

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import com.example.data.local.CategoryBudgetDao
import com.example.data.local.PendingTransactionDao
import com.example.data.local.TransactionDao
import com.example.data.model.CategoryBudget
import com.example.data.model.PendingTransaction
import com.example.data.model.Transaction
import com.example.parser.SmsParser
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val pendingTransactionDao: PendingTransactionDao,
    private val categoryBudgetDao: CategoryBudgetDao
) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val allPendingTransactions: Flow<List<PendingTransaction>> = pendingTransactionDao.getAllPending()
    val allCategoryBudgets: Flow<List<CategoryBudget>> = categoryBudgetDao.getAllBudgets()

    suspend fun insertTransaction(transaction: Transaction): Long {
        return transactionDao.insertTransaction(transaction)
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
     * Populates initial default category budgets and realistic sample transactions
     */
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

        val cal = Calendar.getInstance()

        // Current month sample transactions
        val sampleTransactions = listOf(
            Transaction(
                amount = 75000.0,
                type = "INCOME",
                category = "Salary",
                date = cal.timeInMillis - (1 * 86400000L),
                merchantOrNote = "TechCorp Monthly Payroll",
                isAutoParsed = true,
                smsSender = "AD-HDFCBK"
            ),
            Transaction(
                amount = 3450.0,
                type = "EXPENSE",
                category = "Grocery",
                date = cal.timeInMillis - (2 * 86400000L),
                merchantOrNote = "Blinkit Supermarket",
                isAutoParsed = true,
                smsSender = "VM-BLINKT"
            ),
            Transaction(
                amount = 2100.0,
                type = "EXPENSE",
                category = "Medical",
                date = cal.timeInMillis - (3 * 86400000L),
                merchantOrNote = "Apollo Pharmacy Store",
                isAutoParsed = true,
                smsSender = "AX-APOLLO"
            ),
            Transaction(
                amount = 890.0,
                type = "EXPENSE",
                category = "Dining",
                date = cal.timeInMillis - (4 * 86400000L),
                merchantOrNote = "Swiggy Gourmet Order",
                isAutoParsed = true,
                smsSender = "JK-SWIGGY"
            ),
            Transaction(
                amount = 4500.0,
                type = "EXPENSE",
                category = "Shopping",
                date = cal.timeInMillis - (5 * 86400000L),
                merchantOrNote = "Amazon Fashion Order",
                isAutoParsed = false,
                smsSender = null
            ),
            Transaction(
                amount = 1250.0,
                type = "EXPENSE",
                category = "Transport",
                date = cal.timeInMillis - (6 * 86400000L),
                merchantOrNote = "HPCL Auto Fuel Station",
                isAutoParsed = true,
                smsSender = "BP-HPCL"
            ),
            Transaction(
                amount = 9500.0,
                type = "EXPENSE",
                category = "Hospital",
                date = cal.timeInMillis - (7 * 86400000L),
                merchantOrNote = "Fortis Health Diagnostic",
                isAutoParsed = false,
                smsSender = null
            ),
            Transaction(
                amount = 2300.0,
                type = "EXPENSE",
                category = "Utilities",
                date = cal.timeInMillis - (8 * 86400000L),
                merchantOrNote = "Electricity Bill Payment",
                isAutoParsed = true,
                smsSender = "AD-BESCOM"
            )
        )
        transactionDao.insertTransactions(sampleTransactions)

        // Sample pending SMS items for instant pending tab testing
        val samplePending = listOf(
            PendingTransaction(
                rawSms = "Rs 1,499.00 debited from A/C XX4921 at ZEPTO GROCERY on 12-Sep-26. Ref: 492104.",
                sender = "JM-ZEPTO",
                extractedAmount = 1499.0,
                extractedType = "EXPENSE",
                suggestedCategory = "Grocery",
                extractedMerchant = "Zepto Grocery",
                timestamp = System.currentTimeMillis() - 3600000L
            ),
            PendingTransaction(
                rawSms = "Rs 8,500.00 credited to A/C XX4921 from FREELANCE PAYMENT on 11-Sep-26.",
                sender = "AD-HDFCBK",
                extractedAmount = 8500.0,
                extractedType = "INCOME",
                suggestedCategory = "Salary",
                extractedMerchant = "Freelance Client",
                timestamp = System.currentTimeMillis() - 86400000L
            )
        )
        samplePending.forEach { pendingTransactionDao.insertPending(it) }
    }
}
