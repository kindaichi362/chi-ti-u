package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountType
import com.example.payment.vietqr.VietnameseBankRegistry
import com.example.ui.theme.AppleBorder

@Composable
fun BankLogoBadge(
    bankId: String?,
    bankCode: String? = null,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier
) {
    val bank = if (!bankId.isNullOrBlank()) {
        VietnameseBankRegistry.findById(bankId)
    } else if (!bankCode.isNullOrBlank()) {
        VietnameseBankRegistry.findByNameOrKeyword(bankCode)
    } else {
        null
    }

    val brandColor = bank?.let { parseColorSafe(it.brandColorHex) } ?: Color(0xFF005C2B)
    val shortCode = when {
        bank != null -> when (bank.id) {
            "vcb" -> "VCB"
            "tcb" -> "TCB"
            "mb" -> "MB"
            "bidv" -> "BIDV"
            "vpb" -> "VPB"
            "acb" -> "ACB"
            "vtb" -> "VTB"
            "vba" -> "VBA"
            "stb" -> "STB"
            "tpb" -> "TPB"
            "vib" -> "VIB"
            "ocb" -> "OCB"
            "shb" -> "SHB"
            "hdb" -> "HDB"
            "msb" -> "MSB"
            "momo" -> "MoMo"
            "zalopay" -> "Zalo"
            "cash" -> "💵"
            else -> bank.shortName.take(3).uppercase()
        }
        !bankCode.isNullOrBlank() -> bankCode.take(3).uppercase()
        else -> "🏦"
    }

    val fontSize = when {
        shortCode.length >= 4 -> 9.sp
        shortCode.length == 3 -> 10.sp
        else -> 12.sp
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(brandColor)
            .border(1.dp, AppleBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (shortCode == "💵" || shortCode == "🏦") {
            Text(text = shortCode, fontSize = (size.value * 0.45).sp)
        } else {
            Text(
                text = shortCode,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize,
                letterSpacing = (-0.5).sp
            )
        }
    }
}

@Composable
fun AccountTypeBadge(
    type: AccountType,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val (bgColor, iconVector, emoji) = when (type) {
        AccountType.CASH -> Triple(Color(0xFF10B981), Icons.Default.Payments, "💵")
        AccountType.BANK -> Triple(Color(0xFF005C2B), Icons.Default.AccountBalance, "🏦")
        AccountType.CREDIT_CARD -> Triple(Color(0xFF2563EB), Icons.Default.CreditCard, "💳")
        AccountType.E_WALLET -> Triple(Color(0xFFA50064), Icons.Default.PhoneAndroid, "📱")
        AccountType.INVESTMENT -> Triple(Color(0xFF7C3AED), Icons.Default.TrendingUp, "📈")
        AccountType.SAVINGS -> Triple(Color(0xFFF59E0B), Icons.Default.Savings, "💰")
        AccountType.RECEIVABLE -> Triple(Color(0xFF0EA5E9), Icons.Default.Handshake, "🤝")
        AccountType.PAYABLE, AccountType.LOAN -> Triple(Color(0xFFEF4444), Icons.Default.Receipt, "📋")
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor.copy(alpha = 0.15f))
            .border(1.dp, bgColor.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(text = emoji, fontSize = (size.value * 0.45).sp)
    }
}
