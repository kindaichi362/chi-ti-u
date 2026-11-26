package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ai.NaturalLanguageSearchEngine
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.model.countsInReports
import com.example.ui.components.AccountLogo
import com.example.ui.components.AppCard
import com.example.ui.components.ChoiceChip
import com.example.ui.components.EditTransactionDialog
import com.example.ui.components.EmptyState
import com.example.ui.components.IconTile
import com.example.ui.components.Label
import com.example.ui.components.MINUS
import com.example.ui.components.MoneyText
import com.example.ui.components.RowDivider
import com.example.ui.components.TransactionRow
import com.example.ui.components.formatVnd
import com.example.ui.components.monthLabel
import com.example.ui.theme.AppPalette
import com.example.ui.theme.Radius
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun monthRange(offset: Int): LongRange {
    val start = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.MONTH, offset)
    }
    val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
    return start.timeInMillis until end.timeInMillis
}

private val NATURAL_HINTS = listOf("bao nhiêu", "tháng trước", "hôm nay", "hôm qua", "trên ", "dưới ", "tiêu gì", "tuần")

/**
 * Transactions: search, type chips, month (or all time), totals for exactly what is listed,
 * then one card per day. Filters live in the ViewModel so they survive switching tabs.
 */
@Composable
fun ScreenTransactions(
    viewModel: FinanceViewModel,
    onNavigateToAddTransaction: () -> Unit,
    onVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = AppPalette
    val transactions by viewModel.transactions.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val filter by viewModel.txFilter.collectAsState()

    var editing by remember { mutableStateOf<Transaction?>(null) }
    var deleting by remember { mutableStateOf<Transaction?>(null) }
    var showAccountFilter by remember { mutableStateOf(false) }
    var monthMenu by remember { mutableStateOf(false) }

    val categoryById = remember(categories) { categories.associateBy { it.id } }
    val accountById = remember(accounts) { accounts.associateBy { it.id } }

    // A question ("tháng này ăn uống bao nhiêu") is answered by the search engine; plain words filter.
    val natural = remember(filter.query, transactions, accounts, categories) {
        val q = filter.query.lowercase(Locale.ROOT)
        if (q.isNotBlank() && NATURAL_HINTS.any { q.contains(it) }) NaturalLanguageSearchEngine.executeQuery(filter.query, transactions, accounts, categories) else null
    }

    val listed = remember(filter, transactions, categoryById, accountById, natural) {
        if (natural != null) natural.filteredTransactions
        else {
            val range = filter.monthOffset?.let(::monthRange)
            transactions.filter { tx ->
                (range == null || tx.timestamp in range) &&
                    (!filter.settledOnly || tx.paymentStatus == com.example.data.model.PaymentStatus.PAID) &&
                    (filter.type == null || tx.type == filter.type) &&
                    (filter.accountId == null || tx.sourceAccountId == filter.accountId || tx.destinationAccountId == filter.accountId) &&
                    (filter.query.isBlank() || listOfNotNull(tx.note, categoryById[tx.categoryId]?.name, accountById[tx.sourceAccountId]?.name)
                        .any { it.contains(filter.query.trim(), ignoreCase = true) })
            }
        }
    }
    // Totals of exactly the listed rows; transfers and pending/cancelled payments never count.
    val totalIn = listed.filter { it.type == TransactionType.INCOME && it.countsInReports }.sumOf { it.amount }
    val totalOut = listed.filter { it.type == TransactionType.EXPENSE && it.countsInReports }.sumOf { it.amount }

    val grouped = remember(listed) {
        val dayFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val longFmt = SimpleDateFormat("d 'tháng' M, yyyy", Locale.getDefault())
        val today = dayFmt.format(Date())
        val yesterday = dayFmt.format(Date(System.currentTimeMillis() - 86_400_000L))
        listed.groupBy { dayFmt.format(Date(it.timestamp)) }.map { (day, txs) ->
            val label = when (day) { today -> "Hôm nay"; yesterday -> "Hôm qua"; else -> longFmt.format(Date(txs.first().timestamp)) }
            label to txs
        }
    }

    editing?.takeIf { it.debtId == null }?.let { tx ->
        EditTransactionDialog(
            transaction = tx,
            accounts = accounts,
            categories = categories,
            onDismiss = { editing = null },
            onDelete = { deleting = tx; editing = null },
            onSave = { viewModel.updateTransaction(it); editing = null }
        )
    }
    deleting?.let { tx ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = p.card,
            title = { Text("Xóa giao dịch?", fontWeight = FontWeight.Bold, color = p.textPrimary) },
            text = { Text(if (tx.debtId != null) "Đây là giao dịch của một khoản vay nợ. Số dư tài khoản được hoàn lại; nếu là lần trả/thu nợ thì số còn nợ được cộng lại. Muốn sửa, vào Vay nợ & kế hoạch." else "Số dư tài khoản liên quan sẽ được hoàn lại tương ứng.", color = p.textSecondary) },
            confirmButton = { TextButton(onClick = { viewModel.deleteTransaction(tx); deleting = null }) { Text("Xóa", color = p.danger, fontWeight = FontWeight.SemiBold) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Hủy", color = p.textSecondary) } }
        )
    }
    if (showAccountFilter) {
        AlertDialog(
            onDismissRequest = { showAccountFilter = false },
            containerColor = p.card,
            title = { Text("Lọc theo tài khoản", fontWeight = FontWeight.Bold, color = p.textPrimary) },
            text = {
                Column {
                    FilterOption("Tất cả tài khoản", selected = filter.accountId == null) {
                        viewModel.updateTxFilter { it.copy(accountId = null) }; showAccountFilter = false
                    }
                    accounts.forEach { acc ->
                        FilterOption(acc.name, selected = filter.accountId == acc.id, leading = { AccountLogo(acc, size = 24.dp) }) {
                            viewModel.updateTxFilter { it.copy(accountId = acc.id) }; showAccountFilter = false
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAccountFilter = false }) { Text("Đóng", color = p.textSecondary) } }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(p.background),
        contentPadding = PaddingValues(start = Space.page, end = Space.page, top = Space.md, bottom = Space.bottomBarClearance),  // room for the floating "Nói" button
        verticalArrangement = Arrangement.spacedBy(Space.md)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        filter.settledOnly && filter.type == TransactionType.INCOME -> "Thu trong tháng"
                        filter.settledOnly && filter.type == TransactionType.EXPENSE -> "Chi trong tháng"
                        else -> "Giao dịch"
                    },
                    fontSize = TextSize.xl, fontWeight = FontWeight.Bold, color = p.textPrimary, modifier = Modifier.weight(1f)
                )
                Box {
                    IconButton(onClick = { showAccountFilter = true }) {
                        Icon(Icons.Outlined.FilterList, contentDescription = "Bộ lọc", tint = if (filter.accountId != null) p.accent else p.textPrimary)
                    }
                    if (filter.accountId != null) Box(Modifier.size(8.dp).align(Alignment.TopEnd).padding(1.dp).clip(Radius.pill).background(p.accent))
                }
            }
        }

        // Search
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clip(Radius.md)
                    .background(p.card)
                    .border(1.dp, p.border, Radius.md)
                    .padding(start = Space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Search, contentDescription = null, tint = p.textSecondary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(Space.sm))
                Box(Modifier.weight(1f)) {
                    if (filter.query.isEmpty()) Text("Tìm giao dịch…", fontSize = TextSize.base, color = p.textSecondary)
                    BasicTextField(
                        value = filter.query,
                        onValueChange = { q -> viewModel.updateTxFilter { it.copy(query = q) } },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = TextSize.base, color = p.textPrimary),
                        cursorBrush = SolidColor(p.accent),
                        modifier = Modifier.fillMaxWidth().testTag("transaction_search_field")
                    )
                }
                if (filter.query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateTxFilter { it.copy(query = "") } }) {
                        Icon(Icons.Outlined.Clear, contentDescription = "Xóa tìm kiếm", tint = p.textSecondary)
                    }
                }
                IconButton(onClick = onVoice, modifier = Modifier.testTag("search_voice_btn")) {
                    Icon(Icons.Outlined.Mic, contentDescription = "Ghi bằng giọng nói", tint = p.accent)
                }
            }
        }

        // Type chips
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                listOf("Tất cả" to null, "Chi" to TransactionType.EXPENSE, "Thu" to TransactionType.INCOME, "Chuyển" to TransactionType.TRANSFER)
                    .forEach { (label, type) ->
                        ChoiceChip(label, selected = filter.type == type, onClick = { viewModel.updateTxFilter { it.copy(type = type) } })
                    }
            }
        }

        if (filter.settledOnly) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Giao dịch đã hoàn tất", fontSize = TextSize.sm, color = p.textSecondary, modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.updateTxFilter { FinanceViewModel.TxFilter() } }) {
                        Text("Bỏ lọc", color = p.accent)
                    }
                }
            }
        }

        // Month (or all time)
        item {
            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(Radius.md)
                        .background(p.card)
                        .border(1.dp, p.border, Radius.md)
                        .clickable(role = Role.DropdownList) { monthMenu = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(filter.monthOffset?.let(::monthLabel) ?: "Tất cả thời gian", fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                    Spacer(Modifier.width(Space.xs))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Chọn thời gian", tint = p.textSecondary)
                }
                DropdownMenu(expanded = monthMenu, onDismissRequest = { monthMenu = false }, containerColor = p.card) {
                    DropdownMenuItem(text = { Text("Tất cả thời gian", color = p.textPrimary) }, onClick = { viewModel.updateTxFilter { it.copy(monthOffset = null) }; monthMenu = false })
                    for (o in 0 downTo -11) {
                        DropdownMenuItem(
                            text = { Text(monthLabel(o), color = if (filter.monthOffset == o) p.accent else p.textPrimary, fontWeight = if (filter.monthOffset == o) FontWeight.SemiBold else FontWeight.Normal) },
                            onClick = { viewModel.updateTxFilter { it.copy(monthOffset = o) }; monthMenu = false }
                        )
                    }
                }
            }
        }

        // Totals for the listed range
        item {
            AppCard(contentPadding = Space.lg) {
                if (natural != null) {
                    Label("Kết quả", color = p.accent)
                    Spacer(Modifier.height(Space.xs))
                    Text(natural.summaryText, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                    Spacer(Modifier.height(Space.md))
                }
                if (filter.settledOnly && filter.type in listOf(TransactionType.INCOME, TransactionType.EXPENSE)) {
                    val income = filter.type == TransactionType.INCOME
                    Label(if (income) "Tổng thu" else "Tổng chi")
                    MoneyText(formatVnd(if (income) totalIn else totalOut), fontSize = TextSize.lg, color = if (income) p.income else p.expense)
                    Spacer(Modifier.height(Space.xs))
                    Text("${listed.size} giao dịch đã hoàn tất", fontSize = TextSize.xs, color = p.textSecondary)
                } else {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Label("Tổng thu")
                            MoneyText(formatVnd(totalIn), fontSize = TextSize.lg, color = p.income)
                        }
                        Box(Modifier.width(1.dp).height(40.dp).background(p.border))
                        Column(Modifier.weight(1f).padding(start = Space.lg)) {
                            Label("Tổng chi")
                            MoneyText(formatVnd(totalOut), fontSize = TextSize.lg, color = p.expense)
                        }
                    }
                }
            }
        }

        if (listed.isEmpty()) {
            item {
                AppCard {
                    if (transactions.isEmpty()) {
                        EmptyState(
                            icon = Icons.Outlined.ReceiptLong,
                            title = "Chưa có giao dịch",
                            body = "Bấm + ở thanh dưới để ghi khoản đầu tiên.",
                            actionLabel = "Thêm giao dịch",
                            onAction = onNavigateToAddTransaction
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "Không có giao dịch phù hợp",
                            body = "Thử đổi tháng, loại giao dịch hoặc từ khóa.",
                            actionLabel = if (!filter.isDefault) "Bỏ bộ lọc" else null,
                            onAction = { viewModel.updateTxFilter { FinanceViewModel.TxFilter() } }
                        )
                    }
                }
            }
        }

        for ((day, txs) in grouped) {
            item(key = "day_$day") {
                val dayIn = txs.filter { it.type == TransactionType.INCOME && it.countsInReports }.sumOf { it.amount }
                val dayOut = txs.filter { it.type == TransactionType.EXPENSE && it.countsInReports }.sumOf { it.amount }
                Row(Modifier.fillMaxWidth().padding(top = Space.xs, bottom = Space.sm), verticalAlignment = Alignment.CenterVertically) {
                    Text(day, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = p.textSecondary, modifier = Modifier.weight(1f))
                    if (dayIn > 0) {
                        MoneyText("+" + formatVnd(dayIn), fontSize = TextSize.sm, color = p.income)
                        Spacer(Modifier.width(Space.sm))
                    }
                    if (dayOut > 0) MoneyText(MINUS + formatVnd(dayOut), fontSize = TextSize.sm, color = p.textPrimary)
                }
                AppCard(contentPadding = Space.lg) {
                    txs.forEachIndexed { i, tx ->
                        if (i > 0) RowDivider()
                        Box(Modifier.testTag("tx_item_${tx.id}")) {
                            TransactionRow(
                                tx = tx,
                                category = categoryById[tx.categoryId],
                                account = accountById[tx.sourceAccountId],
                                destination = tx.destinationAccountId?.let { accountById[it] },
                                onClick = { if (tx.debtId != null || tx.type == com.example.data.model.TransactionType.ADJUSTMENT) deleting = tx else editing = tx }
                            )
                        }
                    }
                }
            }
        }

        // Voice shortcut
        item {
            AppCard(onClick = onVoice, color = p.accentSoft, borderColor = p.accentSoft, contentPadding = Space.lg) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Outlined.Mic, tint = p.accent, background = p.card)
                    Spacer(Modifier.width(Space.md))
                    Column(Modifier.weight(1f)) {
                        Text("Ghi nhanh bằng giọng nói", fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                        Text("Ví dụ: Ăn sáng 30 nghìn, tiền mặt", fontSize = TextSize.xs, color = p.textSecondary)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.textTertiary)
                }
            }
        }
    }
}

@Composable
private fun FilterOption(label: String, selected: Boolean, leading: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    val p = AppPalette
    Row(
        modifier = Modifier.fillMaxWidth().clip(Radius.sm).clickable(onClick = onClick).heightIn(min = 48.dp).padding(horizontal = Space.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) { leading(); Spacer(Modifier.width(Space.md)) }
        Text(label, fontSize = TextSize.base, color = if (selected) p.accent else p.textPrimary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
    }
}
