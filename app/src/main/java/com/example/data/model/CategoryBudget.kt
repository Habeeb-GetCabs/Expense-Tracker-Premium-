package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_budgets")
data class CategoryBudget(
    @PrimaryKey
    val categoryName: String,
    val monthlyLimit: Double,
    val iconName: String = "Category",
    val colorHex: String = "#3B82F6"
)
