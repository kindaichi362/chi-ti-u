package com.example.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType(val displayName: String) {
    EXPENSE("Chi tiêu"),
    INCOME("Thu nhập"),
    TRANSFER("Chuyển tiền"),
    ADJUSTMENT("Điều chỉnh")
}

enum class PaymentStatus(val displayName: String) {
    PAID("Đã thanh toán"),
    PENDING_PAYMENT("Chờ thanh toán"),
    CANCELLED("Đã hủy")
}

@Entity(
    tableName = "transactions",
    indices = [Index("timestamp"), Index("sourceAccountId"), Index("destinationAccountId"), Index("categoryId"), Index("debtId")]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val sourceAccountId: Long,
    val destinationAccountId: Long? = null, // Used for TRANSFER
    val categoryId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val payeeOrPayer: String = "",
    val isSplit: Boolean = false,
    val paymentStatus: PaymentStatus = PaymentStatus.PAID,
    val qrBankBin: String? = null,
    val qrAccountNumber: String? = null,
    val qrBeneficiaryName: String? = null,
    val qrContent: String? = null,
    val debtId: Long? = null
) {
    @get:Ignore
    val date: Long get() = timestamp
}

/**
 * True when the transaction really moved money and belongs in income/expense reports:
 * pending or cancelled QR payments are excluded.
 */
val Transaction.countsInReports: Boolean
    get() = paymentStatus == PaymentStatus.PAID && debtId == null

val Transaction.isDebtPrincipal: Boolean get() = debtId != null
