package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PendingTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingTransactionDao {

    @Query("SELECT * FROM pending_transactions ORDER BY timestamp DESC")
    fun getAllPending(): Flow<List<PendingTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPending(pending: PendingTransaction): Long

    @Delete
    suspend fun deletePending(pending: PendingTransaction)

    @Query("DELETE FROM pending_transactions WHERE id = :id")
    suspend fun deletePendingById(id: Long)

    @Query("DELETE FROM pending_transactions")
    suspend fun deleteAllPending()
}
