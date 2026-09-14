package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "payment_reminders")
data class PaymentReminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String = "EXPENSE", // "EXPENSE" or "INCOME"
    val category: String = "Utilities",
    val dueDayOfMonth: Int, // e.g. 10 for 10th of every month
    val notifyDaysBefore: Int = 3, // Default 3 days before (e.g. 7, 8, 9)
    val isAutoPaid: Boolean = false,
    val notes: String = ""
)
