package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.roundToLong

/** Repayment schedule for a purchase already charged to a credit card. */
@Entity(tableName = "card_installments", indices = [Index("cardId")])
data class CardInstallment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cardId: Long,
    val name: String,
    val totalAmount: Double,
    /** Actual monthly bank charge, including interest. Null for plans made before v1.6.5. */
    val monthlyAmount: Double? = null,
    val monthCount: Int,
    val paidMonths: Int = 0,
    val firstDueDate: Long,
    val createdAt: Long = System.currentTimeMillis()
)

fun CardInstallment.nextDueDate(): Long? {
    if (paidMonths >= monthCount) return null
    val base = Calendar.getInstance().apply { timeInMillis = firstDueDate }
    val day = base.get(Calendar.DAY_OF_MONTH)
    base.set(Calendar.DAY_OF_MONTH, 1)
    base.add(Calendar.MONTH, paidMonths)
    base.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(base.getActualMaximum(Calendar.DAY_OF_MONTH)))
    return base.timeInMillis
}

/** Whole VND per month; the last month receives any rounding remainder. */
fun CardInstallment.nextAmount(): Double {
    if (paidMonths >= monthCount || monthCount <= 0) return 0.0
    monthlyAmount?.let { return it }
    val total = totalAmount.roundToLong()
    val regular = total / monthCount
    return if (paidMonths == monthCount - 1) (total - regular * (monthCount - 1)).toDouble() else regular.toDouble()
}

/** Principal represented by the next payment. Existing no-interest plans keep their old rounding. */
fun CardInstallment.nextPrincipal(): Double {
    if (paidMonths >= monthCount || monthCount <= 0) return 0.0
    if (monthlyAmount == null) return nextAmount()
    val total = totalAmount.roundToLong()
    val regular = ceil(total.toDouble() / monthCount).toLong()
    return if (paidMonths == monthCount - 1) (total - regular * (monthCount - 1)).toDouble() else regular.toDouble()
}

fun CardInstallment.nextInterest(): Double = (nextAmount() - nextPrincipal()).coerceAtLeast(0.0)
