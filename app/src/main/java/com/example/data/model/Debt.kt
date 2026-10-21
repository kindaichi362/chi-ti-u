package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DebtType(val displayName: String) {
    I_OWE("Tôi nợ"),
    OWED_TO_ME("Người khác nợ tôi")
}

@Entity(tableName = "debts")
data class Debt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: DebtType,
    val personName: String,
    val phone: String = "",
    val initialAmount: Double,
    val remainingAmount: Double,
    val dueDate: Long? = null,
    val note: String = "",
    val isSettled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
