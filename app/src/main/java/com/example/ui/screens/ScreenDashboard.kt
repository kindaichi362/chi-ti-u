package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.PaymentStatus
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.BankLogoBadge
import com.example.ui.components.CategoryAvatar
import com.example.ui.components.TransactionAmountText
import com.example.ui.components.TransactionTypeBadge
import com.example.ui.components.VoiceTransactionBottomSheet
import com.example.ui.components.formatVnd
import com.example.ui.components.getIconForName
import com.example.ui.components.parseColorSafe
import com.example.ui.screens.paytrack.PayTrackBottomSheet
import com.example.ui.theme.AppleBorder
import com.example.ui.theme.AppleBorderSubtle
import com.example.ui.theme.AppleCyan
import com.example.ui.theme.AppleGreen
import com.example.ui.theme.AppleOrange
import com.example.ui.theme.AppleRed
import com.example.ui.theme.AppleSurface1
import com.example.ui.theme.AppleSurface2
import com.example.ui.theme.AppleSurface3
import com.example.ui.theme.AppleSurface4
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.AppleTextTertiary
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.OledBlack
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScreenDashboard(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAiAssistant: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snapshot by viewModel.financialSnapshot.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val bills by viewModel.bills.collectAsState()

    var isBalanceVisible by remember { mutableStateOf(true) }
    var isNetWorthExpanded by remember { mutableStateOf(false) }
    var showVoiceBottomSheet by remember { mutableStateOf(false) }
    var showPayTrackSheet by remember { mutableStateOf(false) }

    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greetingText = remember(currentHour) {
        when {
            currentHour < 12 -> "Chào buổi sáng"
            currentHour < 18 -> "Chào buổi chiều"
            else -> "Good Evening" // Inspired by the Apple Watch companion UI
        }
    }

    val pendingQrList = remember(transactions) {
        transactions.filter { it.paymentStatus == PaymentStatus.PENDING_PAYMENT }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(OledBlack)
                .padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Apple Hero Header
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = greetingText,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleTextPrimary,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Chi tiêu Thành Phát • Sổ tay tài chính",
                                fontSize = 14.sp,
                                color = AppleTextSecondary
                            )
                        }

                        // Apple Pill AI Button
                        Surface(
                            onClick = onNavigateToAiAssistant,
                            shape = CircleShape,
                            color = AppleSurface2,
                            border = BorderStroke(1.dp, AppleBorder),
                            modifier = Modifier.testTag("ai_assistant_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Trợ lý AI",
                                    tint = AppleOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Hỏi AI",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = AppleTextPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Apple Orange Info Link & Category Management link
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateToAnalytics() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = AppleOrange,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Báo cáo dòng tiền",
                                color = AppleOrange,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onNavigateToCategories() }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🍜 Quản lý danh mục ➔",
                                color = AppleTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Gợi ý 6 danh mục hay dùng (Ghi chép nhanh 1 chạm)
            item {
                val top6Categories = remember(categories) {
                    val exp = categories.filter { it.type == com.example.data.model.CategoryType.EXPENSE }
                    if (exp.size >= 6) exp.take(6) else categories.take(6)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                    border = BorderStroke(1.dp, AppleBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⚡ GỢI Ý 6 DANH MỤC HAY DÙNG",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleTextSecondary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Xem tất cả ➔",
                                color = AppleOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onNavigateToCategories() }
                                    .padding(vertical = 2.dp, horizontal = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Row 1: 3 categories
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            top6Categories.take(3).forEach { cat ->
                                Surface(
                                    onClick = onNavigateToAddTransaction,
                                    shape = RoundedCornerShape(12.dp),
                                    color = AppleSurface2,
                                    border = BorderStroke(1.dp, AppleBorderSubtle),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = cat.emoji ?: "🍜", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = cat.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = AppleTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        if (top6Categories.size > 3) {
                            Spacer(modifier = Modifier.height(8.dp))
                            // Row 2: 3 categories
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                top6Categories.drop(3).take(3).forEach { cat ->
                                    Surface(
                                        onClick = onNavigateToAddTransaction,
                                        shape = RoundedCornerShape(12.dp),
                                        color = AppleSurface2,
                                        border = BorderStroke(1.dp, AppleBorderSubtle),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(text = cat.emoji ?: "🍜", fontSize = 20.sp)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = cat.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AppleTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Net Worth Card (Tài sản ròng)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("net_worth_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                border = BorderStroke(1.dp, AppleBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TÀI SẢN RÒNG",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = AppleTextSecondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = { isBalanceVisible = !isBalanceVisible },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Ẩn/Hiện số dư",
                                    tint = AppleTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { isNetWorthExpanded = !isNetWorthExpanded },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isNetWorthExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Mở rộng tài sản",
                                tint = AppleTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isBalanceVisible) formatVnd(snapshot.netWorth) else "•••••••• ₫",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AppleTextPrimary,
                        letterSpacing = (-0.5).sp
                    )

                    AnimatedVisibility(visible = isNetWorthExpanded) {
                        Column(modifier = Modifier.padding(top = 14.dp)) {
                            HorizontalDivider(
                                color = AppleBorder,
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tổng tài sản có:",
                                    fontSize = 13.sp,
                                    color = AppleTextSecondary
                                )
                                Text(
                                    text = if (isBalanceVisible) formatVnd(snapshot.totalAssets) else "••••••",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleGreen
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Nợ & Thẻ tín dụng:",
                                    fontSize = 13.sp,
                                    color = AppleTextSecondary
                                )
                                Text(
                                    text = if (isBalanceVisible) "- ${formatVnd(snapshot.totalLiabilities)}" else "••••••",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppleRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Monthly Cash Flow Summary (Dòng tiền tháng)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToAnalytics() }
                    .testTag("cashflow_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                border = BorderStroke(1.dp, AppleBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale("vi", "VN")).format(Date())
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Dòng tiền ${currentMonthName.replaceFirstChar { it.titlecase() }}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AppleTextPrimary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Xem báo cáo",
                                tint = AppleOrange,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Surface(
                            color = AppleSurface3,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, AppleBorder)
                        ) {
                            Text(
                                text = "Tiết kiệm ${String.format("%.0f", snapshot.savingsRate)}%",
                                color = AppleGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Thu nhập
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = AppleGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Thu nhập",
                                    fontSize = 12.sp,
                                    color = AppleTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBalanceVisible) "+${formatVnd(snapshot.monthlyIncome)}" else "••••",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleGreen
                            )
                        }

                        // Chi tiêu
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = AppleRed,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Chi tiêu",
                                    fontSize = 12.sp,
                                    color = AppleTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBalanceVisible) "-${formatVnd(snapshot.monthlyExpense)}" else "••••",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleRed
                            )
                        }

                        // Còn lại
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = AppleOrange,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Còn lại",
                                    fontSize = 12.sp,
                                    color = AppleTextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isBalanceVisible) formatVnd(snapshot.monthlySavings) else "••••",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleOrange
                            )
                        }
                    }
                }
            }
        }

        // 5. Accounts Horizontal Carousel
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tài khoản & Ví (${accounts.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = AppleTextPrimary
                    )
                    Text(
                        text = "Xem tất cả",
                        color = AppleOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable { onNavigateToAccounts() }
                            .padding(4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    for (acc in accounts) {
                        AccountSummaryCard(
                            account = acc,
                            isVisible = isBalanceVisible,
                            onClick = onNavigateToAccounts
                        )
                    }
                }
            }
        }

        // 6. Budget Warning Banner
        if (snapshot.exceededBudgets.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToBudgets() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                    border = BorderStroke(1.dp, AppleRed.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppleRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = AppleRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Có ${snapshot.exceededBudgets.size} khoản vượt ngân sách!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AppleRed
                            )
                            Text(
                                text = snapshot.exceededBudgets.first().let { "${it.first}: Vượt ${formatVnd(it.second)}" },
                                fontSize = 12.sp,
                                color = AppleTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = AppleRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 7. Upcoming Bills
        if (bills.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                    border = BorderStroke(1.dp, AppleBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppleOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AppleOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hóa đơn định kỳ sắp đến",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = AppleTextPrimary
                            )
                            val firstBill = bills.first()
                            Text(
                                text = "${firstBill.name}: ${formatVnd(firstBill.amount)} (ngày ${firstBill.dayOfMonth})",
                                fontSize = 12.sp,
                                color = AppleTextSecondary
                            )
                        }
                    }
                }
            }
        }

        // 7.5. Pending QR Payments Banner
        if (pendingQrList.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToTransactions() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                    border = BorderStroke(1.dp, AppleOrange.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(AppleOrange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                tint = AppleOrange,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Có ${pendingQrList.size} khoản chờ thanh toán qua QR",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = AppleOrange
                            )
                            val firstPending = pendingQrList.first()
                            Text(
                                text = "${firstPending.note.ifBlank { "Giao dịch QR" }}: ${formatVnd(firstPending.amount)}",
                                fontSize = 12.sp,
                                color = AppleTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = AppleOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 8. Recent Transactions Header & List
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Giao dịch gần đây",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = AppleTextPrimary
                )
                Text(
                    text = "Xem tất cả",
                    color = AppleOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onNavigateToTransactions() }
                        .padding(4.dp)
                )
            }
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AppleSurface1),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AppleBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Chưa có giao dịch nào",
                            fontWeight = FontWeight.SemiBold,
                            color = AppleTextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            onClick = onNavigateToAddTransaction,
                            shape = CircleShape,
                            color = AppleSurface2,
                            border = BorderStroke(1.dp, AppleBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = AppleOrange)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ghi giao dịch đầu tiên", color = AppleTextPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        } else {
            items(transactions.take(6)) { tx ->
                val category = categories.firstOrNull { it.id == tx.categoryId }
                val account = accounts.firstOrNull { it.id == tx.sourceAccountId }

                TransactionDashboardItem(
                    transaction = tx,
                    categoryName = category?.name ?: if (tx.type == TransactionType.TRANSFER) "Chuyển tiền" else "Khác",
                    categoryEmoji = category?.emoji,
                    categoryIcon = category?.iconName ?: if (tx.type == TransactionType.TRANSFER) "swap_horiz" else "wallet",
                    categoryColor = category?.colorHex ?: "#FF9F0A",
                    account = account,
                    onClick = onNavigateToTransactions
                )
            }
        }
    }

    if (showVoiceBottomSheet) {
        VoiceTransactionBottomSheet(
            viewModel = viewModel,
            onDismiss = { showVoiceBottomSheet = false },
            onTransactionSaved = { showVoiceBottomSheet = false },
            autoStart = true
        )
    }

    if (showPayTrackSheet) {
        val defaultAcc = accounts.firstOrNull()
        val defaultCat = categories.firstOrNull { it.type == com.example.data.model.CategoryType.EXPENSE }
        PayTrackBottomSheet(
            initialAmount = 50000.0,
            initialNote = "Ăn sáng",
            selectedCategory = defaultCat,
            selectedAccount = defaultAcc,
            accounts = accounts,
            onDismiss = { showPayTrackSheet = false },
            onPaymentConfirmed = { amount, noteText, catId, accId, qr ->
                viewModel.addTransaction(
                    type = com.example.data.model.TransactionType.EXPENSE,
                    amount = amount,
                    sourceAccountId = accId,
                    categoryId = catId,
                    note = noteText,
                    paymentStatus = com.example.data.model.PaymentStatus.PAID,
                    qrBankBin = qr.bankBin,
                    qrAccountNumber = qr.accountNumber,
                    qrBeneficiaryName = qr.beneficiaryName,
                    qrContent = qr.rawPayload
                )
                showPayTrackSheet = false
            },
            onPaymentPendingSaved = { amount, noteText, catId, accId, qr ->
                viewModel.createPendingQrPayment(
                    amount = amount,
                    sourceAccountId = accId,
                    categoryId = catId,
                    note = noteText,
                    bankBin = qr.bankBin,
                    accountNumber = qr.accountNumber,
                    beneficiaryName = qr.beneficiaryName,
                    qrContent = qr.rawPayload
                )
                showPayTrackSheet = false
            }
        )
    }
    } // End of Box
}

