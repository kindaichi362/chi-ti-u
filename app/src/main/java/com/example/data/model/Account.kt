package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType(val displayName: String, val iconEmoji: String) {
    CASH("Tiền mặt", "💵"),
    BANK("Ngân hàng", "🏦"),
    CREDIT_CARD("Thẻ tín dụng", "💳"),
    E_WALLET("Ví điện tử", "📱"),
    INVESTMENT("Đầu tư", "📈"),
    SAVINGS("Tiết kiệm", "💰"),
    RECEIVABLE("Khoản phải thu", "🤝"),
    PAYABLE("Khoản phải trả", "📋"),
    LOAN("Khoản vay", "📋")
}

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val type: AccountType,
    val balance: Double,
    val bankCode: String? = null, // VCB, TCB, MB, MOMO, etc.
    val bankId: String? = null,
    val accountNumberMasked: String? = null,
    val creditLimit: Double = 0.0,
    val statementDay: Int? = null,
    val dueDay: Int? = null,
    val colorHex: String = "#0A5C36",
    val iconName: String = "wallet",
    val currency: String = "VND",
    val isExcludedFromNetWorth: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
