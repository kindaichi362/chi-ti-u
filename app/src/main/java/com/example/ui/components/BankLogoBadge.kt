package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.payment.vietqr.VietnameseBankRegistry

/** Official logos bundled in res/drawable-nodpi (source: VietQR's public bank list, by BIN). */
private val bankLogos: Map<String, Int> = mapOf(
    "vcb" to R.drawable.bank_vcb, "mb" to R.drawable.bank_mb, "tcb" to R.drawable.bank_tcb,
    "bidv" to R.drawable.bank_bidv, "vpb" to R.drawable.bank_vpb, "acb" to R.drawable.bank_acb,
    "vtb" to R.drawable.bank_vtb, "vba" to R.drawable.bank_vba, "stb" to R.drawable.bank_stb,
    "tpb" to R.drawable.bank_tpb, "vib" to R.drawable.bank_vib, "ocb" to R.drawable.bank_ocb,
    "shb" to R.drawable.bank_shb, "hdb" to R.drawable.bank_hdb, "msb" to R.drawable.bank_msb,
    "eib" to R.drawable.bank_eib, "seap" to R.drawable.bank_seap, "momo" to R.drawable.bank_momo,
    "viettelmoney" to R.drawable.bank_viettelmoney
)

/**
 * The logo of a bank or wallet. Bank logos are horizontal wordmarks, so the badge is a white
 * 3:2 tile (the way card and bank logos are shown in payment apps); [size] is its height.
 * Accounts without a known bank fall back to the icon of their account type.
 */
@Composable
fun BankLogoBadge(
    bankId: String?,
    bankCode: String? = null,
    size: Dp = 36.dp,
    modifier: Modifier = Modifier,
    accountType: AccountType? = null
) {
    val bank = when {
        !bankId.isNullOrBlank() -> VietnameseBankRegistry.findById(bankId)
        !bankCode.isNullOrBlank() -> VietnameseBankRegistry.findByNameOrKeyword(bankCode)
        else -> null
    }
    val logo = bank?.let { bankLogos[it.id] }
    val shape = RoundedCornerShape(if (size < 24.dp) 4.dp else 8.dp)

    when {
        logo != null -> Box(
            modifier = modifier
                .size(width = size * 1.5f, height = size)
                .clip(shape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(logo),
                contentDescription = bank.shortName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(horizontal = size * 0.06f)
            )
        }
        bank != null && bank.id != "cash" -> Box(
            // Known bank/wallet without a bundled logo (e.g. ZaloPay): brand colour + short name.
            modifier = modifier
                .size(width = size * 1.5f, height = size)
                .clip(shape)
                .background(parseColorSafe(bank.brandColorHex)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bank.shortName.take(7),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.28f).sp,
                maxLines = 1
            )
        }
        else -> AccountTypeBadge(
            type = accountType ?: if (bank?.id == "cash") AccountType.CASH else AccountType.BANK,
            size = size,
            modifier = modifier
        )
    }
}

/** Logo for an account: its bank's logo when known, otherwise its type icon. */
@Composable
fun AccountLogo(account: Account, size: Dp = 32.dp, modifier: Modifier = Modifier) {
    BankLogoBadge(bankId = account.bankId, bankCode = account.bankCode, size = size, modifier = modifier, accountType = account.type)
}

fun accountTypeVisual(type: AccountType): Pair<Color, ImageVector> = when (type) {
    AccountType.CASH -> Color(0xFF30D158) to Icons.Default.Payments
    AccountType.BANK -> Color(0xFF0A84FF) to Icons.Default.AccountBalance
    AccountType.CREDIT_CARD -> Color(0xFFBF5AF2) to Icons.Default.CreditCard
    AccountType.E_WALLET -> Color(0xFFFF375F) to Icons.Default.PhoneAndroid
    AccountType.INVESTMENT -> Color(0xFF64D2FF) to Icons.AutoMirrored.Filled.TrendingUp
    AccountType.SAVINGS -> Color(0xFFFF9F0A) to Icons.Default.Savings
    AccountType.RECEIVABLE -> Color(0xFF64D2FF) to Icons.Default.Handshake
    AccountType.PAYABLE, AccountType.LOAN -> Color(0xFFFF453A) to Icons.Default.Receipt
}

@Composable
fun AccountTypeBadge(
    type: AccountType,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier
) {
    val (tint, icon) = accountTypeVisual(type)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f))
            .border(1.dp, tint.copy(alpha = 0.3f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = type.displayName, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}
