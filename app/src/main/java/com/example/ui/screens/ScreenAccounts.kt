package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material.icons.automirrored.outlined.EventNote
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Visibility
import com.example.ui.components.IconTile
import com.example.ui.components.ActionTile
import com.example.ui.components.SecondaryButton
import com.example.ui.components.PrimaryButton
import com.example.ui.components.MoneyText
import com.example.ui.components.HeroMoney
import com.example.ui.components.HeroCard
import com.example.ui.theme.AppPalette
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.CreditCardStatus
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.payment.vietqr.VietnameseBankRegistry
import com.example.ui.components.AccountLogo
import com.example.ui.components.AppCard
import com.example.ui.components.BankLogoBadge
import com.example.ui.components.Label
import com.example.ui.components.ListRow
import com.example.ui.components.RowDivider
import com.example.ui.components.SectionHeader
import com.example.ui.components.ThresholdBar
import com.example.ui.components.compactVnd
import com.example.ui.components.formatVnd
import com.example.ui.theme.AppleSurface1
import com.example.ui.theme.AppleTextPrimary
import com.example.ui.theme.AppleTextSecondary
import com.example.ui.theme.Radius
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.theme.Tone
import com.example.ui.viewmodel.FinanceViewModel
import kotlin.math.roundToInt

private val SECTIONS = listOf(
    "Tiền mặt & ví" to setOf(AccountType.CASH, AccountType.E_WALLET),
    "Ngân hàng" to setOf(AccountType.BANK, AccountType.SAVINGS, AccountType.INVESTMENT),
    "Thẻ tín dụng" to setOf(AccountType.CREDIT_CARD),
    "Khoản vay & khác" to setOf(AccountType.RECEIVABLE, AccountType.PAYABLE, AccountType.LOAN)
)

internal fun parseVnd(text: String): Double = text.filter(Char::isDigit).toDoubleOrNull() ?: 0.0

/**
 * Accounts: net worth card, two quick actions (transfer, reconcile), accounts grouped by kind
 * with a total per group; tapping an account opens its detail and history.
 */
