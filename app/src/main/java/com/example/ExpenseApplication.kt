package com.example

import android.app.Application
import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ExpenseApplication : Application() {

    @Volatile
    private var _repository: ExpenseRepository? = null

    val repository: ExpenseRepository
        get() = getOrInitRepository(this)

    override fun onCreate() {
        super.onCreate()
        getOrInitRepository(this)
    }

    fun getOrInitRepository(context: Context): ExpenseRepository {
        return _repository ?: synchronized(this) {
            _repository ?: createRepository(context).also { _repository = it }
        }
    }

    private fun createRepository(context: Context): ExpenseRepository {
        val database = AppDatabase.getDatabase(context.applicationContext)
        val repo = ExpenseRepository(
            transactionDao = database.transactionDao(),
            pendingTransactionDao = database.pendingTransactionDao(),
            categoryBudgetDao = database.categoryBudgetDao(),
            paymentReminderDao = database.paymentReminderDao()
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentBudgets = repo.allCategoryBudgets.first()
                if (currentBudgets.isEmpty()) {
                    repo.populateDefaultCategoriesAndSampleData()
                }
            } catch (e: Exception) {
                Log.e("ExpenseApplication", "Error seeding default budget categories", e)
            }
        }
        return repo
    }
}

