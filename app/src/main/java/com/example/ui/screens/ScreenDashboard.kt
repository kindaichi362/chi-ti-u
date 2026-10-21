package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.FinancialAdvisorEngine
import com.example.data.model.AccountType
import com.example.data.model.PaymentStatus
import com.example.ui.components.AccountLogo
import com.example.ui.components.AppCard
import com.example.ui.components.IconTile
import com.example.ui.components.InsightCard
import com.example.ui.components.Label
import com.example.ui.components.ListRow
import com.example.ui.components.RowDivider
import com.example.ui.components.SectionHeader
import com.example.ui.components.ThresholdBar
import com.example.ui.components.TransactionRow
import com.example.ui.components.compactVnd
import com.example.ui.components.formatVnd
import com.example.ui.components.thresholdColor
import com.example.ui.theme.AppleSurface2
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.OledBlack
import com.example.ui.theme.Radius
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.theme.Tone
import com.example.ui.viewmodel.FinanceViewModel
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.roundToInt

private const val HIDDEN = "••••••"

/**
 * Home: answers "is this month OK?" first (what is left to spend), then what needs attention
 * (cards due, bills, pending QR), then where the money is, then the latest entries.
 */
@Composable
fun ScreenDashboard(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAiAssistant: (String?) -> Unit,
    onNavigateToPlans: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snapshot by viewModel.financialSnapshot.collectAsState()
    val insights by viewModel.insights.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val bills by viewModel.bills.collectAsState()
    var visible by rememberSaveable { mutableStateOf(true) }
    fun money(v: Double) = if (visible) formatVnd(v) else HIDDEN

    val categoryById = remember(categories) { categories.associateBy { it.id } }
    val accountById = remember(accounts) { accounts.associateBy { it.id } }
    val pendingQr = remember(transactions) { transactions.filter { it.paymentStatus == PaymentStatus.PENDING_PAYMENT } }
    val now = System.currentTimeMillis()
    val billsDueSoon = remember(bills) { bills.filter { it.nextDueDate - now <= 7 * 86_400_000L } }
    val cardsDueSoon = snapshot.creditCards.filter { it.debt > 0 && (it.daysToDue ?: 99) <= 7 }
    val monthLabel = remember { Calendar.getInstance().let { "Tháng ${it.get(Calendar.MONTH) + 1}, ${it.get(Calendar.YEAR)}" } }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(OledBlack),
        contentPadding = PaddingValues(start = Space.page, end = Space.page, top = Space.lg, bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.lg)
    ) {
        // 1. Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label(monthLabel)
                    Text("Tổng quan", fontSize = TextSize.xl, fontWeight = FontWeight.Bold, color = AppleTextPrimary)
                }
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (visible) "Ẩn số dư" else "Hiện số dư",
                        tint = AppleTextSecondary
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(Radius.xl)
                        .background(AppleSurface2)
                        .clickable { onNavigateToAiAssistant(null) }
                        .padding(horizontal = Space.md, vertical = Space.sm)
                        .testTag("ai_assistant_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Tone.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hỏi AI", fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = AppleTextPrimary)
                }
            }
        }

        // 2. Hero: what is left to spend this month (or what was spent, without budgets)
        item {
            AppCard(onClick = onNavigateToBudgets) {
                val hasBudget = snapshot.totalBudget > 0
                if (hasBudget) {
                    val left = snapshot.totalBudget - snapshot.totalBudgetSpent
                    val ratio = snapshot.totalBudgetSpent / snapshot.totalBudget
                    Label("Còn được tiêu tháng này")
                    Spacer(Modifier.height(Space.xs))
                    Text(money(left), fontSize = TextSize.hero, fontWeight = FontWeight.Bold,
                        color = if (left < 0) Tone.danger else AppleTextPrimary, maxLines = 1)
                    Spacer(Modifier.height(Space.md))
                    ThresholdBar(ratio)
                    Spacer(Modifier.height(Space.sm))
                    Row {
                        Text("Đã chi ${if (visible) compactVnd(snapshot.totalBudgetSpent) else HIDDEN} / ${if (visible) compactVnd(snapshot.totalBudget) else HIDDEN}",
                            fontSize = TextSize.xs, color = AppleTextSecondary, modifier = Modifier.weight(1f))
                        Text(
                            if (left > 0) "≈ ${if (visible) compactVnd(snapshot.dailyAllowance) else HIDDEN}/ngày · còn ${snapshot.daysLeft} ngày" else "Đã vượt ngân sách",
                            fontSize = TextSize.xs, color = if (left > 0) AppleTextSecondary else thresholdColor(ratio)
                        )
                    }
                } else {
                    Label("Đã chi tháng này")
                    Spacer(Modifier.height(Space.xs))
                    Text(money(snapshot.monthlyExpense), fontSize = TextSize.hero, fontWeight = FontWeight.Bold, color = AppleTextPrimary, maxLines = 1)
                    Spacer(Modifier.height(Space.xs))
                    Text("Đặt ngân sách để biết mỗi ngày còn được tiêu bao nhiêu ›", fontSize = TextSize.xs, color = Tone.accent)
                }
                Spacer(Modifier.height(Space.lg))
                RowDivider()
                Spacer(Modifier.height(Space.md))
                Row {
                    Column(Modifier.weight(1f)) {
                        Label("Thu")
                        Text(if (visible) compactVnd(snapshot.monthlyIncome) else HIDDEN, fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = Tone.income)
                    }
                    Column(Modifier.weight(1f)) {
                        Label("Chi")
                        Text(if (visible) compactVnd(snapshot.monthlyExpense) else HIDDEN, fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = AppleTextPrimary)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Label("So tháng trước")
                        val prev = snapshot.previousMonthExpense
                        if (prev > 0) {
                            val change = (snapshot.monthlyExpense - prev) / prev
                            Text(
                                "${if (change >= 0) "▲" else "▼"} ${(abs(change) * 100).roundToInt()}%",
                                fontSize = TextSize.base, fontWeight = FontWeight.SemiBold,
                                // Spending more than last month is the bad direction.
                                color = if (change > 0.05) Tone.warning else Tone.ok
                            )
                        } else {
                            Text("—", fontSize = TextSize.base, color = AppleTextSecondary)
                        }
                    }
                }
            }
        }

        // 3. Assistant findings
        if (insights.isNotEmpty()) {
            item {
                SectionHeader("Trợ lý nhận xét", action = "Hỏi thêm", onAction = { onNavigateToAiAssistant(null) })
                Spacer(Modifier.height(Space.sm))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    items(insights.take(4)) { insight ->
                        InsightCard(insight, modifier = Modifier.width(272.dp)) { onNavigateToAiAssistant(insight.followUp) }
                    }
                }
            }
        }

        // 4. Needs attention: card due dates, bills, pending QR payments
        if (cardsDueSoon.isNotEmpty() || billsDueSoon.isNotEmpty() || pendingQr.isNotEmpty()) {
            item {
                SectionHeader("Cần chú ý")
                Spacer(Modifier.height(Space.sm))
                AppCard(contentPadding = Space.lg) {
                    val rows = mutableListOf<@Composable () -> Unit>()
                    cardsDueSoon.forEach { card ->
                        rows += {
                            ListRow(
                                title = card.account.name.let { if (it.startsWith("Thẻ")) "Trả ${it.replaceFirstChar { c -> c.lowercase() }}" else "Trả thẻ $it" },
                                subtitle = if (card.daysToDue == 0) "Đến hạn hôm nay" else "Đến hạn sau ${card.daysToDue} ngày",
                                leading = { IconTile(Tone.danger, icon = Icons.Default.CreditCard) },
                                trailing = { Text(money(card.debt), fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = Tone.danger) },
                                onClick = onNavigateToAccounts
                            )
                        }
                    }
                    billsDueSoon.forEach { bill ->
                        val days = ((bill.nextDueDate - now) / 86_400_000L).toInt()
                        rows += {
                            ListRow(
                                title = bill.name,
                                subtitle = when { days < 0 -> "Quá hạn ${-days} ngày"; days == 0 -> "Đến hạn hôm nay"; else -> "Đến hạn sau $days ngày" },
                                leading = { IconTile(if (days < 0) Tone.danger else Tone.warning, icon = Icons.Default.Receipt) },
                                trailing = { Text(money(bill.amount), fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = AppleTextPrimary) },
                                onClick = onNavigateToPlans
                            )
                        }
                    }
                    if (pendingQr.isNotEmpty()) {
                        rows += {
                            ListRow(
                                title = "${pendingQr.size} khoản QR chờ xác nhận",
                                subtitle = "Xác nhận đã chuyển khoản để trừ vào số dư",
                                leading = { IconTile(Tone.warning, icon = Icons.Default.QrCodeScanner) },
                                trailing = { Text(money(pendingQr.sumOf { it.amount }), fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = Tone.warning) },
                                onClick = onNavigateToTransactions
                            )
                        }
                    }
                    rows.forEachIndexed { i, row -> if (i > 0) RowDivider(); row() }
                }
            }
        }

        // 5. Accounts
        item {
            SectionHeader("Tài khoản", action = "Tất cả", onAction = onNavigateToAccounts)
            Spacer(Modifier.height(Space.sm))
            AppCard(onClick = onNavigateToAccounts) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) {
                        Label("Tài sản ròng")
                        Text(money(snapshot.netWorth), fontSize = TextSize.figure, fontWeight = FontWeight.Bold, color = AppleTextPrimary, maxLines = 1)
                    }
                    if (snapshot.creditCardDebt > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Label("Nợ thẻ")
                            Text(if (visible) compactVnd(snapshot.creditCardDebt) else HIDDEN, fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = Tone.danger)
                        }
                    }
                }
            }
            Spacer(Modifier.height(Space.md))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                items(accounts, key = { it.id }) { acc ->
                    AppCard(modifier = Modifier.width(164.dp), onClick = onNavigateToAccounts, contentPadding = Space.md + Space.xs) {
                        AccountLogo(acc, size = 28.dp)
                        Spacer(Modifier.height(Space.md))
                        Text(acc.name, fontSize = TextSize.xs, color = AppleTextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (acc.type == AccountType.CREDIT_CARD) {
                            val debt = (-acc.balance).coerceAtLeast(0.0)
                            Text(if (visible) compactVnd(debt) else HIDDEN, fontSize = TextSize.base, fontWeight = FontWeight.SemiBold,
                                color = if (debt > 0) Tone.danger else AppleTextPrimary, maxLines = 1)
                            if (acc.creditLimit > 0) {
                                Spacer(Modifier.height(Space.xs + 2.dp))
                                ThresholdBar(debt / acc.creditLimit, height = 4.dp)
                            }
                        } else {
                            Text(if (visible) compactVnd(acc.balance) else HIDDEN, fontSize = TextSize.base, fontWeight = FontWeight.SemiBold,
                                color = if (acc.balance < 0) Tone.danger else AppleTextPrimary, maxLines = 1)
                        }
                    }
                }
            }
        }

        // 6. Recent transactions
        item {
            SectionHeader("Giao dịch gần đây", action = "Xem tất cả", onAction = onNavigateToTransactions)
            Spacer(Modifier.height(Space.sm))
            AppCard(contentPadding = Space.lg) {
                if (transactions.isEmpty()) {
                    com.example.ui.components.EmptyState(
                        title = "Chưa có giao dịch",
                        body = "Bấm ➕ để ghi, hoặc giữ ➕ rồi nói \"cà phê 35k\".",
                        actionLabel = "Ghi giao dịch đầu tiên",
                        onAction = onNavigateToAddTransaction
                    )
                } else {
                    transactions.take(5).forEachIndexed { i, tx ->
                        if (i > 0) RowDivider()
                        TransactionRow(
                            tx = tx,
                            category = categoryById[tx.categoryId],
                            account = accountById[tx.sourceAccountId],
                            destination = tx.destinationAccountId?.let { accountById[it] },
                            showDate = true,
                            onClick = onNavigateToTransactions
                        )
                    }
                }
            }
        }

        // 7. Plans
        item {
            val debts by viewModel.debts.collectAsState()
            AppCard(onClick = onNavigateToPlans, contentPadding = Space.md + Space.xs) {
                ListRow(
                    title = "Sổ nợ, mục tiêu & hóa đơn",
                    subtitle = "${debts.count { !it.isSettled }} khoản nợ đang mở · ${bills.size} hóa đơn định kỳ",
                    leading = { IconTile(Tone.transfer, icon = Icons.Default.EventNote) },
                    showChevron = true,
                    modifier = Modifier.testTag("card_plans")
                )
            }
        }
    }
}

