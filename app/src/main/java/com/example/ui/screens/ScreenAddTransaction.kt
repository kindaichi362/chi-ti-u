package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.InitialCategories
import com.example.data.model.Account
import com.example.data.model.Category
import com.example.data.model.CategoryType
import com.example.data.model.PaymentStatus
import com.example.data.model.TransactionType
import com.example.ui.components.AccountLogo
import com.example.ui.components.AppCard
import com.example.ui.components.CategoryTile
import com.example.ui.components.ChoiceChip
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SegmentedTabs
import com.example.ui.components.VoiceTransactionBottomSheet
import com.example.ui.components.formatVnd
import com.example.ui.screens.paytrack.PayTrackBottomSheet
import com.example.ui.theme.AppPalette
import com.example.ui.theme.MoneyStyle
import com.example.ui.theme.Radius
import com.example.ui.theme.Space
import com.example.ui.theme.TextSize
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val TYPES = listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER)
private val QUICK_AMOUNTS = listOf(10_000L, 20_000L, 50_000L, 100_000L, 200_000L, 500_000L)

/**
 * Add a transaction. The amount is the biggest thing on screen; frequent categories are one tap;
 * the Save button stays above the keyboard and can only fire once.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScreenAddTransaction(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    initialType: TransactionType = TransactionType.EXPENSE,
    initialSourceAccountId: Long? = null,
    modifier: Modifier = Modifier
) {
    val p = AppPalette
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val accounts by viewModel.accounts.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val transactions by viewModel.transactions.collectAsState()

    var type by remember { mutableStateOf(initialType) }
    var amountDigits by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long?>(null) }
    var sourceId by remember { mutableStateOf(initialSourceAccountId) }
    var destId by remember { mutableStateOf<Long?>(null) }
    var note by remember { mutableStateOf("") }
    var timestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var quickText by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var showAllCategories by remember { mutableStateOf(false) }
    var showQr by remember { mutableStateOf(false) }
    var showVoice by remember { mutableStateOf(false) }

    val amount = amountDigits.toDoubleOrNull() ?: 0.0
    if (sourceId == null && accounts.isNotEmpty()) sourceId = accounts.first().id
    if (type == TransactionType.TRANSFER && (destId == null || destId == sourceId)) destId = accounts.firstOrNull { it.id != sourceId }?.id

    val catType = if (type == TransactionType.INCOME) CategoryType.INCOME else CategoryType.EXPENSE
    // Frequent = starred first, then the most used in the last 60 days.
    val frequent = remember(categories, transactions, catType) {
        val since = System.currentTimeMillis() - 60L * 86_400_000L
        val usage = transactions.filter { it.timestamp >= since && it.categoryId != null }.groupingBy { it.categoryId!! }.eachCount()
        categories.filter { it.type == catType }
            .sortedWith(compareByDescending<Category> { it.isPriority }.thenByDescending { usage[it.id] ?: 0 }.thenBy { it.displayOrder })
            .take(8)
    }
    val selectedCategory = categories.firstOrNull { it.id == categoryId }
    val shownCategories = if (selectedCategory != null && selectedCategory !in frequent) listOf(selectedCategory) + frequent.take(7) else frequent

    fun save() {
        if (saving) return
        val src = sourceId
        when {
            amount <= 0 -> { Toast.makeText(context, "Nhập số tiền lớn hơn 0", Toast.LENGTH_SHORT).show(); return }
            src == null -> { Toast.makeText(context, "Chọn tài khoản", Toast.LENGTH_SHORT).show(); return }
            type == TransactionType.TRANSFER && (destId == null || destId == src) -> {
                Toast.makeText(context, "Chọn tài khoản nhận khác tài khoản chuyển", Toast.LENGTH_SHORT).show(); return
            }
        }
        saving = true // one tap = one transaction, even if the button is hit again before the screen closes
        viewModel.addTransaction(
            type = type,
            amount = amount,
            sourceAccountId = src!!,
            destinationAccountId = if (type == TransactionType.TRANSFER) destId else null,
            categoryId = if (type == TransactionType.TRANSFER) null else categoryId,
            note = note.trim(),
            timestamp = timestamp,
            paymentStatus = PaymentStatus.PAID
        )
        Toast.makeText(context, "Đã lưu ${formatVnd(amount)}", Toast.LENGTH_SHORT).show()
        onNavigateBack()
    }

    fun applyQuickText() {
        val parsed = viewModel.parseNaturalLanguage(quickText) ?: run {
            Toast.makeText(context, "Chưa đọc được số tiền trong câu", Toast.LENGTH_SHORT).show(); return
        }
        type = parsed.type
        amountDigits = parsed.amount.toLong().toString()
        parsed.categoryId?.let { categoryId = it }
        sourceId = parsed.sourceAccountId
        parsed.destinationAccountId?.let { destId = it }
        note = parsed.note
        quickText = ""
        focus.clearFocus()
    }

    Column(modifier = modifier.fillMaxSize().background(p.background).statusBarsPadding()) {
        // Header
        Row(Modifier.fillMaxWidth().padding(horizontal = Space.xs, vertical = Space.xs), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onNavigateBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = p.textPrimary) }
            Text("Thêm giao dịch", fontSize = TextSize.lg, fontWeight = FontWeight.SemiBold, color = p.textPrimary, modifier = Modifier.weight(1f))
            if (type == TransactionType.EXPENSE) {
                IconButton(onClick = { showQr = true }, modifier = Modifier.testTag("btn_pay_track_qr")) {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = "Quét mã QR", tint = p.textPrimary)
                }
            }
            IconButton(onClick = { showVoice = true }, modifier = Modifier.testTag("btn_voice_add_screen")) {
                Icon(Icons.Outlined.Mic, contentDescription = "Nhập bằng giọng nói", tint = p.accent)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.page),
            verticalArrangement = Arrangement.spacedBy(Space.lg)
        ) {
            SegmentedTabs(
                options = listOf("Chi", "Thu", "Chuyển"),
                selected = TYPES.indexOf(type),
                onSelect = { i ->
                    type = TYPES[i]
                    categoryId = null
                }
            )

            // Amount: the most prominent field
            AppCard(contentPadding = Space.lg) {
                Text("Số tiền", fontSize = TextSize.sm, color = p.textSecondary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val shown = if (amountDigits.isEmpty()) "" else java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(amountDigits.toLong())
                    Box(Modifier.weight(1f)) {
                        if (amountDigits.isEmpty()) Text("0", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = p.textTertiary, style = MoneyStyle)
                        BasicTextField(
                            value = shown,
                            onValueChange = { v -> amountDigits = v.filter(Char::isDigit).trimStart('0').take(13) },
                            singleLine = true,
                            textStyle = MoneyStyle.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold,
                                color = when (type) { TransactionType.EXPENSE -> p.expense; TransactionType.INCOME -> p.income; else -> p.textPrimary }),
                            cursorBrush = SolidColor(p.accent),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                            modifier = Modifier.fillMaxWidth().testTag("amount_input")
                        )
                    }
                    Text("₫", fontSize = 28.sp, fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                }
                Spacer(Modifier.height(Space.md))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                    QUICK_AMOUNTS.forEach { q ->
                        ChoiceChip(label = "+" + if (q >= 1_000_000) "${q / 1_000_000}tr" else "${q / 1000}k", selected = false,
                            onClick = { amountDigits = ((amountDigits.toLongOrNull() ?: 0L) + q).toString() })
                    }
                    if (amountDigits.isNotEmpty()) ChoiceChip(label = "Xóa", selected = false, onClick = { amountDigits = "" })
                }
            }

            // Quick sentence ("cà phê 35k momo") fills the form for review
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(Radius.md).background(p.card).border(1.dp, p.border, Radius.md).padding(start = Space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = p.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Space.sm))
                Box(Modifier.weight(1f)) {
                    if (quickText.isEmpty()) Text("Gõ nhanh: cà phê 35k momo", fontSize = TextSize.sm, color = p.textSecondary, maxLines = 1)
                    BasicTextField(
                        value = quickText,
                        onValueChange = { quickText = it },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = TextSize.sm, color = p.textPrimary),
                        cursorBrush = SolidColor(p.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { applyQuickText() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (quickText.isNotBlank()) TextButton(onClick = { applyQuickText() }) { Text("Điền", color = p.accent, fontWeight = FontWeight.SemiBold) }
            }

            // Category (not for transfers)
            if (type != TransactionType.TRANSFER) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Danh mục", fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showAllCategories = true }) {
                            Icon(Icons.Outlined.GridView, contentDescription = null, tint = p.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(Space.xs))
                            Text("Tất cả", color = p.accent, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    FlowRow(
                        maxItemsInEachRow = 4,
                        horizontalArrangement = Arrangement.spacedBy(Space.sm),
                        verticalArrangement = Arrangement.spacedBy(Space.sm),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        shownCategories.forEach { cat ->
                            CategoryCell(cat, selected = cat.id == categoryId, modifier = Modifier.weight(1f)) { categoryId = cat.id }
                        }
                        repeat((4 - shownCategories.size % 4) % 4) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            // Accounts
            Column(verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                Text(if (type == TransactionType.TRANSFER) "Từ tài khoản" else if (type == TransactionType.INCOME) "Vào tài khoản" else "Trả từ tài khoản",
                    fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                AccountPicker(accounts, sourceId) { sourceId = it }
                if (type == TransactionType.TRANSFER) {
                    Spacer(Modifier.height(Space.xs))
                    Text("Đến tài khoản", fontSize = TextSize.base, fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                    AccountPicker(accounts.filter { it.id != sourceId }, destId) { destId = it }
                }
            }

            // Date and note
            AppCard(contentPadding = Space.md) {
                Row(
                    modifier = Modifier.fillMaxWidth().clip(Radius.sm).clickable(role = Role.Button) { showDate = true }.heightIn(min = 48.dp).padding(horizontal = Space.xs),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null, tint = p.textSecondary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Space.md))
                    Text("Ngày", fontSize = TextSize.base, color = p.textSecondary, modifier = Modifier.weight(1f))
                    Text(dateLabel(timestamp), fontSize = TextSize.base, fontWeight = FontWeight.Medium, color = p.textPrimary)
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(p.borderSubtle))
                Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(horizontal = Space.xs), contentAlignment = Alignment.CenterStart) {
                    if (note.isEmpty()) Text("Ghi chú (không bắt buộc)", fontSize = TextSize.base, color = p.textSecondary)
                    BasicTextField(
                        value = note,
                        onValueChange = { note = it },
                        textStyle = TextStyle(fontSize = TextSize.base, color = p.textPrimary),
                        cursorBrush = SolidColor(p.accent),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
                        modifier = Modifier.fillMaxWidth().padding(vertical = Space.md)
                    )
                }
            }
            Spacer(Modifier.height(Space.sm))
        }

        // Save: pinned above the keyboard and the gesture bar
        Box(
            Modifier
                .fillMaxWidth()
                .background(p.card)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = Space.page, vertical = Space.md)
        ) {
            PrimaryButton(
                text = if (amount > 0) "Lưu · ${formatVnd(amount)}" else "Lưu",
                icon = Icons.Default.Check,
                onClick = { save() },
                enabled = !saving && amount > 0,
                modifier = Modifier.fillMaxWidth().testTag("save_transaction_button")
            )
        }
    }

    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = toUtcMidnight(timestamp))
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { timestamp = withDate(timestamp, it) }; showDate = false }) { Text("Chọn", color = p.accent) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text("Hủy", color = p.textSecondary) } }
        ) { DatePicker(state = state) }
    }

    if (showAllCategories) {
        AlertDialog(
            onDismissRequest = { showAllCategories = false },
            containerColor = p.card,
            title = { Text("Chọn danh mục", fontWeight = FontWeight.Bold, color = p.textPrimary) },
            text = {
                val groups = categories.filter { it.type == catType }
                    .groupBy { it.groupName ?: "Khác" }
                    .toList()
                    .sortedBy { (g, _) -> InitialCategories.groupOrder.indexOf(g).let { if (it < 0) 99 else it } }
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.md)) {
                    groups.forEach { (g, cats) ->
                        Text(g, fontSize = TextSize.sm, fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                        FlowRow(maxItemsInEachRow = 3, horizontalArrangement = Arrangement.spacedBy(Space.sm), verticalArrangement = Arrangement.spacedBy(Space.sm)) {
                            cats.forEach { cat ->
                                CategoryCell(cat, selected = cat.id == categoryId, modifier = Modifier.weight(1f)) { categoryId = cat.id; showAllCategories = false }
                            }
                            repeat((3 - cats.size % 3) % 3) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAllCategories = false }) { Text("Đóng", color = p.textSecondary) } }
        )
    }

    if (showQr) {
        PayTrackBottomSheet(
            initialAmount = amount,
            initialNote = note,
            selectedCategory = selectedCategory,
            selectedAccount = accounts.firstOrNull { it.id == sourceId },
            accounts = accounts,
            onDismiss = { showQr = false },
            onPaymentConfirmed = { amt, noteText, catId, accId, qr ->
                viewModel.addTransaction(
                    type = TransactionType.EXPENSE, amount = amt, sourceAccountId = accId, categoryId = catId, note = noteText,
                    paymentStatus = PaymentStatus.PAID, qrBankBin = qr.bankBin, qrAccountNumber = qr.accountNumber,
                    qrBeneficiaryName = qr.beneficiaryName, qrContent = qr.transferContent
                )
                Toast.makeText(context, "Đã thanh toán và ghi vào sổ", Toast.LENGTH_LONG).show()
                showQr = false
                onNavigateBack()
            },
            onPaymentPendingSaved = { amt, noteText, catId, accId, qr ->
                viewModel.addTransaction(
                    type = TransactionType.EXPENSE, amount = amt, sourceAccountId = accId, categoryId = catId, note = noteText,
                    paymentStatus = PaymentStatus.PENDING_PAYMENT, qrBankBin = qr.bankBin, qrAccountNumber = qr.accountNumber,
                    qrBeneficiaryName = qr.beneficiaryName, qrContent = qr.transferContent
                )
                Toast.makeText(context, "Đã lưu tạm, chờ xác nhận thanh toán", Toast.LENGTH_LONG).show()
                showQr = false
                onNavigateBack()
            }
        )
    }

    if (showVoice) {
        // The sheet shows what it understood and saves only after the user confirms.
        VoiceTransactionBottomSheet(
            viewModel = viewModel,
            onDismiss = { showVoice = false },
            onTransactionSaved = { showVoice = false; onNavigateBack() },
            autoStart = true
        )
    }
}

@Composable
private fun CategoryCell(cat: Category, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val p = AppPalette
    Column(
        modifier = modifier
            .clip(Radius.md)
            .background(if (selected) p.accentSoft else p.card)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) p.accent else p.border, Radius.md)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .height(84.dp)
            .padding(vertical = Space.sm, horizontal = Space.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CategoryTile(cat, size = 36.dp)
        Spacer(Modifier.height(6.dp))
        Text(cat.name, fontSize = 12.sp, lineHeight = 15.sp, color = p.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}

@Composable
private fun AccountPicker(accounts: List<Account>, selectedId: Long?, onSelect: (Long) -> Unit) {
    val p = AppPalette
    LazyRow(horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
        items(accounts, key = { it.id }) { acc ->
            val on = acc.id == selectedId
            Row(
                modifier = Modifier
                    .widthIn(min = 120.dp)
                    .clip(Radius.md)
                    .background(if (on) p.accentSoft else p.card)
                    .border(if (on) 1.5.dp else 1.dp, if (on) p.accent else p.border, Radius.md)
                    .clickable(role = Role.RadioButton) { onSelect(acc.id) }
                    .heightIn(min = 52.dp)
                    .padding(horizontal = Space.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AccountLogo(acc, size = 24.dp)
                Spacer(Modifier.width(Space.sm))
                Column {
                    Text(acc.name, fontSize = TextSize.sm, fontWeight = FontWeight.Medium, color = p.textPrimary, maxLines = 1)
                    Text(formatVnd(acc.balance), fontSize = TextSize.xs, color = p.textSecondary, style = MoneyStyle, maxLines = 1)
                }
            }
        }
    }
}

private fun dateLabel(ts: Long): String {
    val c = Calendar.getInstance().apply { timeInMillis = ts }
    val today = Calendar.getInstance()
    val sameDay = c.get(Calendar.YEAR) == today.get(Calendar.YEAR) && c.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
    return if (sameDay) "Hôm nay, $time" else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ts)) + ", $time"
}

private fun toUtcMidnight(localMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = localMillis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun withDate(originalLocalMillis: Long, pickedUtcMidnight: Long): Long {
    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickedUtcMidnight }
    return Calendar.getInstance().apply {
        timeInMillis = originalLocalMillis
        set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}