@Composable
fun ScreenAccounts(
    viewModel: FinanceViewModel,
    onOpenAccount: (Long) -> Unit,
    onTransfer: () -> Unit,
    onOpenPlans: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = AppPalette
    val context = LocalContext.current
    val accounts by viewModel.accounts.collectAsState()
    val snapshot by viewModel.financialSnapshot.collectAsState()
    val hidden by viewModel.balancesHidden.collectAsState()
    val cardStatus = snapshot.creditCards.associateBy { it.account.id }
    fun money(v: Double) = if (hidden) HIDDEN_AMOUNT else formatVnd(v)

    var showAdd by remember { mutableStateOf(false) }
    var pickReconcile by remember { mutableStateOf(false) }
    var reconciling by remember { mutableStateOf<Account?>(null) }
    var paying by remember { mutableStateOf<Account?>(null) }

    if (showAdd) {
        AddAccountDialog(onDismiss = { showAdd = false }) { acc ->
            viewModel.addAccount(acc)
            showAdd = false
            Toast.makeText(context, "Đã thêm ${acc.name}", Toast.LENGTH_SHORT).show()
        }
    }
    if (pickReconcile) {
        AlertDialog(
            onDismissRequest = { pickReconcile = false },
            containerColor = p.card,
            title = { Text("Đối soát tài khoản nào?", fontWeight = FontWeight.Bold, color = p.textPrimary) },
            text = {
                Column {
                    accounts.forEach { acc ->
                        ListRow(
                            title = acc.name,
                            leading = { AccountLogo(acc, size = 28.dp) },
                            trailing = { MoneyText(formatVnd(acc.balance), color = p.textSecondary, fontWeight = FontWeight.Normal) },
                            onClick = { reconciling = acc; pickReconcile = false }
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickReconcile = false }) { Text("Đóng", color = p.textSecondary) } }
        )
    }
    reconciling?.let { acc ->
        AmountDialog(
            title = "Đối soát: ${acc.name}",
            hint = "Trong app: ${formatVnd(acc.balance)}. Nhập số dư thật đang có (âm nếu là nợ); chênh lệch được ghi thành một dòng đối soát.",
            label = "Số dư thực tế (₫)",
            allowNegative = true,
            initial = acc.balance.toLong().toString(),
            onDismiss = { reconciling = null }
        ) { value ->
            viewModel.reconcileAccountBalance(acc.id, value)
            reconciling = null
        }
    }
    paying?.let { card ->
        PayCardDialog(
            card = card,
            debt = (-card.balance).coerceAtLeast(0.0),
            sources = accounts.filter { it.id != card.id && it.type != AccountType.CREDIT_CARD },
            onDismiss = { paying = null }
        ) { fromId, amount ->
            viewModel.payCreditCard(card.id, fromId, amount)
            Toast.makeText(context, "Đã ghi nhận trả thẻ ${formatVnd(amount)}", Toast.LENGTH_SHORT).show()
            paying = null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = Space.page, end = Space.page, top = Space.md, bottom = Space.bottomBarClearance),  // room for the floating "Nói" button
        verticalArrangement = Arrangement.spacedBy(Space.lg)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tài khoản", fontSize = TextSize.xl, fontWeight = FontWeight.Bold, color = p.textPrimary, modifier = Modifier.weight(1f))
                PrimaryButton(
                    text = "Thêm",
                    icon = Icons.Default.Add,
                    onClick = { showAdd = true },
                    modifier = Modifier.testTag("add_account_button")
                )
            }
        }

        item {
            HeroCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Tài sản ròng", fontSize = TextSize.sm, color = p.onHeroMuted, modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.toggleBalancesHidden() }) {
                        Icon(if (hidden) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (hidden) "Hiện số dư" else "Ẩn số dư", tint = p.onHeroMuted)
                    }
                }
                HeroMoney(money(snapshot.netWorth))
                Spacer(Modifier.height(Space.md))
                Box(Modifier.fillMaxWidth().height(1.dp).background(p.onHero.copy(alpha = 0.18f)))
                Spacer(Modifier.height(Space.md))
                Row {
                    Column(Modifier.weight(1f)) {
                        Text("Tổng tài sản", fontSize = TextSize.xs, color = p.onHeroMuted)
                        MoneyText(money(snapshot.totalAssets), fontSize = TextSize.base, color = p.onHero)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text("Tổng nợ", fontSize = TextSize.xs, color = p.onHeroMuted)
                        MoneyText(money(snapshot.totalLiabilities), fontSize = TextSize.base, color = p.onHero)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                ActionTile(Icons.Outlined.SwapHoriz, "Chuyển tiền", onTransfer, Modifier.weight(1f))
                ActionTile(Icons.Outlined.FactCheck, "Đối soát số dư", { pickReconcile = true }, Modifier.weight(1f))
            }
        }

        for ((title, types) in SECTIONS) {
            val inSection = accounts.filter { it.type in types }
            if (inSection.isEmpty()) continue
            item(key = title) {
                val isCard = types.contains(AccountType.CREDIT_CARD)
                val total = inSection.sumOf { it.balance }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = Space.sm)) {
                    Text(title, fontSize = TextSize.lg, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                    Text(" · ", fontSize = TextSize.lg, color = p.textSecondary)
                    MoneyText(
                        if (isCard) "Dư nợ " + money(-total) else money(total),
                        fontSize = TextSize.lg,
                        color = if (isCard && total < 0) p.expense else p.textPrimary
                    )
                }
                if (isCard) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                        inSection.forEach { card ->
                            CreditCardTile(card, cardStatus[card.id], hidden, onClick = { onOpenAccount(card.id) }, onPay = { paying = card })
                        }
                    }
                } else {
                    AppCard(contentPadding = Space.lg) {
                        inSection.forEachIndexed { i, acc ->
                            if (i > 0) RowDivider()
                            ListRow(
                                title = acc.name,
                                subtitle = acc.accountNumberMasked ?: acc.type.displayName.takeIf { it != acc.name },
                                leading = { AccountLogo(acc, size = 32.dp) },
                                trailing = { MoneyText(money(acc.balance), color = if (acc.balance < 0) p.expense else p.textPrimary) },
                                showChevron = true,
                                onClick = { onOpenAccount(acc.id) }
                            )
                        }
                    }
                }
            }
        }

        item {
            AppCard(contentPadding = Space.lg) {
                ListRow(
                    title = "Sổ nợ, mục tiêu & hóa đơn",
                    leading = { IconTile(Icons.AutoMirrored.Outlined.EventNote, tint = p.accent, background = p.accentSoft) },
                    showChevron = true,
                    onClick = onOpenPlans
                )
                RowDivider()
                ListRow(
                    title = "Quản lý tài khoản & cài đặt",
                    subtitle = "Tài khoản đã ẩn, giao diện sáng/tối, danh mục",
                    leading = { IconTile(Icons.Outlined.Settings, tint = p.accent, background = p.accentSoft) },
                    showChevron = true,
                    onClick = onOpenSettings
                )
            }
        }
    }
}

