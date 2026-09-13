package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExpenseApplication : Application() {

    lateinit var repository: ExpenseRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val database = AppDatabase.getDatabase(this)
        repository = ExpenseRepository(
            transactionDao = database.transactionDao(),
            pendingTransactionDao = database.pendingTransactionDao(),
            categoryBudgetDao = database.categoryBudgetDao()
        )

        // Populate initial data asynchronously if empty
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentTransactions = repository.allTransactions.first()
                if (currentTransactions.isEmpty()) {
                    repository.populateDefaultCategoriesAndSampleData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
