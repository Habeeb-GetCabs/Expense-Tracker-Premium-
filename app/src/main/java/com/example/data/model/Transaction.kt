package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val type: String, // "INCOME" or "EXPENSE"
    val category: String,
    val date: Long = System.currentTimeMillis(),
    val merchantOrNote: String,
    val isAutoParsed: Boolean = false,
    val smsSender: String? = null
)