@Composable
internal fun CreditCardTile(card: Account, status: CreditCardStatus?, hidden: Boolean, onClick: () -> Unit, onPay: () -> Unit) {
    val p = AppPalette
    val debt = (-card.balance).coerceAtLeast(0.0)
    val util = if (card.creditLimit > 0) debt / card.creditLimit else 0.0
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AccountLogo(card, size = 32.dp)
            Spacer(Modifier.width(Space.md))
            Column(Modifier.weight(1f)) {
                Text(card.name, fontSize = TextSize.base, fontWeight = FontWeight.Medium, color = p.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(card.accountNumberMasked, card.statementDay?.let { "Sao kê ngày $it" }).joinToString(" · ").ifBlank { "Thẻ tín dụng" },
                    fontSize = TextSize.xs, color = p.textSecondary
                )
            }
            status?.daysToDue?.let { d ->
                val c = when { debt <= 0 -> p.textSecondary; d <= 3 -> p.danger; d <= 7 -> p.warning; else -> p.textSecondary }
                Text(if (debt <= 0) "Không nợ" else if (d == 0) "Hạn hôm nay" else "Hạn sau $d ngày", fontSize = TextSize.xs, fontWeight = FontWeight.SemiBold, color = c)
            }
        }
        Spacer(Modifier.height(Space.lg))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("Dư nợ", fontSize = TextSize.xs, color = p.textSecondary)
                HeroMoney(if (hidden) HIDDEN_AMOUNT else formatVnd(debt), color = if (debt > 0) p.expense else p.textPrimary, base = TextSize.figure)
            }
            if (card.creditLimit > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("Còn trống", fontSize = TextSize.xs, color = p.textSecondary)
                    MoneyText(if (hidden) HIDDEN_AMOUNT else formatVnd((card.creditLimit - debt).coerceAtLeast(0.0)))
                }
            }
        }
        if (card.creditLimit > 0) {
            Spacer(Modifier.height(Space.md))
            ThresholdBar(util)
            Spacer(Modifier.height(Space.xs + 2.dp))
            Text("Đã dùng ${(util * 100).roundToInt()}% hạn mức ${formatVnd(card.creditLimit)}", fontSize = TextSize.xs, color = p.textSecondary)
        } else {
            Spacer(Modifier.height(Space.sm))
            Text("Chưa nhập hạn mức — mở thẻ để cài đặt", fontSize = TextSize.xs, color = p.accent)
        }
        if (debt > 0) {
            Spacer(Modifier.height(Space.md))
            SecondaryButton("Trả thẻ", onClick = onPay, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
internal fun AccountActionsDialog(
    account: Account,
    onDismiss: () -> Unit,
    onReconcile: () -> Unit,
    onPayCard: () -> Unit,
    onEditCard: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppleSurface1,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccountLogo(account, size = 28.dp)
                Spacer(Modifier.width(Space.md))
                Text(account.name, fontWeight = FontWeight.Bold, color = AppleTextPrimary)
            }
        },
        text = {
            Column {
                if (account.type == AccountType.CREDIT_CARD) {
                    ActionRow("Trả thẻ", onPayCard)
                    ActionRow("Cài đặt thẻ (hạn mức, ngày sao kê, ngày đến hạn)", onEditCard)
                }
                ActionRow("Điều chỉnh số dư", onReconcile)
                ActionRow("Ngừng dùng (ẩn, giữ lịch sử)", onArchive)
                ActionRow("Xóa hẳn", onDelete, color = Tone.danger)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Đóng", color = AppleTextSecondary) } }
    )
}

@Composable
private fun ActionRow(text: String, onClick: () -> Unit, color: Color = AppleTextPrimary) {
    Text(
        text,
        fontSize = TextSize.sm,
        color = color,
        modifier = Modifier.fillMaxWidth().clip(Radius.sm).clickable(onClick = onClick).padding(vertical = Space.md, horizontal = Space.xs)
    )
}

@Composable
internal fun AmountDialog(
    title: String,
    hint: String,
    label: String,
    initial: String = "",
    allowNegative: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var input by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppleSurface1,
        title = { Text(title, fontWeight = FontWeight.Bold, color = AppleTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Text(hint, fontSize = TextSize.sm, color = AppleTextSecondary)
                OutlinedTextField(
                    value = input,
                    onValueChange = { v -> input = v.filterIndexed { i, c -> c.isDigit() || (allowNegative && c == '-' && i == 0) } },
                    label = { Text(label) },
                    supportingText = { Text(formatVnd(input.toDoubleOrNull() ?: 0.0)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("reconcile_balance_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { input.toDoubleOrNull()?.let(onConfirm) },
                colors = ButtonDefaults.buttonColors(containerColor = Tone.accent, contentColor = com.example.ui.theme.Tone.onAccent)
            ) { Text("Lưu", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = AppleTextSecondary) } }
    )
}

@Composable
internal fun PayCardDialog(card: Account, debt: Double, sources: List<Account>, onDismiss: () -> Unit, onConfirm: (Long, Double) -> Unit) {
    var input by remember { mutableStateOf(debt.toLong().toString()) }
    var fromId by remember { mutableStateOf(sources.firstOrNull { it.type == AccountType.BANK }?.id ?: sources.firstOrNull()?.id) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppleSurface1,
        title = { Text("Trả thẻ ${card.name}", fontWeight = FontWeight.Bold, color = AppleTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                Text("Dư nợ: ${formatVnd(debt)}. Trả thẻ là chuyển tiền giữa tài khoản của bạn, không tính vào chi tiêu.",
                    fontSize = TextSize.sm, color = AppleTextSecondary)
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter(Char::isDigit) },
                    label = { Text("Số tiền trả") },
                    supportingText = { Text(formatVnd(input.toDoubleOrNull() ?: 0.0)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Label("Trả từ")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(sources, key = { it.id }) { acc ->
                        FilterChip(selected = fromId == acc.id, onClick = { fromId = acc.id }, label = { Text(acc.name) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = input.toDoubleOrNull() ?: 0.0
                    val from = fromId
                    if (amount > 0 && from != null) onConfirm(from, amount)
                },
                enabled = sources.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = Tone.accent, contentColor = com.example.ui.theme.Tone.onAccent)
            ) { Text("Trả thẻ", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = AppleTextSecondary) } }
    )
}

@Composable
internal fun CardSettingsDialog(card: Account, onDismiss: () -> Unit, onSave: (Account) -> Unit) {
    var limit by remember { mutableStateOf(if (card.creditLimit > 0) card.creditLimit.toLong().toString() else "") }
    var statement by remember { mutableStateOf(card.statementDay?.toString() ?: "") }
    var due by remember { mutableStateOf(card.dueDay?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppleSurface1,
        title = { Text("Cài đặt thẻ ${card.name}", fontWeight = FontWeight.Bold, color = AppleTextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Space.md)) {
                CreditCardFields(limit, { limit = it }, statement, { statement = it }, due, { due = it })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(card.copy(creditLimit = parseVnd(limit), statementDay = statement.toIntOrNull()?.coerceIn(1, 31), dueDay = due.toIntOrNull()?.coerceIn(1, 31)))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Tone.accent, contentColor = com.example.ui.theme.Tone.onAccent)
            ) { Text("Lưu", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = AppleTextSecondary) } }
    )
}

@Composable
private fun CreditCardFields(
    limit: String, onLimit: (String) -> Unit,
    statement: String, onStatement: (String) -> Unit,
    due: String, onDue: (String) -> Unit
) {
    OutlinedTextField(
        value = limit,
        onValueChange = { onLimit(it.filter(Char::isDigit)) },
        label = { Text("Hạn mức (VNĐ)") },
        supportingText = { Text(formatVnd(limit.toDoubleOrNull() ?: 0.0)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Space.md)) {
        OutlinedTextField(
            value = statement,
            onValueChange = { onStatement(it.filter(Char::isDigit).take(2)) },
            label = { Text("Ngày sao kê") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = due,
            onValueChange = { onDue(it.filter(Char::isDigit).take(2)) },
            label = { Text("Ngày đến hạn") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
    }
}

private val ADDABLE_TYPES = listOf(AccountType.CASH, AccountType.BANK, AccountType.E_WALLET, AccountType.CREDIT_CARD, AccountType.SAVINGS, AccountType.INVESTMENT)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddAccountDialog(
    onDismiss: () -> Unit,
    onAddAccount: (Account) -> Unit
) {
    var type by remember { mutableStateOf(AccountType.BANK) }
    var bankId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var balance by remember { mutableStateOf("") }
    var masked by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("") }
    var statement by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }
    val needsBank = type == AccountType.BANK || type == AccountType.CREDIT_CARD || type == AccountType.E_WALLET || type == AccountType.SAVINGS
    val banks = remember(type) {
        VietnameseBankRegistry.BANKS.filter { b ->
            val wallet = b.supportedPaymentMethods.contains("wallet")
            if (type == AccountType.E_WALLET) wallet else !wallet
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppleSurface1,
        title = { Text("Thêm tài khoản", fontWeight = FontWeight.Bold, color = AppleTextPrimary) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Space.md)
            ) {
                Label("Loại")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ADDABLE_TYPES.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = {
                                type = t
                                bankId = null
                                if (t == AccountType.CASH) name = "Tiền mặt"
                            },
                            label = { Text(t.displayName) }
                        )
                    }
                }
                if (needsBank) {
                    Label(if (type == AccountType.E_WALLET) "Ví" else "Ngân hàng")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                        banks.forEach { bank ->
                            val on = bankId == bank.id
                            Box(
                                modifier = Modifier
                                    .clip(Radius.sm)
                                    .border(2.dp, if (on) Tone.accent else Color.Transparent, Radius.sm)
                                    .clickable {
                                        bankId = bank.id
                                        name = if (type == AccountType.CREDIT_CARD) "Thẻ ${bank.shortName}" else bank.shortName
                                    }
                                    .padding(3.dp)
                            ) {
                                BankLogoBadge(bankId = bank.id, size = 30.dp)
                            }
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Tên hiển thị") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (needsBank) {
                    OutlinedTextField(
                        value = masked,
                        onValueChange = { masked = it.filter(Char::isDigit).takeLast(4) },
                        label = { Text("4 số cuối (không bắt buộc)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = balance,
                    onValueChange = { balance = it.filter(Char::isDigit) },
                    label = { Text(if (type == AccountType.CREDIT_CARD) "Dư nợ hiện tại (VNĐ)" else "Số dư hiện tại (VNĐ)") },
                    supportingText = { Text(formatVnd(balance.toDoubleOrNull() ?: 0.0)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (type == AccountType.CREDIT_CARD) {
                    CreditCardFields(limit, { limit = it }, statement, { statement = it }, due, { due = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = parseVnd(balance)
                    val bank = bankId?.let { VietnameseBankRegistry.findById(it) }
                    onAddAccount(
                        Account(
                            name = name.ifBlank { bank?.shortName ?: type.displayName },
                            type = type,
                            // A card's balance is what you owe, stored as a negative number.
                            balance = if (type == AccountType.CREDIT_CARD) -amount else amount,
                            bankId = bankId ?: if (type == AccountType.CASH) "cash" else null,
                            bankCode = bank?.id?.uppercase(),
                            accountNumberMasked = masked.takeIf { it.isNotBlank() }?.let { "•••• $it" },
                            creditLimit = parseVnd(limit),
                            statementDay = statement.toIntOrNull()?.coerceIn(1, 31),
                            dueDay = due.toIntOrNull()?.coerceIn(1, 31),
                            colorHex = bank?.brandColorHex ?: "#0A84FF"
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = Tone.accent, contentColor = com.example.ui.theme.Tone.onAccent)
            ) { Text("Thêm", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy", color = AppleTextSecondary) } }
    )
}