@Composable
fun AccountSummaryCard(
    account: Account,
    isVisible: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(156.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppleSurface1),
        border = BorderStroke(1.dp, AppleBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                BankLogoBadge(
                    bankId = account.bankId,
                    bankCode = account.bankCode,
                    size = 32.dp
                )

                if (account.accountNumberMasked != null) {
                    Text(
                        text = account.accountNumberMasked,
                        fontSize = 10.sp,
                        color = AppleTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = account.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = AppleTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isVisible) formatVnd(account.balance) else "•••••• ₫",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (account.balance < 0) AppleRed else AppleTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TransactionDashboardItem(
    transaction: Transaction,
    categoryName: String,
    categoryEmoji: String?,
    categoryIcon: String,
    categoryColor: String,
    account: Account?,
    onClick: () -> Unit
) {
    val cal = Calendar.getInstance()
    val todayDay = cal.get(Calendar.DAY_OF_YEAR)
    val todayYear = cal.get(Calendar.YEAR)

    cal.timeInMillis = transaction.date
    val txDay = cal.get(Calendar.DAY_OF_YEAR)
    val txYear = cal.get(Calendar.YEAR)

    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(transaction.date))
    val timeLabel = when {
        todayYear == txYear && todayDay == txDay -> "Hôm nay, $timeStr"
        todayYear == txYear && todayDay - txDay == 1 -> "Hôm qua, $timeStr"
        else -> SimpleDateFormat("dd/MM, HH:mm", Locale.getDefault()).format(Date(transaction.date))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("transaction_timeline_item"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AppleSurface1),
        border = BorderStroke(1.dp, AppleBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon/Emoji Box
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(parseColorSafe(categoryColor).copy(alpha = 0.16f))
                    .border(1.dp, parseColorSafe(categoryColor).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!categoryEmoji.isNullOrBlank()) {
                    Text(text = categoryEmoji, fontSize = 20.sp)
                } else {
                    Icon(
                        imageVector = getIconForName(categoryIcon),
                        contentDescription = null,
                        tint = parseColorSafe(categoryColor),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (transaction.note.isNotBlank()) transaction.note else categoryName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = AppleTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = timeLabel,
                        fontSize = 12.sp,
                        color = AppleTextSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "•", fontSize = 10.sp, color = AppleTextSecondary)
                    Spacer(modifier = Modifier.width(6.dp))
                    if (account != null) {
                        BankLogoBadge(bankId = account.bankId, bankCode = account.bankCode, size = 18.dp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = account.name,
                            fontSize = 12.sp,
                            color = AppleTextSecondary
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                val amountPrefix = when (transaction.type) {
                    TransactionType.EXPENSE -> "-"
                    TransactionType.INCOME -> "+"
                    TransactionType.TRANSFER -> "⇄ "
                    else -> ""
                }
                val amountColor = when (transaction.type) {
                    TransactionType.EXPENSE -> AppleRed
                    TransactionType.INCOME -> AppleGreen
                    TransactionType.TRANSFER -> AppleCyan
                    else -> AppleTextPrimary
                }

                Text(
                    text = "$amountPrefix${formatVnd(transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = amountColor
                )

                if (transaction.paymentStatus == PaymentStatus.PENDING_PAYMENT) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Chờ QR",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppleOrange
                    )
                }
            }
        }
    }
}
