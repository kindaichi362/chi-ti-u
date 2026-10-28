package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.CardInstallment
import com.example.data.model.nextAmount
import com.example.data.model.nextDueDate
import com.example.data.model.nextInterest
import com.example.ui.components.AppCard
import com.example.ui.components.MoneyText
import com.example.ui.components.RowDivider
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatVnd
import com.example.ui.theme.AppPalette
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
internal fun CardInstallmentSection(viewModel: FinanceViewModel, card: Account, accounts: List<Account>, hidden: Boolean) {
    val p = AppPalette
    val context = LocalContext.current
    val plans by remember(card.id) { viewModel.installmentsForCard(card.id) }.collectAsState(initial = emptyList())
    var adding by remember { mutableStateOf(false) }
    var paying by remember { mutableStateOf<CardInstallment?>(null) }
    var editing by remember { mutableStateOf<CardInstallment?>(null) }
    var deleting by remember { mutableStateOf<CardInstallment?>(null) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")) }

    SectionHeader("Trả góp hàng tháng")
    AppCard(contentPadding = Space.lg) {
        if (plans.isEmpty()) {
            Text("Chưa có khoản trả góp nào", color = p.textSecondary, fontSize = TextSize.sm)
        } else {
            plans.forEachIndexed { index, item ->
                if (index > 0) RowDivider()
                Column(Modifier.fillMaxWidth().padding(vertical = Space.sm), verticalArrangement = Arrangement.spacedBy(Space.xs)) {
                    Text(item.name, color = p.textPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Đã trả ${item.paidMonths}/${item.monthCount} kỳ · Giá mua ${if (hidden) HIDDEN_AMOUNT else formatVnd(item.totalAmount)}", color = p.textSecondary, fontSize = TextSize.sm)
                    item.monthlyAmount?.let { monthly ->
                        val remainingInterest = (item.paidMonths until item.monthCount).sumOf { item.copy(paidMonths = it).nextInterest() }
                        Text("Mỗi tháng ${if (hidden) HIDDEN_AMOUNT else formatVnd(monthly)} · Lãi còn lại ${if (hidden) HIDDEN_AMOUNT else formatVnd(remainingInterest)}", color = p.textSecondary, fontSize = TextSize.sm)
                    }
                    val due = item.nextDueDate()
                    if (due != null) {
                        Text("Kỳ tiếp theo ${dateFormat.format(Date(due))}", color = p.textSecondary, fontSize = TextSize.sm)
                        MoneyText(if (hidden) HIDDEN_AMOUNT else formatVnd(item.nextAmount()), color = p.expense)
                    } else {
                        Text("Đã hoàn tất", color = p.income, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold)
                    }
                    Row {
                        if (due != null) TextButton(onClick = { paying = item }) { Text("Trả kỳ này") }
                        if (due != null) TextButton(onClick = { editing = item }) { Text("Sửa mức trả") }
                        TextButton(onClick = { deleting = item }) { Text("Xóa lịch", color = p.textSecondary) }
                    }
                }
            }
        }
        TextButton(onClick = { adding = true }) { Text("+ Thêm khoản trả góp") }
    }

    if (adding) AddCardInstallmentDialog(card, onDismiss = { adding = false }) { item ->
        viewModel.addCardInstallment(item)
        adding = false
    }
    editing?.let { item ->
        var amount by remember(item.id) { mutableStateOf(item.monthlyAmount?.toLong()?.toString() ?: "") }
        val value = amount.toDoubleOrNull() ?: 0.0
        val minimum = kotlin.math.ceil(item.totalAmount / item.monthCount)
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("Sửa tiền trả mỗi tháng") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text(item.name, fontWeight = FontWeight.SemiBold)
                    Text("Áp dụng cho các kỳ chưa trả. Giá mua và các giao dịch đã trả được giữ nguyên.", color = p.textSecondary, fontSize = TextSize.sm)
                    OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Tiền trả mỗi tháng (gồm lãi)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (value > 0 && value < minimum) Text("Cần ít nhất ${formatVnd(minimum)} để trả đủ tiền gốc.", color = p.expense, fontSize = TextSize.sm)
                }
            },
            confirmButton = { Button(onClick = {
                viewModel.updateCardInstallmentMonthlyAmount(item.id, value) { success ->
                    Toast.makeText(context, if (success) "Đã đổi mức trả" else "Không lưu được mức trả", Toast.LENGTH_SHORT).show()
                }
                editing = null
            }, enabled = value in minimum..1_000_000_000_000_000.0) { Text("Lưu") } },
            dismissButton = { TextButton(onClick = { editing = null }) { Text("Hủy") } }
        )
    }
    paying?.let { item ->
        val sources = accounts.filter { it.id != card.id && it.type != AccountType.CREDIT_CARD }
        var sourceId by remember(item.id) { mutableStateOf(sources.firstOrNull { it.type == AccountType.BANK }?.id ?: sources.firstOrNull()?.id) }
        AlertDialog(
            onDismissRequest = { paying = null },
            title = { Text("Trả góp: ${item.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    Text("Kỳ ${item.paidMonths + 1}/${item.monthCount}: ${formatVnd(item.nextAmount())}")
                    Text("Tiền sẽ chuyển từ tài khoản chọn sang thẻ. Khoản trả này không tính là chi tiêu lần nữa.", color = p.textSecondary, fontSize = TextSize.sm)
                    if (sources.isEmpty()) Text("Hãy thêm tài khoản ngân hàng hoặc tiền mặt trước.", color = p.expense)
                    sources.forEach { source ->
                        FilterChip(selected = sourceId == source.id, onClick = { sourceId = source.id }, label = { Text(source.name) })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    sourceId?.let { selected ->
                        viewModel.payCardInstallment(item.id, selected) { success ->
                            Toast.makeText(context, if (success) "Đã ghi nhận kỳ trả góp" else "Không ghi nhận được khoản trả", Toast.LENGTH_SHORT).show()
                        }
                        paying = null
                    }
                }, enabled = sourceId != null) { Text("Xác nhận trả") }
            },
            dismissButton = { TextButton(onClick = { paying = null }) { Text("Hủy") } }
        )
    }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Xóa lịch trả góp?") },
            text = { Text("Chỉ xóa lịch ${item.name}. Những giao dịch đã trả vẫn được giữ trong lịch sử thẻ.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteCardInstallment(item); deleting = null }) { Text("Xóa", color = p.expense) } },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun AddCardInstallmentDialog(card: Account, onDismiss: () -> Unit, onSave: (CardInstallment) -> Unit) {
    val context = LocalContext.current
    val p = AppPalette
    var name by remember { mutableStateOf("") }
    var total by remember { mutableStateOf("") }
    var months by remember { mutableStateOf("") }
    var monthly by remember { mutableStateOf("") }
    var firstDue by remember { mutableStateOf(Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, (card.dueDay ?: 15).coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
        set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (timeInMillis < System.currentTimeMillis()) add(Calendar.MONTH, 1)
    }.timeInMillis) }
    val purchasePrice = total.toDoubleOrNull() ?: 0.0
    val numberOfMonths = months.toIntOrNull() ?: 0
    val monthlyPayment = monthly.toDoubleOrNull() ?: 0.0
    val valid = name.isNotBlank() && purchasePrice in 1.0..1_000_000_000_000_000.0 && numberOfMonths in 2..60 &&
        monthlyPayment >= kotlin.math.ceil(purchasePrice / numberOfMonths.coerceAtLeast(1)) && monthlyPayment <= 1_000_000_000_000_000.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm khoản trả góp") },
        text = {
            Column(Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text("Dùng cho khoản mua đã ghi vào thẻ. Lịch trả góp không tạo thêm chi tiêu.", color = p.textSecondary, fontSize = TextSize.sm)
                OutlinedTextField(name, { name = it }, label = { Text("Tên khoản mua") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(total, { total = it.filter(Char::isDigit) }, label = { Text("Giá mua (tiền gốc)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(months, { months = it.filter(Char::isDigit).take(2) }, label = { Text("Số tháng (2–60)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(monthly, { monthly = it.filter(Char::isDigit) }, label = { Text("Số tiền trả mỗi tháng (gồm lãi)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
                TextButton(onClick = {
                    val day = Calendar.getInstance().apply { timeInMillis = firstDue }
                    DatePickerDialog(context, { _, year, month, date ->
                        firstDue = Calendar.getInstance().apply { set(year, month, date, 12, 0, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
                    }, day.get(Calendar.YEAR), day.get(Calendar.MONTH), day.get(Calendar.DAY_OF_MONTH)).show()
                }) { Text("Kỳ đầu: ${SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(Date(firstDue))}") }
                if (valid) Text("Tổng phải trả ${formatVnd(monthlyPayment * numberOfMonths)} · Lãi ${formatVnd(monthlyPayment * numberOfMonths - purchasePrice)}", color = p.textSecondary, fontSize = TextSize.sm)
                else if (purchasePrice > 0 && numberOfMonths in 2..60 && monthlyPayment > 0) Text("Tiền mỗi tháng cần đủ để trả hết giá mua sau $numberOfMonths tháng.", color = p.expense, fontSize = TextSize.sm)
            }
        },
        confirmButton = { Button(onClick = { onSave(CardInstallment(cardId = card.id, name = name.trim(), totalAmount = purchasePrice, monthlyAmount = monthlyPayment, monthCount = numberOfMonths, firstDueDate = firstDue)) }, enabled = valid) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}
