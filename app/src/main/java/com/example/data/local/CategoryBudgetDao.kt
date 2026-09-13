package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryBudget
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {

    @Query("SELECT * FROM category_budgets")
    fun getAllBudgets(): Flow<List<CategoryBudget>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudget)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<CategoryBudget>)

    @Update
    suspend fun updateBudget(budget: CategoryBudget)

    @Query("DELETE FROM category_budgets WHERE categoryName = :name")
    suspend fun deleteBudgetByName(name: String)
}
