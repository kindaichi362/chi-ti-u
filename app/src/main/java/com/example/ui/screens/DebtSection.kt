package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.formatVnd
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/*
 * Debts ("Vay nợ"): lending or borrowing moves real money in or out of an account, and repayments
 * move it back. None of it is income or spending, so reports leave it out (see FinanceRepository).
 */

private val DAY_FORMAT get() = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
private const val DAY_MS = 24L * 60 * 60 * 1000

private fun digitsOnly(input: String): String = input.filter { it.isDigit() }.trimStart('0').take(13)

private fun Debt.isBorrowed() = type == DebtType.I_OWE

/** Start of the local day, for comparing due dates by calendar day. */
private fun dayStart(millis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = millis
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

/** "Quá hạn 3 ngày" (red), "Còn 2 ngày" (orange, a week or less) or "Hạn dd/MM/yyyy". */
@Composable
internal fun debtDueLabel(debt: Debt, now: Long = System.currentTimeMillis()): Pair<String, Color>? {
    val due = debt.dueDate ?: return null
    if (debt.isSettled) return null
    val days = ((dayStart(due) - dayStart(now)) / DAY_MS).toInt()
    return when {
        days < 0 -> "Quá hạn ${-days} ngày" to ExpenseRed
        days == 0 -> "Đến hạn hôm nay" to ExpenseRed
        days <= 7 -> "Còn $days ngày" to Color(0xFFF59E0B)
        else -> "Hạn ${DAY_FORMAT.format(Date(due))}" to Color.Gray
    }
}

/** Date picker works in UTC midnight; store the chosen day as local noon so time zones never shift it. */
private fun utcMidnightToLocalNoon(utc: Long): Long {
    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utc }
    return Calendar.getInstance().apply {
        clear(); set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH), 12, 0)
    }.timeInMillis
}

private fun localToUtcMidnight(local: Long): Long {
    val c = Calendar.getInstance().apply { timeInMillis = local }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, millis: Long?, onChange: (Long?) -> Unit, clearable: Boolean) {
    var open by remember { mutableStateOf(false) }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = localToUtcMidnight(millis ?: System.currentTimeMillis()))
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onChange(utcMidnightToLocalNoon(it)) }; open = false }) { Text("Chọn") } },
            dismissButton = { TextButton(onClick = { open = false }) { Text("Hủy") } }
        ) { DatePicker(state = state) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("$label: " + (millis?.let { DAY_FORMAT.format(Date(it)) } ?: "chưa đặt"), fontSize = 13.sp)
        }
        if (clearable && millis != null) TextButton(onClick = { onChange(null) }) { Text("Bỏ") }
    }
}

