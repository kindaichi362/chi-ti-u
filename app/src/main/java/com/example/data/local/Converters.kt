package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import com.example.data.model.DebtType
import com.example.data.model.PaymentStatus
import com.example.data.model.TransactionType

class Converters {
    @TypeConverter
    fun fromAccountType(value: AccountType?): String? = value?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? = value?.let {
        try {
            AccountType.valueOf(it)
        } catch (e: Exception) {
            AccountType.BANK
        }
    }

    @TypeConverter
    fun fromCategoryType(value: CategoryType?): String? = value?.name

    @TypeConverter
    fun toCategoryType(value: String?): CategoryType? = value?.let {
        try {
            CategoryType.valueOf(it)
        } catch (e: Exception) {
            CategoryType.EXPENSE
        }
    }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let {
        try {
            TransactionType.valueOf(it)
        } catch (e: Exception) {
            TransactionType.EXPENSE
        }
    }

    @TypeConverter
    fun fromDebtType(value: DebtType?): String? = value?.name

    @TypeConverter
    fun toDebtType(value: String?): DebtType? = value?.let {
        try {
            DebtType.valueOf(it)
        } catch (e: Exception) {
            DebtType.I_OWE
        }
    }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus?): String? = value?.name

    @TypeConverter
    fun toPaymentStatus(value: String?): PaymentStatus? = value?.let {
        try {
            PaymentStatus.valueOf(it)
        } catch (e: Exception) {
            PaymentStatus.PAID
        }
    }
}
