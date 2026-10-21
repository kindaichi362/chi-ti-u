package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_bills")
data class RecurringBill(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val accountId: Long,
    val categoryId: Long,
    val dayOfMonth: Int, // 1 to 31
    val nextDueDate: Long,
    val note: String = "",
    val isActive: Boolean = true
)
