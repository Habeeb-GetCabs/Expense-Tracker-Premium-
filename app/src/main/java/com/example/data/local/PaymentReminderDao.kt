package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PaymentReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentReminderDao {
    @Query("SELECT * FROM payment_reminders ORDER BY dueDayOfMonth ASC")
    fun getAllReminders(): Flow<List<PaymentReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: PaymentReminder): Long

    @Update
    suspend fun updateReminder(reminder: PaymentReminder)

    @Delete
    suspend fun deleteReminder(reminder: PaymentReminder)

    @Query("DELETE FROM payment_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: Long)
}
