package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long,
    val amountLimit: Double,
    val monthYear: String, // Format "yyyy-MM" e.g., "2026-09"
    val alert70: Boolean = true,
    val alert90: Boolean = true,
    val alert100: Boolean = true
)
