package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.TransactionType
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import java.text.NumberFormat
import java.util.Locale

private val vnFormat = NumberFormat.getNumberInstance(Locale("vi", "VN"))

fun formatVnd(amount: Double): String {
    return "${vnFormat.format(amount.toLong())} ₫"
}

fun parseColorSafe(hex: String, fallback: Color = Color(0xFF0A5C36)): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        fallback
    }
}

fun getIconForName(name: String): ImageVector {
    return when (name.lowercase(Locale.ROOT)) {
        "restaurant", "dining" -> Icons.Default.Restaurant
        "local_cafe", "cafe" -> Icons.Default.LocalCafe
        "shopping_cart", "cart" -> Icons.Default.ShoppingCart
        "local_gas_station", "gas" -> Icons.Default.LocalGasStation
        "shopping_bag" -> Icons.Default.ShoppingBag
        "home" -> Icons.Default.Home
        "movie" -> Icons.Default.Movie
        "medical_services" -> Icons.Default.MedicalServices
        "school" -> Icons.Default.School
        "card_giftcard" -> Icons.Default.CardGiftcard
        "payments" -> Icons.Default.Payments
        "redeem" -> Icons.Default.Redeem
        "storefront" -> Icons.Default.Storefront
        "trending_up" -> Icons.Default.TrendingUp
        "price_check" -> Icons.Default.PriceCheck
        "credit_card" -> Icons.Default.CreditCard
        "account_balance" -> Icons.Default.AccountBalance
        "phone_android" -> Icons.Default.PhoneAndroid
        "savings" -> Icons.Default.Savings
        "swap_horiz" -> Icons.Default.SwapHoriz
        else -> Icons.Default.AccountBalanceWallet
    }
}

@Composable
fun CategoryAvatar(
    iconName: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val bgColor = parseColorSafe(colorHex).copy(alpha = 0.15f)
    val iconColor = parseColorSafe(colorHex)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = getIconForName(iconName),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
fun TransactionTypeBadge(
    type: TransactionType,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg) = when (type) {
        TransactionType.EXPENSE -> Triple("Chi tiêu", ExpenseRed.copy(alpha = 0.12f), ExpenseRed)
        TransactionType.INCOME -> Triple("Thu nhập", IncomeGreen.copy(alpha = 0.12f), IncomeGreen)
        TransactionType.TRANSFER -> Triple("Chuyển tiền", TransferBlue.copy(alpha = 0.12f), TransferBlue)
        TransactionType.ADJUSTMENT -> Triple("Điều chỉnh", Color.Gray.copy(alpha = 0.2f), Color(0xFFD1D1D6))
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun TransactionAmountText(
    type: TransactionType,
    amount: Double,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val (prefix, color) = when (type) {
        TransactionType.EXPENSE -> Pair("- ", ExpenseRed)
        TransactionType.INCOME -> Pair("+ ", IncomeGreen)
        TransactionType.TRANSFER -> Pair("", TransferBlue)
        TransactionType.ADJUSTMENT -> Pair("", MaterialTheme.colorScheme.onSurface)
    }

    Text(
        text = "$prefix${formatVnd(amount)}",
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight
    )
}
