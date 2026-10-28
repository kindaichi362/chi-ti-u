package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.AccountType
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.countsInReports
import com.example.ui.components.AccountLogo
import com.example.ui.components.ActionTile
import com.example.ui.components.AppCard
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.HeroCard
import com.example.ui.components.HeroMoney
import com.example.ui.components.MoneyText
import com.example.ui.components.RowDivider
import com.example.ui.components.SectionHeader
import com.example.ui.components.ThresholdBar
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatVnd
import com.example.ui.theme.AppPalette
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.viewmodel.FinanceViewModel
import kotlin.math.roundToInt

/**
 * One account: balance (or card debt and limit), quick actions, details and its full history.
 * Transfers in and out are shown with their direction.
 */
@Composable
fun ScreenAccountDetail(
    viewModel: FinanceViewModel,
    accountId: Long?,
    onBack: () -> Unit,
    onAddTransaction: (TransactionType, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = AppPalette
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val hidden by viewModel.balancesHidden.collectAsState()
    val account = accounts.firstOrNull { it.id == accountId }
    val history by remember(accountId) { viewModel.transactionsForAccount(accountId ?: -1) }.collectAsState(initial = emptyList())
    fun money(v: Double) = if (hidden) HIDDEN_AMOUNT else formatVnd(v)

    var actions by remember { mutableStateOf(false) }
    var reconciling by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf(false) }
    var cardSettings by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Transaction?>(null) }

    if (account == null) {
        // Archived or deleted from another screen: nothing to show here any more.
        Column(modifier.fillMaxSize().padding(Space.page)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = p.textPrimary) }
            EmptyState(title = "Không tìm thấy tài khoản", body = "Tài khoản này đã bị ẩn hoặc xóa.")
        }
        return
    }

    val isCard = account.type == AccountType.CREDIT_CARD
    val categoryById = remember(categories) { categories.associateBy { it.id } }
    val accountById = remember(accounts) { accounts.associateBy { it.id } }

    if (actions) {
        AccountActionsDialog(
            account = account,
            onDismiss = { actions = false },
            onReconcile = { reconciling = true; actions = false },
            onPayCard = { paying = true; actions = false },
            onEditCard = { cardSettings = true; actions = false },
            onArchive = {
                viewModel.archiveAccount(account.id)
                Toast.makeText(context, "Đã ẩn ${account.name}; lịch sử vẫn được giữ", Toast.LENGTH_SHORT).show()
                actions = false
                onBack()
            },
            onDelete = {
                viewModel.deleteAccountById(account.id) { deleted ->
                    Toast.makeText(
                        context,
                        if (deleted) "Đã xóa ${account.name}" else "\"${account.name}\" đã có giao dịch nên không xóa hẳn được. Hãy chọn \"Ngừng dùng\".",
                        Toast.LENGTH_LONG
                    ).show()
                    if (deleted) onBack()
                }
                actions = false
            }
        )
    }
    if (reconciling) {
        AmountDialog(
            title = "Đối soát: ${account.name}",
            hint = "Trong app: ${formatVnd(account.balance)}. Nhập số dư thật đang có (âm nếu là nợ).",
            label = "Số dư thực tế (₫)",
            allowNegative = true,
            initial = account.balance.toLong().toString(),
            onDismiss = { reconciling = false }
        ) { viewModel.reconcileAccountBalance(account.id, it); reconciling = false }
    }
    if (paying) {
        PayCardDialog(
            card = account,
            debt = (-account.balance).coerceAtLeast(0.0),
            sources = accounts.filter { it.id != account.id && it.type != AccountType.CREDIT_CARD },
            onDismiss = { paying = false }
        ) { from, amount -> viewModel.payCreditCard(account.id, from, amount); paying = false }
    }
    if (cardSettings) {
        CardSettingsDialog(card = account, onDismiss = { cardSettings = false }) { viewModel.updateAccount(it); cardSettings = false }
    }
    editing?.let { tx ->
        EditTransactionDialog(
            transaction = tx, accounts = accounts, categories = categories,
            onDismiss = { editing = null },
            onDelete = { viewModel.deleteTransaction(tx); editing = null },
            onSave = { viewModel.updateTransaction(it); editing = null }
        )
    }

    val monthKey = FinanceViewModel.monthKeyOf(System.currentTimeMillis())
    val thisMonth = history.filter { it.countsInReports && FinanceViewModel.monthKeyOf(it.timestamp) == monthKey }
    // Money in / out of THIS account this month, transfers included (they move this balance).
    val moneyIn = thisMonth.filter { it.type == TransactionType.INCOME || (it.type == TransactionType.TRANSFER && it.destinationAccountId == account.id) }.sumOf { it.amount }
    val moneyOut = thisMonth.filter { it.type == TransactionType.EXPENSE || (it.type == TransactionType.TRANSFER && it.sourceAccountId == account.id) }.sumOf { it.amount }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(p.background),
        contentPadding = PaddingValues(start = Space.page, end = Space.page, bottom = Space.xxl),
        verticalArrangement = Arrangement.spacedBy(Space.lg)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = Space.xs)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = p.textPrimary) }
                AccountLogo(account, size = 28.dp)
                Spacer(Modifier.width(Space.md))
                Text(account.name, fontSize = TextSize.lg, fontWeight = FontWeight.SemiBold, color = p.textPrimary,
                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                IconButton(onClick = { actions = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Thao tác", tint = p.textPrimary) }
            }
        }

        item {
            HeroCard {
                if (isCard) {
                    val debt = (-account.balance).coerceAtLeast(0.0)
                    Text("Dư nợ", fontSize = TextSize.sm, color = p.onHeroMuted)
                    HeroMoney(money(debt))
                    if (account.creditLimit > 0) {
                        Spacer(Modifier.height(Space.md))
                        ThresholdBar(debt / account.creditLimit, trackColor = p.onHero.copy(alpha = 0.18f), color = p.onHero)
                        Spacer(Modifier.height(Space.sm))
                        Text("Đã dùng ${(debt / account.creditLimit * 100).roundToInt()}% hạn mức ${formatVnd(account.creditLimit)}", fontSize = TextSize.xs, color = p.onHeroMuted)
                    }
                } else {
                    Text("Số dư", fontSize = TextSize.sm, color = p.onHeroMuted)
                    HeroMoney(money(account.balance))
                }
                Spacer(Modifier.height(Space.md))
                Box(Modifier.fillMaxWidth().height(1.dp).background(p.onHero.copy(alpha = 0.18f)))
                Spacer(Modifier.height(Space.md))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Vào tháng này", fontSize = TextSize.xs, color = p.onHeroMuted)
                        MoneyText(money(moneyIn), color = p.onHero)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Ra tháng này", fontSize = TextSize.xs, color = p.onHeroMuted)
                        MoneyText(money(moneyOut), color = p.onHero)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                ActionTile(Icons.Outlined.Add, "Ghi giao dịch", { onAddTransaction(TransactionType.EXPENSE, account.id) }, Modifier.weight(1f))
                if (isCard) ActionTile(Icons.Outlined.CreditCard, "Trả thẻ", { paying = true }, Modifier.weight(1f))
                else ActionTile(Icons.Outlined.SwapHoriz, "Chuyển tiền", { onAddTransaction(TransactionType.TRANSFER, account.id) }, Modifier.weight(1f))
            }
        }

        if (isCard) item {
            CardInstallmentSection(viewModel, account, accounts, hidden)
        }

        item {
            AppCard(contentPadding = Space.lg) {
                InfoRow("Loại", account.type.displayName)
                account.accountNumberMasked?.let { RowDivider(); InfoRow("Số tài khoản", it) }
                if (isCard) {
                    RowDivider(); InfoRow("Hạn mức", if (account.creditLimit > 0) formatVnd(account.creditLimit) else "Chưa nhập")
                    RowDivider(); InfoRow("Ngày sao kê", account.statementDay?.let { "Ngày $it hằng tháng" } ?: "Chưa nhập")
                    RowDivider(); InfoRow("Ngày đến hạn", account.dueDay?.let { "Ngày $it hằng tháng" } ?: "Chưa nhập")
                }
                RowDivider()
                Row(Modifier.fillMaxWidth().padding(vertical = Space.sm), horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                    ActionTile(Icons.Outlined.FactCheck, "Đối soát", { reconciling = true }, Modifier.weight(1f))
                    if (isCard) ActionTile(Icons.Outlined.CreditCard, "Cài đặt thẻ", { cardSettings = true }, Modifier.weight(1f))
                }
            }
        }

        item {
            SectionHeader("Lịch sử giao dịch")
            Spacer(Modifier.height(Space.xs))
            AppCard(contentPadding = Space.lg) {
                if (history.isEmpty()) {
                    EmptyState(icon = Icons.Outlined.ReceiptLong, title = "Chưa có giao dịch", body = "Các khoản thu, chi và chuyển tiền của tài khoản này sẽ hiện ở đây.")
                } else {
                    history.take(100).forEachIndexed { i, tx ->
                        if (i > 0) RowDivider()
                        TransactionRow(
                            tx = tx,
                            category = categoryById[tx.categoryId],
                            account = accountById[tx.sourceAccountId],
                            destination = tx.destinationAccountId?.let { accountById[it] },
                            showDate = true,
                            onClick = { if (tx.type != TransactionType.ADJUSTMENT) editing = tx }
                        )
                    }
                    if (history.size > 100) Text("Hiển thị 100 giao dịch gần nhất. Xem đủ ở tab Giao dịch (lọc theo tài khoản).", fontSize = TextSize.xs, color = p.textSecondary, modifier = Modifier.padding(top = Space.sm))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val p = AppPalette
    Row(Modifier.fillMaxWidth().padding(vertical = Space.md), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = TextSize.sm, color = p.textSecondary, modifier = Modifier.weight(1f))
        Text(value, fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = p.textPrimary)
    }
}
