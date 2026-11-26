package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.data.model.TransactionType
import com.example.ui.components.AccountLogo
import com.example.ui.components.AppCard
import com.example.ui.components.EmptyState
import com.example.ui.components.HeroCard
import com.example.ui.components.HeroMoney
import com.example.ui.components.IconTile
import com.example.ui.components.ListRow
import com.example.ui.components.MoneyText
import com.example.ui.components.MonthPill
import com.example.ui.components.RowDivider
import com.example.ui.components.SectionHeader
import com.example.ui.components.ThresholdBar
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatVnd
import com.example.ui.theme.AppPalette
import com.example.ui.theme.Radius
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.viewmodel.FinanceViewModel
import kotlin.math.roundToInt

const val HIDDEN_AMOUNT = "•••••• ₫"

/**
 * Overview, in this order: header · budget (or total balance) card · income/expense this month ·
 * net worth with accounts · recent transactions · shortcut to debts, goals and bills.
 * Everything reflects the month chosen in the header.
 */
@Composable
fun ScreenDashboard(
    viewModel: FinanceViewModel,
    onNavigateToTransactions: () -> Unit,
    onOpenMonthlyFlow: (TransactionType, Int) -> Unit,
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onOpenAccount: (Long) -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToAiAssistant: (String?) -> Unit,
    onNavigateToPlans: () -> Unit,
    /** Opens the "Vay nợ" tab; a type also opens its add dialog (borrow / lend). */
    onNavigateToDebts: (DebtType?) -> Unit = {},
    dragonCard: @Composable () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val p = AppPalette
    val snapshot by viewModel.financialSnapshot.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val debts by viewModel.debts.collectAsState()
    val hidden by viewModel.balancesHidden.collectAsState()
    val monthOffset by viewModel.selectedMonthOffset.collectAsState()
    fun money(v: Double) = if (hidden) HIDDEN_AMOUNT else formatVnd(v)

    val categoryById = remember(categories) { categories.associateBy { it.id } }
    val accountById = remember(accounts) { accounts.associateBy { it.id } }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(p.background),
        contentPadding = PaddingValues(start = Space.page, end = Space.page, top = Space.md, bottom = Space.bottomBarClearance),  // room for the floating "Nói" button
        verticalArrangement = Arrangement.spacedBy(Space.lg)
    ) {
        // A. Header
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Tổng quan", fontSize = TextSize.xl, fontWeight = FontWeight.Bold, color = p.textPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                IconButton(onClick = { viewModel.toggleBalancesHidden() }) {
                    Icon(if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (hidden) "Hiện số dư" else "Ẩn số dư", tint = p.textSecondary)
                }
            }
            Spacer(Modifier.height(Space.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonthPill(selectedOffset = monthOffset, onSelect = viewModel::selectMonth)
                Spacer(Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .clip(Radius.md)
                        .background(p.accentSoft)
                        .clickable { onNavigateToAiAssistant(null) }
                        .heightIn(min = 36.dp)
                        .padding(horizontal = Space.md)
                        .testTag("ai_assistant_button"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = p.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hỏi AI", fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = p.accent)
                }
            }
        }

        // B. Budget card, or total balance when no budget is set
        item {
            val hasBudget = snapshot.totalBudget > 0
            HeroCard(onClick = onNavigateToBudgets) {
                val muted = p.onHeroMuted
                if (hasBudget) {
                    val left = snapshot.totalBudget - snapshot.totalBudgetSpent
                    val ratio = snapshot.totalBudgetSpent / snapshot.totalBudget
                    Text(if (left >= 0) "Ngân sách còn lại" else "Đã vượt ngân sách", fontSize = TextSize.sm, color = muted)
                    Spacer(Modifier.height(Space.xs))
                    HeroMoney(money(kotlin.math.abs(left)))
                    Spacer(Modifier.height(Space.md))
                    ThresholdBar(
                        ratio,
                        trackColor = LocalContentColor.current.copy(alpha = 0.18f),
                        color = if (ratio > 1) p.expenseSoft else if (ratio >= 0.8) p.warning else LocalContentColor.current
                    )
                    Spacer(Modifier.height(Space.sm))
                    Row {
                        Text("Đã chi ${money(snapshot.totalBudgetSpent)} / ${money(snapshot.totalBudget)}", fontSize = TextSize.xs, color = muted, modifier = Modifier.weight(1f), maxLines = 1)
                        Text("${(ratio * 100).roundToInt()}%", fontSize = TextSize.xs, fontWeight = FontWeight.SemiBold, color = if (ratio > 1) p.expenseSoft else muted)
                    }
                    if (monthOffset == 0 && left > 0) {
                        Spacer(Modifier.height(Space.xs))
                        Text("Mỗi ngày còn khoảng ${money(snapshot.dailyAllowance)} · còn ${snapshot.daysLeft} ngày", fontSize = TextSize.xs, color = muted)
                    }
                } else {
                    // No budget: show the money actually in the tracked accounts, never a made-up budget.
                    Text("Tổng số dư các tài khoản", fontSize = TextSize.sm, color = muted)
                    Spacer(Modifier.height(Space.xs))
                    HeroMoney(money(snapshot.totalAssets))
                    Spacer(Modifier.height(Space.md))
                    Row(
                        modifier = Modifier
                            .clip(Radius.md)
                            .background(LocalContentColor.current.copy(alpha = 0.14f))
                            .clickable(onClick = onNavigateToBudgets)
                            .heightIn(min = 40.dp)
                            .padding(horizontal = Space.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Thiết lập ngân sách", fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // C. Income and expense this month
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                FlowCard(
                    "Thu ${if (monthOffset == 0) "tháng này" else "trong tháng"}",
                    money(snapshot.monthlyIncome), up = true,
                    onClick = { onOpenMonthlyFlow(TransactionType.INCOME, monthOffset) },
                    modifier = Modifier.weight(1f).testTag("monthly_income_card")
                )
                FlowCard(
                    "Chi ${if (monthOffset == 0) "tháng này" else "trong tháng"}",
                    money(snapshot.monthlyExpense), up = false,
                    onClick = { onOpenMonthlyFlow(TransactionType.EXPENSE, monthOffset) },
                    modifier = Modifier.weight(1f).testTag("monthly_expense_card")
                )
            }
        }

        // Dragon Home: the pet that grows with the recording habit
        item { dragonCard() }

        // D. Net worth with a compact account list
        item {
            AppCard(contentPadding = Space.lg) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToAccounts),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Tài sản ròng", fontSize = TextSize.sm, color = p.textSecondary)
                        HeroMoney(money(snapshot.netWorth), color = p.textPrimary, base = TextSize.figure)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Xem tài khoản", tint = p.textTertiary)
                }
                if (accounts.isNotEmpty()) {
                    Spacer(Modifier.height(Space.sm))
                    accounts.take(4).forEach { acc ->
                        RowDivider()
                        val debt = acc.type == AccountType.CREDIT_CARD
                        ListRow(
                            title = acc.name,
                            subtitle = acc.accountNumberMasked,
                            leading = { AccountLogo(acc, size = 32.dp) },
                            trailing = {
                                MoneyText(money(acc.balance), color = if (acc.balance < 0 || debt && acc.balance < 0) p.expense else p.textPrimary)
                            },
                            showChevron = true,
                            onClick = { onOpenAccount(acc.id) }
                        )
                    }
                    if (accounts.size > 4) {
                        RowDivider()
                        Text(
                            "Xem tất cả ${accounts.size} tài khoản",
                            fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = p.accent,
                            modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigateToAccounts).heightIn(min = Space.touch).padding(top = Space.md)
                        )
                    }
                }
            }
        }

        // E. Recent transactions
        item {
            SectionHeader("Giao dịch gần đây", action = "Xem tất cả", onAction = onNavigateToTransactions)
            Spacer(Modifier.height(Space.xs))
            AppCard(contentPadding = Space.lg) {
                if (transactions.isEmpty()) {
                    EmptyState(
                        title = "Chưa có giao dịch",
                        body = "Bấm + để ghi khoản đầu tiên.",
                        actionLabel = "Thêm giao dịch",
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

        // E2. Loans: what I still owe and what others still owe me, with one-tap borrow / lend
        item { DebtSummaryCard(debts, { money(it) }, onNavigateToDebts) }

        // F. Shortcut to goals and bills
        item {
            AppCard(onClick = onNavigateToPlans, contentPadding = Space.md, color = p.accentSoft, borderColor = p.accentSoft) {
                ListRow(
                    title = "Mục tiêu & hóa đơn",
                    subtitle = "${bills.size} hóa đơn định kỳ",
                    leading = { IconTile(Icons.AutoMirrored.Outlined.EventNote, tint = p.accent, background = p.card) },
                    showChevron = true,
                    modifier = Modifier.testTag("card_plans")
                )
            }
        }
    }
}

@Composable
private fun DebtSummaryCard(debts: List<Debt>, money: (Double) -> String, onOpen: (DebtType?) -> Unit) {
    val p = AppPalette
    val open = debts.filter { !it.isSettled }
    val iOwe = open.filter { it.type == DebtType.I_OWE }.sumOf { it.remainingAmount }
    val owedToMe = open.filter { it.type == DebtType.OWED_TO_ME }.sumOf { it.remainingAmount }
    AppCard(onClick = { onOpen(null) }, contentPadding = Space.md, modifier = Modifier.testTag("card_debts")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Outlined.Handshake, tint = p.accent, background = p.accentSoft)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text("Vay nợ", fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                Text(if (open.isEmpty()) "Chưa có khoản nào đang mở" else "${open.size} khoản đang mở",
                    fontSize = TextSize.xs, color = p.textSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.textSecondary)
        }
        if (open.isNotEmpty()) {
            Spacer(Modifier.height(Space.sm))
            Row {
                Column(Modifier.weight(1f)) {
                    Text("Người khác nợ tôi", fontSize = TextSize.xs, color = p.textSecondary)
                    MoneyText(money(owedToMe), color = p.income)
                }
                Column(Modifier.weight(1f)) {
                    Text("Tôi đang nợ", fontSize = TextSize.xs, color = p.textSecondary)
                    MoneyText(money(iOwe), color = p.expense)
                }
            }
        }
        Spacer(Modifier.height(Space.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
            OutlinedButton(onClick = { onOpen(DebtType.OWED_TO_ME) }, modifier = Modifier.weight(1f).testTag("btn_lend")) { Text("Cho vay") }
            OutlinedButton(onClick = { onOpen(DebtType.I_OWE) }, modifier = Modifier.weight(1f).testTag("btn_borrow")) { Text("Vay tiền") }
        }
    }
}

@Composable
private fun FlowCard(label: String, amount: String, up: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = AppPalette
    AppCard(
        modifier = modifier.heightIn(min = 100.dp),
        onClick = onClick,
        contentPadding = Space.md,
        color = if (up) p.accentSoft else p.expenseSoft,
        borderColor = if (up) p.accentSoft else p.expenseSoft
    ) {
        // Icon and label on one line, the amount on its own full-width line so large sums fit at 360 dp.
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                if (up) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                tint = if (up) p.income else p.expense,
                background = if (up) p.accentSoft else p.expenseSoft,
                size = 28.dp,
                shape = CircleShape
            )
            Spacer(Modifier.width(Space.sm))
            Text(label, fontSize = TextSize.xs, color = p.textSecondary, maxLines = 1, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Xem giao dịch", tint = p.textSecondary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(Space.sm))
        HeroMoney(amount, color = if (up) p.income else p.expense, base = TextSize.lg)
    }
}