@Composable
private fun MoneyField(value: String, onChange: (String) -> Unit, label: String, error: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(digitsOnly(it)) },
        label = { Text(label) },
        // Shows the amount grouped ("5.000.000 ₫") so a missing or extra zero is obvious.
        supportingText = { Text(error ?: (value.toDoubleOrNull()?.let { formatVnd(it) } ?: "Nhập số, ví dụ 5000000"), color = if (error != null) ExpenseRed else Color.Unspecified) },
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Accounts as chips with their balance, so it is clear where money goes in or comes from. */
@Composable
private fun AccountPicker(accounts: List<Account>, selectedId: Long?, onSelect: (Long) -> Unit) {
    if (accounts.isEmpty()) {
        Text("Chưa có tài khoản nào — thêm ở mục Tài khoản trước.", fontSize = 12.sp, color = ExpenseRed)
        return
    }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(accounts, key = { it.id }) { acc ->
            FilterChip(
                selected = selectedId == acc.id,
                onClick = { onSelect(acc.id) },
                label = {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        Text(acc.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(formatVnd(acc.balance), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
private fun TypeToggle(type: DebtType, onChange: (DebtType) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(DebtType.I_OWE to "Tôi đi vay", DebtType.OWED_TO_ME to "Tôi cho vay").forEach { (t, label) ->
            val selected = type == t
            val color = if (t == DebtType.I_OWE) ExpenseRed else IncomeGreen
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) color.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f).heightIn(min = 44.dp).clickable { onChange(t) }
            ) {
                Text(
                    label, textAlign = TextAlign.Center, fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

/** An AlertDialog, or the same parts laid out in place (screenshot tests cannot capture dialog windows). */
@Composable
private fun DialogFrame(
    inline: Boolean,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit
) {
    if (!inline) {
        AlertDialog(onDismissRequest = onDismissRequest, title = title, text = text, confirmButton = confirmButton, dismissButton = dismissButton)
        return
    }
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.padding(16.dp)) {
        Column(Modifier.padding(24.dp)) {
            androidx.compose.material3.ProvideTextStyle(MaterialTheme.typography.headlineSmall) { title() }
            Spacer(Modifier.height(16.dp))
            text()
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.End) { dismissButton(); confirmButton() }
        }
    }
}

@Composable
fun AddDebtDialog(accounts: List<Account>, onDismiss: () -> Unit, inline: Boolean = false, initialType: DebtType = DebtType.I_OWE, onSave: (Debt) -> Unit) {
    var type by remember { mutableStateOf(initialType) }
    var person by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var due by remember { mutableStateOf<Long?>(null) }
    var bookToAccount by remember { mutableStateOf(true) }
    var accountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
    var error by remember { mutableStateOf<String?>(null) }
    if (accountId == null && accounts.isNotEmpty()) accountId = accounts.first().id
    val amt = amount.toDoubleOrNull() ?: 0.0
    val selected = accounts.firstOrNull { it.id == accountId }

    DialogFrame(
        inline = inline,
        onDismissRequest = onDismiss,
        title = { Text("Thêm khoản vay nợ", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TypeToggle(type) { type = it }
                OutlinedTextField(person, { person = it; error = null }, label = { Text(if (type == DebtType.I_OWE) "Vay của ai" else "Cho ai vay") },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                MoneyField(amount, { amount = it; error = null }, "Số tiền")

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (type == DebtType.I_OWE) "Tiền vay nhận vào tài khoản" else "Tiền cho vay lấy từ tài khoản", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        if (!bookToAccount) Text("Nợ cũ, không đổi số dư tài khoản", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = bookToAccount, onCheckedChange = { bookToAccount = it; error = null })
                }
                if (bookToAccount) {
                    AccountPicker(accounts, accountId) { accountId = it; error = null }
                    if (selected != null && amt > 0) {
                        val after = if (type == DebtType.I_OWE) selected.balance + amt else selected.balance - amt
                        Text("Số dư ${selected.name}: ${formatVnd(selected.balance)} → ${formatVnd(after)}", fontSize = 12.sp,
                            color = if (after < 0) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                DateField("Hẹn trả", due, { due = it }, clearable = true)
                OutlinedTextField(phone, { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } }, label = { Text("Số điện thoại (không bắt buộc)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(note, { note = it }, label = { Text("Ghi chú (không bắt buộc)") }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = ExpenseRed, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            Button(onClick = {
                error = when {
                    person.isBlank() -> "Nhập tên người ${if (type == DebtType.I_OWE) "cho mình vay" else "vay tiền"}"
                    amt <= 0 -> "Nhập số tiền lớn hơn 0"
                    bookToAccount && accountId == null -> "Chọn tài khoản, hoặc tắt \"ghi vào tài khoản\" nếu là nợ cũ"
                    else -> null
                }
                if (error == null) onSave(
                    Debt(type = type, personName = person.trim(), phone = phone.trim(), initialAmount = amt, remainingAmount = amt,
                        dueDate = due, note = note.trim(), accountId = if (bookToAccount) accountId else null)
                )
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
fun PayDebtDialog(debt: Debt, accounts: List<Account>, onDismiss: () -> Unit, onPay: (amount: Double, accountId: Long, timestamp: Long) -> Unit) {
    var amount by remember(debt.id) { mutableStateOf("") }
    var accountId by remember(debt.id) { mutableStateOf(debt.accountId?.takeIf { id -> accounts.any { it.id == id } } ?: accounts.firstOrNull()?.id) }
    var date by remember(debt.id) { mutableLongStateOf(System.currentTimeMillis()) }
    var error by remember(debt.id) { mutableStateOf<String?>(null) }
    val amt = amount.toDoubleOrNull() ?: 0.0
    val borrowed = debt.isBorrowed()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (borrowed) "Trả nợ ${debt.personName}" else "Thu nợ ${debt.personName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Còn ${if (borrowed) "nợ" else "phải thu"}: ${formatVnd(debt.remainingAmount)}", fontWeight = FontWeight.Bold,
                    color = if (borrowed) ExpenseRed else IncomeGreen)
                MoneyField(amount, { amount = it; error = null }, if (borrowed) "Số tiền trả lần này" else "Số tiền thu lần này", error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { amount = debt.remainingAmount.toLong().toString(); error = null }) { Text(if (borrowed) "Trả hết" else "Thu hết") }
                    if (debt.remainingAmount >= 2) OutlinedButton(onClick = { amount = (debt.remainingAmount / 2).toLong().toString(); error = null }) { Text("Một nửa") }
                }
                Text(if (borrowed) "Trả từ tài khoản" else "Nhận vào tài khoản", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                AccountPicker(accounts, accountId) { accountId = it }
                DateField("Ngày", date, { if (it != null) date = it }, clearable = false)
            }
        },
        confirmButton = {
            Button(onClick = {
                val acc = accountId
                error = when {
                    amt <= 0 -> "Nhập số tiền lớn hơn 0"
                    amt > debt.remainingAmount + 0.001 -> "Không được lớn hơn số còn lại ${formatVnd(debt.remainingAmount)}"
                    acc == null -> "Chọn tài khoản"
                    else -> null
                }
                if (error == null && acc != null) {
                    // A past date keeps the current time of day; today records "now".
                    val ts = if (dayStart(date) == dayStart(System.currentTimeMillis())) System.currentTimeMillis() else date
                    onPay(amt, acc, ts)
                }
            }) { Text("Xác nhận") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
fun DebtDetailDialog(
    debt: Debt,
    history: List<Transaction>,
    accounts: List<Account>,
    onDismiss: () -> Unit,
    onPay: () -> Unit,
    onUpdate: (Debt) -> Unit,
    onDelete: () -> Unit
) {
    var editing by remember(debt.id) { mutableStateOf(false) }
    var confirmingDelete by remember(debt.id) { mutableStateOf(false) }
    var name by remember(debt.id) { mutableStateOf(debt.personName) }
    var phone by remember(debt.id) { mutableStateOf(debt.phone) }
    var note by remember(debt.id) { mutableStateOf(debt.note) }
    var due by remember(debt.id) { mutableStateOf(debt.dueDate) }
    val borrowed = debt.isBorrowed()
    val color = if (borrowed) ExpenseRed else IncomeGreen
    val accountName = { id: Long -> accounts.firstOrNull { it.id == id }?.name ?: "?" }
    val paid = debt.initialAmount - debt.remainingAmount

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Xóa khoản ${if (borrowed) "vay" else "cho vay"} của ${debt.personName}?") },
            text = {
                Text(
                    if (history.isEmpty()) "Khoản này không ghi vào tài khoản nào nên số dư không đổi."
                    else "Sẽ xóa ${history.size} giao dịch liên quan và hoàn lại số dư tài khoản như trước khi ghi khoản này. " +
                        "Nếu chỉ muốn đánh dấu đã xong, hãy dùng \"${if (borrowed) "Trả nợ" else "Thu nợ"}\" thay vì xóa."
                )
            },
            confirmButton = { TextButton(onClick = { confirmingDelete = false; onDelete() }) { Text("Xóa", color = ExpenseRed, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmingDelete = false }) { Text("Hủy") } }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing) "Sửa thông tin" else "${if (borrowed) "Vay của" else "Cho vay"} ${debt.personName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (editing) {
                    OutlinedTextField(name, { name = it }, label = { Text("Tên") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it.filter { c -> c.isDigit() || c == '+' || c == ' ' } }, label = { Text("Số điện thoại") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true, modifier = Modifier.fillMaxWidth())
                    DateField("Hẹn trả", due, { due = it }, clearable = true)
                    OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") }, modifier = Modifier.fillMaxWidth())
                    Text("Số tiền gốc không sửa được. Ghi sai thì xóa khoản này rồi thêm lại.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(if (debt.isSettled) "Đã tất toán" else "Còn ${if (borrowed) "nợ" else "phải thu"} ${formatVnd(debt.remainingAmount)}",
                        fontWeight = FontWeight.Bold, fontSize = 18.sp, color = if (debt.isSettled) IncomeGreen else color)
                    LinearProgressIndicator(
                        progress = { if (debt.initialAmount > 0) (paid / debt.initialAmount).toFloat().coerceIn(0f, 1f) else 0f },
                        color = color, modifier = Modifier.fillMaxWidth()
                    )
                    Text("Đã ${if (borrowed) "trả" else "thu"} ${formatVnd(paid)} / gốc ${formatVnd(debt.initialAmount)}", fontSize = 12.sp)
                    debtDueLabel(debt)?.let { (label, c) -> Text(label, color = c, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                    if (debt.phone.isNotBlank()) Text("SĐT: ${debt.phone}", fontSize = 12.sp)
                    if (debt.note.isNotBlank()) Text("Ghi chú: ${debt.note}", fontSize = 12.sp)
                    if (debt.accountId == null) Text("Nợ cũ — lúc tạo không ghi vào tài khoản.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Text("Lịch sử", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    if (history.isEmpty()) Text("Chưa có giao dịch nào.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    history.forEach { tx ->
                        val moneyIn = tx.type == TransactionType.INCOME
                        val origin = tx.note.startsWith("DEBT_ORIGIN:")
                        val label = when {
                            origin && borrowed -> "Nhận tiền vay"
                            origin -> "Đưa tiền cho vay"
                            borrowed -> "Trả nợ"
                            else -> "Thu nợ"
                        }
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(label, fontSize = 13.sp)
                                Text("${DAY_FORMAT.format(Date(tx.timestamp))} · ${accountName(tx.sourceAccountId)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text((if (moneyIn) "+" else "−") + formatVnd(tx.amount), fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = if (moneyIn) IncomeGreen else ExpenseRed)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (editing) Button(onClick = {
                if (name.isNotBlank()) {
                    onUpdate(debt.copy(personName = name.trim(), phone = phone.trim(), dueDate = due, note = note.trim()))
                    editing = false
                }
            }) { Text("Lưu") }
            else if (!debt.isSettled) Button(onClick = onPay) { Text(if (borrowed) "Trả nợ" else "Thu nợ") }
            else TextButton(onClick = onDismiss) { Text("Đóng") }
        },
        dismissButton = {
            if (editing) TextButton(onClick = { editing = false }) { Text("Hủy") }
            else Row {
                TextButton(onClick = { confirmingDelete = true }) { Text("Xóa", color = ExpenseRed) }
                TextButton(onClick = { editing = true }) { Text("Sửa") }
            }
        }
    )
}

/** The "Vay nợ" tab: totals, open/settled filter, and one card per debt (tap to open). */
@Composable
fun DebtTab(debts: List<Debt>, onOpen: (Debt) -> Unit, onPay: (Debt) -> Unit) {
    var filter by remember { mutableIntStateOf(0) } // 0 đang theo dõi · 1 đã xong
    val open = debts.filter { !it.isSettled }
    val totalOwedToMe = open.filter { it.type == DebtType.OWED_TO_ME }.sumOf { it.remainingAmount }
    val totalIOwe = open.filter { it.type == DebtType.I_OWE }.sumOf { it.remainingAmount }
    // Overdue and soon-due first, then the newest.
    val shown = (if (filter == 0) open else debts.filter { it.isSettled })
        .sortedWith(compareBy<Debt> { it.dueDate == null || it.isSettled }.thenBy { it.dueDate ?: Long.MAX_VALUE }.thenByDescending { it.createdAt })

    Column(Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceAround) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Người khác nợ tôi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatVnd(totalOwedToMe), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = IncomeGreen)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tôi đang nợ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatVnd(totalIOwe), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ExpenseRed)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = filter == 0, onClick = { filter = 0 }, label = { Text("Đang theo dõi (${open.size})") })
            FilterChip(selected = filter == 1, onClick = { filter = 1 }, label = { Text("Đã xong (${debts.size - open.size})") })
        }
        Spacer(Modifier.height(8.dp))
        if (shown.isEmpty()) {
            Text(
                if (filter == 0) "Chưa có khoản vay nợ nào. Bấm \"Khoản nợ\" để ghi khoản đi vay hoặc cho vay." else "Chưa có khoản nào đã tất toán.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(16.dp)
            )
        } else LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(shown, key = { it.id }) { debt -> DebtCard(debt, onOpen = { onOpen(debt) }, onPay = { onPay(debt) }) }
        }
    }
}

@Composable
private fun DebtCard(debt: Debt, onOpen: () -> Unit, onPay: () -> Unit) {
    val borrowed = debt.isBorrowed()
    val color = if (borrowed) ExpenseRed else IncomeGreen
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Person, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(debt.personName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    if (debt.isSettled) "${if (borrowed) "Đã trả hết" else "Đã thu hết"} ${formatVnd(debt.initialAmount)}"
                    else "${if (borrowed) "Tôi nợ" else "Nợ tôi"}: ${formatVnd(debt.remainingAmount)}",
                    fontSize = 12.sp, color = if (debt.isSettled) MaterialTheme.colorScheme.onSurfaceVariant else color, fontWeight = FontWeight.SemiBold
                )
                if (!debt.isSettled && debt.remainingAmount < debt.initialAmount) {
                    LinearProgressIndicator(
                        progress = { ((debt.initialAmount - debt.remainingAmount) / debt.initialAmount).toFloat().coerceIn(0f, 1f) },
                        color = color, modifier = Modifier.fillMaxWidth().padding(top = 4.dp, end = 8.dp)
                    )
                }
                debtDueLabel(debt)?.let { (label, c) -> Text(label, fontSize = 11.sp, color = c, fontWeight = FontWeight.SemiBold) }
                if (debt.note.isNotBlank()) Text(debt.note, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            if (!debt.isSettled) {
                Button(onClick = onPay, shape = RoundedCornerShape(8.dp), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp), modifier = Modifier.heightIn(min = 36.dp)) {
                    Text(if (borrowed) "Trả nợ" else "Thu nợ", fontSize = 12.sp)
                }
            } else {
                Surface(shape = RoundedCornerShape(6.dp), color = IncomeGreen.copy(alpha = 0.15f)) {
                    Text("Đã tất toán", color = IncomeGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}
