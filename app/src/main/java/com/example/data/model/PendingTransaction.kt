package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_transactions")
data class PendingTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawSms: String,
    val sender: String,
    val extractedAmount: Double,
    val extractedType: String, // "INCOME" or "EXPENSE"
    val suggestedCategory: String,
    val extractedMerchant: String,
    val timestamp: Long = System.currentTimeMillis()
)
