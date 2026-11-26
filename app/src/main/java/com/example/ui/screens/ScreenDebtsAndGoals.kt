package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.AccountType
import com.example.data.model.CategoryType
import com.example.data.model.Debt
import com.example.data.model.DebtType
import com.example.data.model.Goal
import com.example.data.model.RecurringBill
import com.example.ui.components.formatVnd
import com.example.ui.components.CategoryChoiceChip
import com.example.ui.components.matchesSearch
import com.example.ui.theme.ExpenseRed
import com.example.ui.theme.IncomeGreen
import com.example.ui.theme.TransferBlue
import com.example.ui.viewmodel.FinanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Reads amounts typed as "1000000", "1.000.000" or "1,000,000"; anything unreadable is 0. */
private fun parseVndInput(input: String): Double =
    input.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountChips(accounts: List<Account>, selectedId: Long?, onSelect: (Long) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(accounts, key = { it.id }) { acc ->
            FilterChip(
                selected = selectedId == acc.id,
                onClick = { onSelect(acc.id) },
                label = { Text(acc.name, fontSize = 12.sp) }
            )
        }
    }
}

private val DEBT_TRACKING_TYPES = setOf(AccountType.RECEIVABLE, AccountType.PAYABLE, AccountType.LOAN)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenDebtsAndGoals(
    viewModel: FinanceViewModel,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    /** Bottom-bar "Vay nợ" tab: only the debt list, no goals or bills. */
    debtsOnly: Boolean = false,
    /** Opens the add-debt dialog right away with this type (dashboard quick buttons). */
    startAddDebt: DebtType? = null,
    onStartAddDebtConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val debts by viewModel.debts.collectAsState()
    val goals by viewModel.goals.collectAsState()
    val bills by viewModel.bills.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    // Loan money really moves in or out of a wallet/bank; tracking-only accounts would hide it.
    val moneyAccounts = remember(accounts) { accounts.filter { it.type !in DEBT_TRACKING_TYPES } }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    // Dialogs
    var showAddDebtDialog by remember { mutableStateOf(false) }
    var addDebtType by remember { mutableStateOf(DebtType.I_OWE) }
    LaunchedEffect(startAddDebt) {
        if (startAddDebt != null) {
            selectedTabIndex = 0
            addDebtType = startAddDebt
            showAddDebtDialog = true
            onStartAddDebtConsumed()
        }
    }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddBillDialog by remember { mutableStateOf(false) }
    val categories by viewModel.categories.collectAsState()
    var payingDebt by remember { mutableStateOf<Debt?>(null) }
    var selectedDebt by remember { mutableStateOf<Debt?>(null) }
    var depositingGoal by remember { mutableStateOf<Goal?>(null) }
    var paymentAmountInput by remember { mutableStateOf("") }
    var selectedAccountForAction by remember { mutableStateOf<Long?>(null) }

    if (selectedAccountForAction == null && accounts.isNotEmpty()) {
        selectedAccountForAction = accounts.first().id
    }

    selectedDebt?.let { picked ->
        // Follow the live row so the dialog shows payments as soon as they are saved.
        val debt = debts.firstOrNull { it.id == picked.id }
        if (debt == null) selectedDebt = null else {
            val history by viewModel.transactionsForDebt(debt.id).collectAsState(initial = emptyList())
            DebtDetailDialog(
                debt = debt, history = history, accounts = accounts,
                onDismiss = { selectedDebt = null },
                onPay = { payingDebt = debt },
                onUpdate = { viewModel.updateDebt(it) },
                onDelete = { viewModel.deleteDebt(debt.id); selectedDebt = null; Toast.makeText(context, "Đã xóa khoản nợ", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    if (showAddDebtDialog) {
        AddDebtDialog(moneyAccounts, onDismiss = { showAddDebtDialog = false }, initialType = addDebtType) { debt ->
            viewModel.addDebt(debt)
            showAddDebtDialog = false
            Toast.makeText(context, "Đã lưu khoản ${if (debt.type == DebtType.I_OWE) "vay" else "cho vay"}", Toast.LENGTH_SHORT).show()
        }
    }

    // Add Recurring Bill Dialog
    if (showAddBillDialog) {
        var billName by remember { mutableStateOf("") }
        var billAmount by remember { mutableStateOf("") }
        var billDay by remember { mutableStateOf("") }
        var billCategoryId by remember { mutableStateOf<Long?>(null) }
        var billCategoryQuery by remember { mutableStateOf("") }
        var billAccountId by remember { mutableStateOf(accounts.firstOrNull()?.id) }
        // Utility-type categories first, so the usual picks (điện, nước, internet...) are in view.
        val billCategories = remember(categories) {
            categories.filter { it.type == CategoryType.EXPENSE }
                .sortedByDescending { it.groupName == "Sinh hoạt" || it.groupName == "Nhà ở" }
        }

        AlertDialog(
            onDismissRequest = { showAddBillDialog = false },
            title = { Text("Thêm hóa đơn định kỳ", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = billName,
                        onValueChange = { billName = it },
                        label = { Text("Tên hóa đơn") },
                        placeholder = { Text("Ví dụ: Tiền điện, Internet, Tiền nhà...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = billAmount,
                        onValueChange = { billAmount = it },
                        label = { Text("Số tiền mỗi tháng (VNĐ)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = billDay,
                        onValueChange = { billDay = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Ngày đến hạn hàng tháng (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Danh mục", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = billCategoryQuery,
                        onValueChange = { billCategoryQuery = it },
                        label = { Text("Tìm danh mục") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(billCategories.filter { it.matchesSearch(billCategoryQuery) }, key = { it.id }) { cat ->
                            CategoryChoiceChip(category = cat, selected = billCategoryId == cat.id) {
                                billCategoryId = cat.id
                            }
                        }
                    }
                    Text("Trả từ tài khoản", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    AccountChips(accounts, billAccountId) { billAccountId = it }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = parseVndInput(billAmount)
                        val day = billDay.toIntOrNull()
                        val catId = billCategoryId
                        val accId = billAccountId
                        if (billName.isBlank() || amt <= 0 || day == null || day !in 1..31 || catId == null || accId == null) {
                            Toast.makeText(context, "Vui lòng nhập đủ tên, số tiền, ngày (1-31), danh mục và tài khoản", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.addBill(billName.trim(), amt, accId, catId, day)
                            showAddBillDialog = false
                            Toast.makeText(context, "Đã thêm hóa đơn định kỳ!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddBillDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Add Goal Dialog
    if (showAddGoalDialog) {
        var goalName by remember { mutableStateOf("") }
        var targetAmt by remember { mutableStateOf("") }
        var initialAmt by remember { mutableStateOf("") }
        var monthlyTarget by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text("Tạo mục tiêu tài chính", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        label = { Text("Tên mục tiêu") },
                        placeholder = { Text("Ví dụ: Mua ô tô, Quỹ dự phòng, Mua nhà...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = targetAmt,
                        onValueChange = { targetAmt = it },
                        label = { Text("Số tiền mục tiêu (VNĐ)") },
                        placeholder = { Text("Ví dụ: 500000000") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = initialAmt,
                        onValueChange = { initialAmt = it },
                        label = { Text("Đã có sẵn (VNĐ)") },
                        placeholder = { Text("0") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = monthlyTarget,
                        onValueChange = { monthlyTarget = it },
                        label = { Text("Dự kiến tiết kiệm / tháng (VNĐ)") },
                        placeholder = { Text("Ví dụ: 20000000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = parseVndInput(targetAmt)
                        val initial = parseVndInput(initialAmt)
                        val monthly = parseVndInput(monthlyTarget)
                        if (goalName.isNotBlank() && target > 0) {
                            viewModel.addGoal(goalName, target, initial, monthly)
                            showAddGoalDialog = false
                            Toast.makeText(context, "Đã tạo mục tiêu!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Vui lòng nhập tên và số tiền mục tiêu", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Tạo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddGoalDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    payingDebt?.let { picked ->
        val debt = debts.firstOrNull { it.id == picked.id } ?: picked
        PayDebtDialog(debt, moneyAccounts, onDismiss = { payingDebt = null }) { amount, accountId, ts ->
            viewModel.recordDebtPayment(debt, amount, accountId, ts)
            payingDebt = null
            Toast.makeText(context, "Đã ghi ${formatVnd(amount)}", Toast.LENGTH_SHORT).show()
        }
    }

    // Deposit to Goal Dialog
    if (depositingGoal != null) {
        val goal = depositingGoal!!
        AlertDialog(
            onDismissRequest = { depositingGoal = null },
            title = { Text("Nạp tiết kiệm: ${goal.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mục tiêu: ${formatVnd(goal.targetAmount)} (Đã có: ${formatVnd(goal.currentAmount)})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = paymentAmountInput,
                        onValueChange = { paymentAmountInput = it },
                        label = { Text("Số tiền nạp thêm (VNĐ)") },
                        placeholder = { Text("Ví dụ: 5000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Lấy từ tài khoản", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    AccountChips(accounts, selectedAccountForAction) { selectedAccountForAction = it }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = parseVndInput(paymentAmountInput)
                        val accId = selectedAccountForAction ?: accounts.firstOrNull()?.id
                        if (amt > 0 && accId != null) {
                            viewModel.depositToGoal(goal, amt, accId)
                            depositingGoal = null
                            paymentAmountInput = ""
                            Toast.makeText(context, "Đã nạp tiền vào mục tiêu!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Nạp ngay")
                }
            },
            dismissButton = {
                TextButton(onClick = { depositingGoal = null }) {
                    Text("Hủy")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
                Text(
                    text = if (debtsOnly) "Vay nợ" else "Vay nợ & kế hoạch",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = {
                    when (selectedTabIndex) {
                        0 -> { addDebtType = DebtType.I_OWE; showAddDebtDialog = true }
                        1 -> showAddGoalDialog = true
                        else -> showAddBillDialog = true
                    }
                },
                modifier = Modifier.testTag("add_plan_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = when (selectedTabIndex) {
                        0 -> "Khoản nợ"
                        1 -> "Mục tiêu"
                        else -> "Hóa đơn"
                    },
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs: Sổ Nợ (Debts) | Mục tiêu (Goals) | Hóa đơn (Bills)
        if (!debtsOnly) TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Vay nợ (${debts.count { !it.isSettled }})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Mục tiêu (${goals.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Hóa đơn (${bills.size})", fontWeight = FontWeight.SemiBold, fontSize = 12.sp) }
            )
        }

        if (!debtsOnly) Spacer(modifier = Modifier.height(14.dp))

        // Content for each tab
        when (selectedTabIndex) {
            0 -> DebtTab(debts, onOpen = { selectedDebt = it }, onPay = { payingDebt = it })

            1 -> { // MỤC TIÊU TÀI CHÍNH (GOALS)
                if (goals.isEmpty()) {
                    Text(
                        text = "Chưa có mục tiêu nào. Hãy đặt mục tiêu mua xe, mua nhà hoặc quỹ khẩn cấp!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(goals) { goal ->
                            val pct = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount) * 100 else 0.0
                            val remaining = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
                            val monthsLeft = if (goal.monthlyTarget > 0) kotlin.math.ceil(remaining / goal.monthlyTarget).toInt() else 0

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Flag,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = goal.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "${String.format("%.0f", pct)}%",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    LinearProgressIndicator(
                                        progress = { (pct / 100).toFloat().coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = Color.LightGray.copy(alpha = 0.3f)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Đã có: ${formatVnd(goal.currentAmount)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Mục tiêu: ${formatVnd(goal.targetAmount)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    if (monthsLeft > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "⏱️ Dự kiến hoàn thành sau ~$monthsLeft tháng (với mức tiết kiệm ${formatVnd(goal.monthlyTarget)}/tháng)",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Button(
                                        onClick = { depositingGoal = goal },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Savings, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Nạp tiền vào mục tiêu", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> { // HÓA ĐƠN ĐỊNH KỲ (BILLS)
                if (bills.isEmpty()) {
                    Text(
                        text = "Chưa có hóa đơn định kỳ nào. Bạn có thể thêm tiền điện, nước, internet...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(bills) { bill ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Receipt,
                                        contentDescription = null,
                                        tint = TransferBlue,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = bill.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "Số tiền: ${formatVnd(bill.amount)} • Hạn: Ngày ${bill.dayOfMonth} hàng tháng",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    TextButton(
                                        onClick = {
                                            viewModel.deleteBill(bill)
                                            Toast.makeText(context, "Đã xóa hóa đơn ${bill.name}", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Xóa", fontSize = 11.sp, color = ExpenseRed)
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.payBill(bill)
                                            Toast.makeText(context, "Đã ghi nhận thanh toán hóa đơn!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Đã trả", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
